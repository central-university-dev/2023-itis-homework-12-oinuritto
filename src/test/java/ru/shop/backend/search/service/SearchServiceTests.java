package ru.shop.backend.search.service;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.shop.backend.search.model.ItemElastic;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
@Testcontainers
public class SearchServiceTests {
    @Autowired
    private SearchService service;
    private final Pageable pageable = PageRequest.of(0, 150);

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:10")
                    .withInitScript("test_init.sql");
    @Container
    public static ElasticsearchContainer elasticsearchContainer =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.4")
                    .withEnv("discovery.type", "single-node")
                    .dependsOn(postgreSQLContainer);


    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.rest.uris", () -> elasticsearchContainer.getHttpHostAddress());
    }


    @Test
    @Order(1)
    public void waitUntilOtherTests() throws InterruptedException {
        Thread.sleep(2000);
    }

    @Nested
    @DisplayName("Test getByName method")
    class TestGetByNameMethod {
        @Test
        public void testGetByName_forExistTextName_shouldReturnCorrectly() {
            String text = "macbook";
            var result = service.getByName(text);

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getItems().size());
            assertEquals(8, result.get(0).getItems().get(0).getItemId());
            assertEquals(1, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetByName_forExistNumericInName_shouldReturnCorrectly() {
            String text = "2022";
            var result = service.getByName(text);

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getItems().size());
            assertEquals(8, result.get(0).getItems().get(0).getItemId());
            assertEquals(1, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetByName_forNotExistName_shouldReturnCorrectly() {
            String text = "notExistName";
            var result = service.getByName(text);

            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("Test getByItemId method")
    class TestGetByItemIdMethod {
        @Test
        public void testGetByItemId_forExistItemId_shouldReturnCorrectly() {
            String text = "8";
            var result = service.getByItemId(text);

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getItems().size());
            assertEquals(8, result.get(0).getItems().get(0).getItemId());
            assertEquals(1, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetByItemId_forNotExistItemId_shouldReturnCorrectly() {
            String text = "100";
            var result = service.getByItemId(text);

            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("Test getAll method")
    class TestGetAllMethod {
        @Test
        public void testGetAll_forNotExistText_shouldReturnEmptyResponse() {
            String text = "notExistText";
            var result = service.getAll(text, pageable);

            assertEquals(0, result.size());
        }

        @Test
        public void testGetAll_forItemName_shouldReturnCorrectly() {
            String text = "macbook";
            var result = service.getAll(text, pageable);

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getItems().size());
            assertEquals(8, result.get(0).getItems().get(0).getItemId());
            assertEquals(1, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetAll_forType_shouldReturnCorrectly() {
            String text = "clothing";
            var result = service.getAll(text, pageable);

            assertEquals(1, result.size());
            assertEquals(3, result.get(0).getItems().size());
            for (ItemElastic i : result.get(0).getItems()) {
                assertEquals(text.toLowerCase(Locale.ROOT), i.getType().toLowerCase(Locale.ROOT));
            }
            assertEquals(2, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetAll_forBrand_shouldReturnCorrectly() {
            String text = "Apple";
            var result = service.getAll(text, pageable);

            assertEquals(1, result.size());
            assertEquals(text.toLowerCase(Locale.ROOT), result.get(0).getBrand().toLowerCase(Locale.ROOT));
            assertEquals(1, result.get(0).getItems().size());
            assertEquals(8, result.get(0).getItems().get(0).getItemId());
            assertEquals(1, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetAll_forCatalogue_shouldReturnCorrectly() {
            String text = "Fashion";
            var result = service.getAll(text, pageable);

            assertEquals(1, result.size());
            assertEquals(3, result.get(0).getItems().size());
            assertEquals(2, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetAll_forTypeAndItemName_shouldReturnCorrectly() {
            String text = "clothing shirt";
            var result = service.getAll(text, pageable);

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getItems().size());
            assertEquals(2, result.get(0).getItems().get(0).getItemId());
            assertEquals(2, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetAll_forTypeAndBrand_shouldReturnCorrectly() {
            String text = "clothing cool";
            var result = service.getAll(text, pageable);

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getItems().size());
            assertEquals(7, result.get(0).getItems().get(0).getItemId());
            assertEquals(2, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetAll_forBrandAndItemName_shouldReturnCorrectly() {
            String text = "brand shirt";
            var result = service.getAll(text, pageable);

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getItems().size());
            assertEquals(2, result.get(0).getItems().get(0).getItemId());
            assertEquals(2, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetAll_forCatalogueAndItemName_shouldReturnCorrectly() {
            String text = "Fashion shirt";
            var result = service.getAll(text, pageable);

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getItems().size());
            assertEquals(2, result.get(0).getItems().get(0).getItemId());
            assertEquals(2, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetAll_forCatalogueAndType_shouldReturnCorrectly() {
            String text = "Fashion clothing";
            var result = service.getAll(text, pageable);

            assertEquals(1, result.size());
            assertEquals(3, result.get(0).getItems().size());
            assertEquals(2, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetAll_forCatalogueAndTypeAndItemName_shouldReturnCorrectly() {
            String text = "Fashion clothing shirt";
            var result = service.getAll(text, pageable);

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getItems().size());
            assertEquals(2, result.get(0).getItems().get(0).getItemId());
            assertEquals(2, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetAll_forDescription_shouldReturnCorrectly() {
            String text = "Powerful laptop with Z";
            var result = service.getAll(text, pageable);

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getItems().size());
            assertEquals(3, result.get(0).getItems().get(0).getItemId());
            assertEquals(1, result.get(0).getCatalogueId());
        }

        @Test
        public void testGetAll_forConvertedText_shouldReturnCorrectly() {
            String text = "афыршщт сдщерштп ыршке"; // fashion clothing shirt
            var result = service.getAll(text, pageable);

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getItems().size());
            assertEquals(2, result.get(0).getItems().get(0).getItemId());
            assertEquals(2, result.get(0).getCatalogueId());
        }
    }

    @Nested
    @DisplayName("Test getSearchResult method")
    class TestGetSearchResultMethod {
        @Test
        public void testGetSearchResult_forExistSkuAndExistRegionId_shouldReturnCorrectly() {
            String text = "12345";
            int regionId = 1;

            var result = service.getSearchResult(regionId, text);

            assertEquals(1, result.getItems().size());
            assertEquals(1, result.getItems().get(0).getItemId());
            assertEquals(1, result.getCategories().size());
            assertEquals("Electronics", result.getCategories().get(0).getName());
        }

        @Test
        public void testGetSearchResult_forNotExistRegionId_shouldReturnCorrectly() {
            String text = "12345";
            int regionId = 10;

            var result = service.getSearchResult(regionId, text);

            assertEquals(0, result.getItems().size());
            assertEquals(0, result.getCategories().size());
        }

        @Test
        public void testGetSearchResult_forNotNumericText_shouldReturnCorrectly() {
            String text = "macbook";
            int regionId = 1;

            var result = service.getSearchResult(regionId, text);

            assertEquals(1, result.getItems().size());
            assertEquals(8, result.getItems().get(0).getItemId());
            assertEquals(1, result.getCategories().size());
            assertEquals("Electronics", result.getCategories().get(0).getName());
        }

        @Test
        public void testGetSearchResult_forNotExistText_shouldReturnCorrectly() {
            String text = "notExistText";
            int regionId = 10;

            var result = service.getSearchResult(regionId, text);

            assertEquals(0, result.getItems().size());
            assertEquals(0, result.getCategories().size());
        }
    }

    @Nested
    @DisplayName("Test getSearchResultElastic method")
    class TestGetSearchResultElasticMethod {
        @Test
        public void testGetSearchResultElastic_forExistSku_shouldReturnCorrectly() {
            String text = "12345";

            var result = service.getSearchResultElastic(text);
            var resultList = result.getResult();

            assertEquals(1, resultList.size());
            assertEquals(1, resultList.get(0).getItems().size());
            assertEquals(1, resultList.get(0).getItems().get(0).getItemId());
            assertEquals(1, resultList.get(0).getCatalogueId());
        }


        @Test
        public void testGetSearchResultElastic_forNotNumericText_shouldReturnCorrectly() {
            String text = "macbook";

            var result = service.getSearchResultElastic(text);
            var resultList = result.getResult();

            assertEquals(1, resultList.size());
            assertEquals(1, resultList.get(0).getItems().size());
            assertEquals(8, resultList.get(0).getItems().get(0).getItemId());
            assertEquals(1, resultList.get(0).getCatalogueId());
        }

        @Test
        public void testGetSearchResultElastic_forNotExistText_shouldReturnEmptyList() {
            String text = "notExistText";

            var result = service.getSearchResultElastic(text);
            var resultList = result.getResult();

            assertEquals(0, resultList.size());
        }
    }
}
