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
import java.util.concurrent.atomic.AtomicReference;
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
        if (isNumeric(text)) {
            Integer itemId = repoDb.findBySku(text).stream().findFirst().orElse(null);
            if (itemId == null) {
                var catalogue = getByName(text);
                if (!catalogue.isEmpty()) {
                    return new SearchResultElastic(catalogue);
                }
                return new SearchResultElastic(getAllFull(text));
            } else {
                return new SearchResultElastic(getByItemId(itemId.toString()));
            }
        }
        return new SearchResultElastic(getAllFull(text));
    }

    public synchronized SearchResult getSearchResult(Integer regionId, String text){
        List<CatalogueElastic> result = checkForNumericAndGetCatalogueList(text);

        if(result == null) {
            result = getAll(text);
        }

        List<Item> items = getItemsFromCatalogueList(regionId, result);

        Set<String> catUrls = new HashSet<>();
        String finalBrand = getBrandFromCatalogueList(result);
        List<Category> categories = getCategoriesFromItemList(items, catUrls, finalBrand);

        return new SearchResult(
                items,
                categories,
                getTypeQueriesFromCatalogueList(result)
        );
    }

    public synchronized List<CatalogueElastic> getAll(String text){
        return getAll(text, pageableSmall);
    }

    public List<CatalogueElastic> getAll(String text, Pageable pageable){
        String type = "";
        List<ItemElastic> list = new ArrayList<>();
        String brand = "", text2 =text;
        Long catalogueId = null;
        boolean needConvert = true;
        if(isContainErrorChar(text)) {
            text = convert(text);
            needConvert = false;
        }
        if(needConvert && isContainErrorChar(convert(text))) {
            needConvert = false;
        }
        if(text.contains(" "))
            for(String queryWord: text.split("\\s")){
                list = repo.findAllByBrand(queryWord, pageable);
                if(list.isEmpty()&&needConvert){
                    list = repo.findAllByBrand(convert(text), pageable);
                }
                if(!list.isEmpty()) {
                        text = text.replace(queryWord, "").trim().replace("  ", " ");
                        brand = list.get(0).getBrand();
                        break;

                }

            }
        list = repo.findAllByType(text,pageable);
        if(list.isEmpty()&&needConvert){
            list = repo.findAllByType(convert(text), pageable);
        }
        if(!list.isEmpty()) {
            type=(list.stream().map( itemElastic ->
                    itemElastic.getType()).min(Comparator.comparingInt(x-> x.length())).get());
        } else {
            for (String queryWord : text.split("\\s")) {
                list = repo.findAllByType(queryWord,pageable);
                if(list.isEmpty()&&needConvert){
                    list = repo.findAllByType(convert(text), pageable);
                }
                if (!list.isEmpty()) {
                    text = text.replace(queryWord, "");
                    type=(list.stream().map( itemElastic ->
                            itemElastic.getType()).min(Comparator.comparingInt(x-> x.length())).get());
                }
            }
        }
        if(brand.isEmpty()){
            list = repo.findByCatalogue(text, pageable);
            if(list.isEmpty()&&needConvert){
                list = repo.findByCatalogue(convert(text), pageable);
            }
            if(!list.isEmpty()){
                catalogueId = list.get(0).getCatalogueId();
            }
        }
        text = text.trim();
        if(text.isEmpty() && !brand.isEmpty())
            return Collections.singletonList(new CatalogueElastic(list.get(0).getCatalogue(), list.get(0).getCatalogueId(), null, brand));
        text += "?";
        if(brand.isEmpty()) {
                type += "?";
                if(catalogueId == null)
                    if(type.isEmpty()) {
                        list = repo.find(text, pageable);
                        if (list.isEmpty()) {
                            list = repo.find(convert(text), pageable);
                        }
                    }
                    else {
                        list = repo.findAllByType(text, type, pageable);
                        if (list.isEmpty()) {
                            list = repo.findAllByType(convert(text), type, pageable);
                        }
                    }
                else
                    if(type.isEmpty()) {
                        list = repo.find(text, catalogueId, type, pageable);
                        if (list.isEmpty()) {
                            list = repo.find(convert(text), catalogueId, type, pageable);
                        }
                    }
                    else {
                        list = repo.find(text, catalogueId, pageable);
                        if (list.isEmpty()) {
                            list = repo.find(convert(text), catalogueId, pageable);
                        }
                    }

        }else {
            if(type.isEmpty()) {
                list = repo.findAllByBrand(text, brand, pageable);
                if (list.isEmpty()) {
                    list = repo.findAllByBrand(convert(text), brand, pageable);
                }
            }else {
                type += "?";
                list = repo.findAllByTypeAndBrand(text, brand, type, pageable);
                if (list.isEmpty()) {
                    list = repo.findAllByTypeAndBrand(convert(text), brand, type, pageable);
                }
            }
        }

        if(list.isEmpty()){
            if(text2.contains(" "))
                text = Arrays.stream(text.split("\\s")).collect(Collectors.joining(" "));
            text2 += "?";
            list  =repo.findAllNotStrong(text2, pageable);
            if (list.isEmpty()&&needConvert) {
                list = repo.findAllByTypeAndBrand(convert(text2), brand, type, pageable);
            }
        }
        return get(list, text, brand);
    }

    private List<CatalogueElastic> get(List<ItemElastic> list, String name, String brand){
        Map<String, List<ItemElastic>> map = new HashMap<>();
        AtomicReference<ItemElastic> searchedItem = new AtomicReference<>();
        list.stream().forEach(
                i ->
                {
                    if(name.replace("?","").equals(i.getName())) {
                        searchedItem.set(i);
                    }
                    if(name.replace("?","").endsWith(i.getName()) && name.replace("?","").startsWith(i.getType())) {
                        searchedItem.set(i);
                    }
                    if(!map.containsKey(i.getCatalogue())) {
                        map.put(i.getCatalogue(), new ArrayList<>());
                    }
                    map.get(i.getCatalogue()).add(i);
                }
        );
        if(brand.isEmpty())
            brand = null;
        if(searchedItem.get()!=null){
            ItemElastic i = searchedItem.get();
            return Collections.singletonList(new CatalogueElastic(i.getCatalogue(), i.getCatalogueId(), Collections.singletonList(i),brand));
        }
        List<CatalogueElastic> cats = new ArrayList<>();
        String finalBrand = brand;
        return map.keySet().stream().map(c ->
                new CatalogueElastic(c, map.get(c).get(0).getCatalogueId(), map.get(c), finalBrand)).collect(Collectors.toList());
    }
    public List<CatalogueElastic> getByName(String num){
        List<ItemElastic> list = new ArrayList<>();
        list = repo.findAllByName(".*" + num + ".*", pageable);
        return get(list, num, "");
    }
    public List<CatalogueElastic> getByItemId(String itemId) {
        var list = repo.findByItemId(itemId, PageRequest.of(0, 1));
        return Collections.singletonList(new CatalogueElastic(list.get(0).getCatalogue(), list.get(0).getCatalogueId(), list, list.get(0).getBrand()));
    }

    public static String convert(String message) {
        boolean result = message.matches(".*\\p{InCyrillic}.*");
        char[] ru = {'й','ц','у','к','е','н','г','ш','щ','з','х','ъ','ф','ы','в','а','п','р','о','л','д','ж','э', 'я','ч', 'с','м','и','т','ь','б', 'ю','.',
                ' ','0','1','2','3','4','5','6','7','8','9','-'};
        char[] en = {'q','w','e','r','t','y','u','i','o','p','[',']','a','s','d','f','g','h','j','k','l',';','"','z','x','c','v','b','n','m',',','.','/',
                ' ','0','1','2','3','4','5','6','7','8','9','-'};
        StringBuilder builder = new StringBuilder();

        if (result) {
            for (int i = 0; i < message.length(); i++) {
                for (int j = 0; j < ru.length; j++ ) {
                    if (message.charAt(i) == ru[j]) {
                        builder.append(en[j]);
                    }
                }
            }
        } else {
            for (int i = 0; i < message.length(); i++) {
                for (int j = 0; j < en.length; j++ ) {
                    if (message.charAt(i) == en[j]) {
                        builder.append(ru[j]);
                    }
                }
            }
        }
        return builder.toString();
    }
    private Boolean isContainErrorChar(String text){
        if(text.contains("[") || text.contains("]") || text.contains("\"") || text.contains("/") || text.contains(";"))
            return true;
        return false;
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

    private List<Category> getCategoriesFromItemList(List<Item> items, Set<String> catUrls, String finalBrand) {
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
        return items.stream().map(Item::getItemId).collect(Collectors.toList());
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
                .flatMap(category -> category.getItems().stream())
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
            }
            else {
                result = getByItemId(itemId.toString());
            }
        }
        return result;
    }
}
