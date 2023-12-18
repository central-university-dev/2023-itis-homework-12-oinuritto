package ru.shop.backend.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.shop.backend.search.model.*;
import ru.shop.backend.search.repository.ItemDbRepository;
import ru.shop.backend.search.repository.ItemRepository;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final ItemRepository repo;
    private final ItemDbRepository repoDb;

    private final Pageable pageable = PageRequest.of(0, 150);
    private final Pageable pageableSmall = PageRequest.of(0, 10);
    private static final Pattern PATTERN = Pattern.compile("\\d+");

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        return PATTERN.matcher(strNum).matches();
    }

    public SearchResultElastic getSearchResultElastic(String text) {
        List<CatalogueElastic> result = checkForNumericAndGetCatalogueList(text);
        if (result == null) {
            result = getAllFull(text);
        }
        return new SearchResultElastic(result);
    }

    public synchronized SearchResult getSearchResult(Integer regionId, String text) {
        List<CatalogueElastic> result = checkForNumericAndGetCatalogueList(text);

        if (result == null) {
            result = getAll(text);
        }

        List<Item> items = getItemsFromCatalogueList(regionId, result);
        String finalBrand = getBrandFromCatalogueList(result);
        List<Category> categories = getCategoriesFromItemList(items, finalBrand);

        return new SearchResult(items, categories, getTypeQueriesFromCatalogueList(result)
        );
    }

    public synchronized List<CatalogueElastic> getAll(String text) {
        return getAll(text, pageableSmall);
    }

    public List<CatalogueElastic> getAll(String text, Pageable pageable) {
        text = text.toLowerCase(Locale.ROOT);
        List<ItemElastic> list = new ArrayList<>();
        String text2 = text;
        Long catalogueId = null;

        boolean needConvert = false;
        Optional<String> convertedText = convertTextIfContainErrorChar(text);
        if (convertedText.isPresent()) {
            text = convertedText.get();
        } else {
            needConvert = true;
        }

        Map<String, String> brandMap = extractBrandWithBrandQuery(text, needConvert, list, pageable);
        String brand = brandMap.get("brand");
        text = text.replace(brandMap.get("brandQuery"), "").trim().replace("  ", " ");

        Map<String, String> typeMap = extractTypeWithTypeQuery(text, needConvert, list, pageable);
        String type = typeMap.get("type");
        if (text.contains(" ")) {
            text = text.replace(typeMap.get("typeQuery"), "").trim().replace("  ", " ");
        }

        if (brand.isEmpty()) {
            list = getItemElasticListByCatalogue(text, pageable, needConvert);
            if (!list.isEmpty()) {
                catalogueId = list.get(0).getCatalogueId();
            }
        }

        text = text.trim();

        text += "?";

        if (brand.isEmpty()) {
            type += "?";
            list = getItemElasticListForEmptyBrand(text, pageable, catalogueId, type);
        } else if (text.equals("?")) {
            list = getItemElasticListByBrandQuery(brandMap.get("brandQuery"), needConvert, pageable);
        } else {
            if (type.isEmpty()) {
                list = getItemElasticListForBrandAndEmptyType(text, pageable, brand);
            } else {
                type += "?";
                list = getItemElasticListForBrandAndType(text, pageable, brand, type);
            }
        }

        return getCatalogueElasticList(text, pageable, list, text2, needConvert, brand, type);
    }

    private List<CatalogueElastic> get(List<ItemElastic> list, String name, String brand) {
        Map<String, List<ItemElastic>> map = new HashMap<>();
        ItemElastic searchedItem = getSearchedItem(list, name, map);

        if (brand.isEmpty())
            brand = null;
        if (searchedItem != null) {
            return Collections.singletonList(
                    new CatalogueElastic(searchedItem.getCatalogue(), searchedItem.getCatalogueId(),
                            Collections.singletonList(searchedItem), brand));
        }

        String finalBrand = brand;
        return map.keySet()
                .stream()
                .map(c -> new CatalogueElastic(c, map.get(c).get(0).getCatalogueId(), map.get(c), finalBrand))
                .collect(Collectors.toList());
    }

    private ItemElastic getSearchedItem(List<ItemElastic> list, String name, Map<String, List<ItemElastic>> map) {
        ItemElastic searchedItem = null;
        String clearedName = name.replace("?", "");

        for (ItemElastic i : list) {
            if (clearedName.equals(i.getName())) {
                searchedItem = i;
            }
            if (clearedName.endsWith(i.getName()) && clearedName.startsWith(i.getType())) {
                searchedItem = i;
            }
            map.computeIfAbsent(i.getCatalogue(), k -> new ArrayList<>()).add(i);
        }
        return searchedItem;
    }

    public List<CatalogueElastic> getByName(String num) {
        List<ItemElastic> list = repo.findAllByName(".*" + num + ".*", pageable);
        return get(list, num, "");
    }

    public List<CatalogueElastic> getByItemId(String itemId) {
        var list = repo.findByItemId(itemId, PageRequest.of(0, 1));
        return Collections.singletonList(new CatalogueElastic(list.get(0).getCatalogue(), list.get(0).getCatalogueId(), list, list.get(0).getBrand()));
    }

    public static String convert(String message) {
        boolean result = message.matches(".*\\p{InCyrillic}.*");
        char[] ru = {'й', 'ц', 'у', 'к', 'е', 'н', 'г', 'ш', 'щ', 'з', 'х', 'ъ', 'ф', 'ы', 'в', 'а', 'п', 'р', 'о', 'л', 'д', 'ж', 'э', 'я', 'ч', 'с', 'м', 'и', 'т', 'ь', 'б', 'ю', '.',
                ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-'};
        char[] en = {'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']', 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '"', 'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/',
                ' ', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-'};
        StringBuilder builder = new StringBuilder();

        if (result) {
            for (int i = 0; i < message.length(); i++) {
                for (int j = 0; j < ru.length; j++) {
                    if (message.charAt(i) == ru[j]) {
                        builder.append(en[j]);
                    }
                }
            }
        } else {
            for (int i = 0; i < message.length(); i++) {
                for (int j = 0; j < en.length; j++) {
                    if (message.charAt(i) == en[j]) {
                        builder.append(ru[j]);
                    }
                }
            }
        }
        return builder.toString();
    }

    private Boolean isContainErrorChar(String text) {
        List<String> errorChars = List.of("[", "]", "\"", "/", ";");
        return errorChars.stream().anyMatch(text::contains);
    }

    public List<CatalogueElastic> getAllFull(String text) {
        return getAll(text, pageable);
    }

    private List<TypeHelpText> getTypeQueriesFromCatalogueList(List<CatalogueElastic> result) {
        if (result.isEmpty() || result.get(0).getItems().isEmpty()) {
            return Collections.emptyList();
        }

        CatalogueElastic catalogue = result.get(0);
        ItemElastic item = catalogue.getItems().get(0);
        String type = Optional.ofNullable(item.getType()).orElse("");
        String brand = Optional.ofNullable(catalogue.getBrand()).orElse("");
        String queryText = (type + " " + brand).trim();

        return Collections.singletonList(new TypeHelpText(TypeOfQuery.SEE_ALSO, queryText));
    }

    private List<Category> getCategoriesFromItemList(List<Item> items, String finalBrand) {
        Set<String> catUrls = new HashSet<>();
        return repoDb.findCatsByIds(getItemIdsFromItemList(items))
                .stream()
                .map(arr -> getCategoryFromObjArray(arr, catUrls, finalBrand))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Item> getItemsFromCatalogueList(Integer regionId, List<CatalogueElastic> result) {
        return repoDb.findByIds(regionId, getItemIdsFromCatalogueList(result))
                .stream()
                .map(this::getItemFromObjArray)
                .collect(Collectors.toList());
    }

    private Category getCategoryFromObjArray(Object[] arr, Set<String> catUrls, String finalBrand) {
        String catUrl = arr[2].toString();

        if (catUrls.contains(catUrl)) {
            return null;
        }
        catUrls.add(catUrl);

        String brandUrl = finalBrand.isEmpty() ? "" : "/brands/" + finalBrand;
        String categoryUrl = "/cat/" + catUrl + brandUrl;
        String description = (arr[4] == null) ? null : arr[4].toString();

        return new Category(
                arr[0].toString(),
                arr[1].toString(),
                categoryUrl,
                "/cat/" + arr[3].toString(),
                description
        );
    }

    private List<Integer> getItemIdsFromItemList(List<Item> items) {
        return items.stream()
                .map(Item::getItemId)
                .collect(Collectors.toList());
    }

    private String getBrandFromCatalogueList(List<CatalogueElastic> result) {
        return result.stream()
                .findFirst()
                .map(CatalogueElastic::getBrand)
                .orElse("")
                .toLowerCase(Locale.ROOT);
    }

    private Item getItemFromObjArray(Object[] arr) {
        return new Item(((BigInteger) arr[2]).intValue(), arr[1].toString(), arr[3].toString(), arr[4].toString(),
                ((BigInteger) arr[0]).intValue(), arr[5].toString());
    }

    private List<Long> getItemIdsFromCatalogueList(List<CatalogueElastic> result) {
        return result.stream()
                .flatMap(category -> Optional.ofNullable(category.getItems()).orElse(Collections.emptyList()).stream())
                .map(ItemElastic::getItemId)
                .collect(Collectors.toList());
    }

    private List<CatalogueElastic> checkForNumericAndGetCatalogueList(String text) {
        List<CatalogueElastic> result = null;
        if (isNumeric(text)) {
            Integer itemId = repoDb.findBySku(text).stream().findFirst().orElse(null);
            if (itemId == null) {
                var catalogue = getByName(text);
                if (!catalogue.isEmpty()) {
                    result = catalogue;
                }
            } else {
                result = getByItemId(itemId.toString());
            }
        }
        return result;
    }

    private Optional<String> convertTextIfContainErrorChar(String text) {
        if (isContainErrorChar(text)) {
            return Optional.of(convert(text));
        } else if (isContainErrorChar(convert(text))) {
            return Optional.of(text);
        } else {
            return Optional.empty();
        }
    }

    private Map<String, String> extractBrandWithBrandQuery(String text, boolean needConvert, List<ItemElastic> list, Pageable pageable) {
        Map<String, String> result = new HashMap<>();
        String brand = "";
        String brandQuery = "";

        for (String queryWord : text.split("\\s")) {
            list = getItemElasticListByBrandQuery(queryWord, needConvert, pageable);
            if (!list.isEmpty()) {
                brand = list.get(0).getBrand();
                brandQuery = queryWord;
                break;
            }
        }

        result.put("brand", brand);
        result.put("brandQuery", brandQuery);
        return result;
    }

    private List<ItemElastic> getItemElasticListByBrandQuery(String brand, boolean needConvert, Pageable pageable) {
        List<ItemElastic> list;
        list = repo.findAllByBrand(brand, pageable);
        if (list.isEmpty() && needConvert) {
            list = repo.findAllByBrand(convert(brand), pageable);
        }
        return list;
    }

    private Map<String, String> extractTypeWithTypeQuery(String text, boolean needConvert, List<ItemElastic> list, Pageable pageable) {
        Map<String, String> result = new HashMap<>();
        String type = "";
        String typeQuery = "";

        for (String queryWord : text.split("\\s")) {
            list = getItemElasticListByTypeQuery(queryWord, needConvert, pageable);
            if (!list.isEmpty()) {
                typeQuery = queryWord;
                type = getMinLengthType(list);
                break;
            }
        }

        result.put("type", type);
        result.put("typeQuery", typeQuery);
        return result;
    }

    private String getMinLengthType(List<ItemElastic> list) {
        return list.stream()
                .map(ItemElastic::getType)
                .min(Comparator.comparingInt(String::length))
                .orElse("");
    }

    private List<ItemElastic> getItemElasticListByTypeQuery(String type, boolean needConvert, Pageable pageable) {
        List<ItemElastic> list;
        list = repo.findAllByType(type, pageable);
        if (list.isEmpty() && needConvert) {
            list = repo.findAllByType(convert(type), pageable);
        }
        return list;
    }

    private List<ItemElastic> getItemElasticListForBrandAndType(String text, Pageable pageable, String brand, String type) {
        List<ItemElastic> list;
        list = repo.findAllByTypeAndBrand(text, brand, type, pageable);
        if (list.isEmpty()) {
            list = repo.findAllByTypeAndBrand(convert(text), brand, type, pageable);
        }
        return list;
    }

    private List<ItemElastic> getItemElasticListForBrandAndEmptyType(String text, Pageable pageable, String brand) {
        List<ItemElastic> list;
        list = repo.findAllByBrand(text, brand, pageable);
        if (list.isEmpty()) {
            list = repo.findAllByBrand(convert(text), brand, pageable);
        }
        return list;
    }

    private List<CatalogueElastic> getCatalogueElasticList(String text, Pageable pageable, List<ItemElastic> list, String text2, boolean needConvert, String brand, String type) {
        if (list.isEmpty()) {
            if (text2.contains(" "))
                text = String.join(" ", text.split("\\s"));
            text2 += "?";
            list = repo.findAllNotStrong(text2, pageable);
            if (list.isEmpty() && needConvert) {
                list = repo.findAllByTypeAndBrand(convert(text2), brand, type, pageable);
            }
        }
        return get(list, text, brand);
    }

    private List<ItemElastic> getItemElasticListForEmptyBrand(String text, Pageable pageable, Long catalogueId, String type) {
        List<ItemElastic> list;
        String emptyType = "?";

        if (catalogueId == null) {
            if (type.equals(emptyType)) {
                list = repo.find(text, pageable);
                if (list.isEmpty()) {
                    list = repo.find(convert(text), pageable);
                }
            } else {
                list = repo.findAllByType(text, type, pageable);
                if (list.isEmpty()) {
                    list = repo.findAllByType(convert(text), type, pageable);
                }
            }
        } else {
            String catalogueName = getItemElasticListByCatalogue(text, pageable, true).get(0)
                    .getCatalogue().toLowerCase(Locale.ROOT);
            text = text.replace(catalogueName, "").trim();

            if (!type.equals(emptyType)) {
                list = repo.find(text, catalogueId, type, pageable);
                if (list.isEmpty()) {
                    list = repo.find(convert(text), catalogueId, type, pageable);
                }
            } else {
                list = repo.find(text, catalogueId, pageable);
                if (list.isEmpty()) {
                    list = repo.find(convert(text), catalogueId, pageable);
                }
            }
        }
        return list;
    }

    private List<ItemElastic> getItemElasticListByCatalogue(String text, Pageable pageable, boolean needConvert) {
        List<ItemElastic> list;
        list = repo.findByCatalogue(text, pageable);
        if (list.isEmpty() && needConvert) {
            list = repo.findByCatalogue(convert(text), pageable);
        }
        return list;
    }
}
