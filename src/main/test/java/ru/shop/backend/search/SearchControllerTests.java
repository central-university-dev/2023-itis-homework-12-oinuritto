package ru.shop.backend.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.servlet.http.Cookie;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class SearchControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:15")
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

    @Nested
    @DisplayName("Test SearchResult find method")
    class TestFindMethod {
        @Test
        public void testFind_forExistNumericSKUAndExistRegionId() throws Exception {
            String searchText = "12345";
            Cookie regionIdCookie = new Cookie("regionId", "1");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [\n" +
                                    "    {\n" +
                                    "      \"price\": 10000,\n" +
                                    "      \"name\": \"Smartphone X\",\n" +
                                    "      \"url\": \"example.com/item1\",\n" +
                                    "      \"image\": \"i1\",\n" +
                                    "      \"itemId\": 1,\n" +
                                    "      \"cat\": \"electronics\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"categories\": [\n" +
                                    "    {\n" +
                                    "      \"name\": \"Electronics\",\n" +
                                    "      \"parentName\": \"Electronics\",\n" +
                                    "      \"url\": \"/cat/Electronics/brands/ant\",\n" +
                                    "      \"parentUrl\": \"/cat/Electronics\",\n" +
                                    "      \"image\": \"example.com/electronics\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"typeQueries\": [\n" +
                                    "    {\n" +
                                    "      \"type\": \"SEE_ALSO\",\n" +
                                    "      \"text\": \"electronics Ant\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}")
                    );
        }

        @Test
        public void testFind_forNotExistTextAndAnyRegionId() throws Exception {
            String searchText = "text";
            Cookie regionIdCookie = new Cookie("regionId", "1");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [],\n" +
                                    "  \"categories\": [],\n" +
                                    "  \"typeQueries\": []\n" +
                                    "}")
                    );
        }

        @Test
        public void testFind_forExistTextAndNotExistRegionId() throws Exception {
            String searchText = "clothing";
            Cookie regionIdCookie = new Cookie("regionId", "10");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [],\n" +
                                    "  \"categories\": [],\n" +
                                    "  \"typeQueries\": [\n" +
                                    "    {\n" +
                                    "      \"type\": \"SEE_ALSO\",\n" +
                                    "      \"text\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}")
                    );
        }

        @Test
        public void testFind_forExistTypeAndExistRegionId() throws Exception {
            String searchText = "clothing";
            Cookie regionIdCookie = new Cookie("regionId", "1");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [\n" +
                                    "    {\n" +
                                    "      \"price\": 500,\n" +
                                    "      \"name\": \"T-Shirt Y\",\n" +
                                    "      \"url\": \"example.com/item2\",\n" +
                                    "      \"image\": \"i2\",\n" +
                                    "      \"itemId\": 2,\n" +
                                    "      \"cat\": \"clothing\"\n" +
                                    "    },\n" +
                                    "    {\n" +
                                    "      \"price\": 12000,\n" +
                                    "      \"name\": \"Shoes\",\n" +
                                    "      \"url\": \"example.com/item7\",\n" +
                                    "      \"image\": \"i7\",\n" +
                                    "      \"itemId\": 7,\n" +
                                    "      \"cat\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"categories\": [\n" +
                                    "    {\n" +
                                    "      \"name\": \"Fashion\",\n" +
                                    "      \"parentName\": \"Fashion\",\n" +
                                    "      \"url\": \"/cat/Fashion\",\n" +
                                    "      \"parentUrl\": \"/cat/Fashion\",\n" +
                                    "      \"image\": \"example.com/fashion\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"typeQueries\": [\n" +
                                    "    {\n" +
                                    "      \"type\": \"SEE_ALSO\",\n" +
                                    "      \"text\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}")
                    );
        }

        @Test
        public void testFind_forCatalogueAndType() throws Exception {
            String searchText = "fashion clothing";
            Cookie regionIdCookie = new Cookie("regionId", "1");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [\n" +
                                    "    {\n" +
                                    "      \"price\": 500,\n" +
                                    "      \"name\": \"T-Shirt Y\",\n" +
                                    "      \"url\": \"example.com/item2\",\n" +
                                    "      \"image\": \"i2\",\n" +
                                    "      \"itemId\": 2,\n" +
                                    "      \"cat\": \"clothing\"\n" +
                                    "    },\n" +
                                    "    {\n" +
                                    "      \"price\": 12000,\n" +
                                    "      \"name\": \"Shoes\",\n" +
                                    "      \"url\": \"example.com/item7\",\n" +
                                    "      \"image\": \"i7\",\n" +
                                    "      \"itemId\": 7,\n" +
                                    "      \"cat\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"categories\": [\n" +
                                    "    {\n" +
                                    "      \"name\": \"Fashion\",\n" +
                                    "      \"parentName\": \"Fashion\",\n" +
                                    "      \"url\": \"/cat/Fashion\",\n" +
                                    "      \"parentUrl\": \"/cat/Fashion\",\n" +
                                    "      \"image\": \"example.com/fashion\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"typeQueries\": [\n" +
                                    "    {\n" +
                                    "      \"type\": \"SEE_ALSO\",\n" +
                                    "      \"text\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}")
                    );
        }

        @Test
        public void testFind_forCatalogueAndItemName() throws Exception {
            String searchText = "fashion shirt";
            Cookie regionIdCookie = new Cookie("regionId", "1");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [\n" +
                                    "    {\n" +
                                    "      \"price\": 500,\n" +
                                    "      \"name\": \"T-Shirt Y\",\n" +
                                    "      \"url\": \"example.com/item2\",\n" +
                                    "      \"image\": \"i2\",\n" +
                                    "      \"itemId\": 2,\n" +
                                    "      \"cat\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"categories\": [\n" +
                                    "    {\n" +
                                    "      \"name\": \"Fashion\",\n" +
                                    "      \"parentName\": \"Fashion\",\n" +
                                    "      \"url\": \"/cat/Fashion\",\n" +
                                    "      \"parentUrl\": \"/cat/Fashion\",\n" +
                                    "      \"image\": \"example.com/fashion\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"typeQueries\": [\n" +
                                    "    {\n" +
                                    "      \"type\": \"SEE_ALSO\",\n" +
                                    "      \"text\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}")
                    );
        }

        @Test
        public void testFind_forTypeAndItemName() throws Exception {
            String searchText = "clothing shirt";
            Cookie regionIdCookie = new Cookie("regionId", "1");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [\n" +
                                    "    {\n" +
                                    "      \"price\": 500,\n" +
                                    "      \"name\": \"T-Shirt Y\",\n" +
                                    "      \"url\": \"example.com/item2\",\n" +
                                    "      \"image\": \"i2\",\n" +
                                    "      \"itemId\": 2,\n" +
                                    "      \"cat\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"categories\": [\n" +
                                    "    {\n" +
                                    "      \"name\": \"Fashion\",\n" +
                                    "      \"parentName\": \"Fashion\",\n" +
                                    "      \"url\": \"/cat/Fashion\",\n" +
                                    "      \"parentUrl\": \"/cat/Fashion\",\n" +
                                    "      \"image\": \"example.com/fashion\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"typeQueries\": [\n" +
                                    "    {\n" +
                                    "      \"type\": \"SEE_ALSO\",\n" +
                                    "      \"text\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}")
                    );
        }

        @Test
        public void testFind_forItemName() throws Exception {
            String searchText = "shirt";
            Cookie regionIdCookie = new Cookie("regionId", "1");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [\n" +
                                    "    {\n" +
                                    "      \"price\": 500,\n" +
                                    "      \"name\": \"T-Shirt Y\",\n" +
                                    "      \"url\": \"example.com/item2\",\n" +
                                    "      \"image\": \"i2\",\n" +
                                    "      \"itemId\": 2,\n" +
                                    "      \"cat\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"categories\": [\n" +
                                    "    {\n" +
                                    "      \"name\": \"Fashion\",\n" +
                                    "      \"parentName\": \"Fashion\",\n" +
                                    "      \"url\": \"/cat/Fashion\",\n" +
                                    "      \"parentUrl\": \"/cat/Fashion\",\n" +
                                    "      \"image\": \"example.com/fashion\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"typeQueries\": [\n" +
                                    "    {\n" +
                                    "      \"type\": \"SEE_ALSO\",\n" +
                                    "      \"text\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}")
                    );
        }

        @Test
        public void testFind_forBrandAndItemName() throws Exception {
            String searchText = "cool shoes";
            Cookie regionIdCookie = new Cookie("regionId", "1");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [\n" +
                                    "    {\n" +
                                    "      \"price\": 12000,\n" +
                                    "      \"name\": \"Shoes\",\n" +
                                    "      \"url\": \"example.com/item7\",\n" +
                                    "      \"image\": \"i7\",\n" +
                                    "      \"itemId\": 7,\n" +
                                    "      \"cat\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"categories\": [\n" +
                                    "    {\n" +
                                    "      \"name\": \"Fashion\",\n" +
                                    "      \"parentName\": \"Fashion\",\n" +
                                    "      \"url\": \"/cat/Fashion/brands/cool\",\n" +
                                    "      \"parentUrl\": \"/cat/Fashion\",\n" +
                                    "      \"image\": \"example.com/fashion\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"typeQueries\": [\n" +
                                    "    {\n" +
                                    "      \"type\": \"SEE_ALSO\",\n" +
                                    "      \"text\": \"clothing Cool\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}")
                    );
        }

        @Test
        public void testFind_forTypeAndBrand() throws Exception {
            String searchText = "clothing cool";
            Cookie regionIdCookie = new Cookie("regionId", "1");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [\n" +
                                    "    {\n" +
                                    "      \"price\": 12000,\n" +
                                    "      \"name\": \"Shoes\",\n" +
                                    "      \"url\": \"example.com/item7\",\n" +
                                    "      \"image\": \"i7\",\n" +
                                    "      \"itemId\": 7,\n" +
                                    "      \"cat\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"categories\": [\n" +
                                    "    {\n" +
                                    "      \"name\": \"Fashion\",\n" +
                                    "      \"parentName\": \"Fashion\",\n" +
                                    "      \"url\": \"/cat/Fashion/brands/cool\",\n" +
                                    "      \"parentUrl\": \"/cat/Fashion\",\n" +
                                    "      \"image\": \"example.com/fashion\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"typeQueries\": [\n" +
                                    "    {\n" +
                                    "      \"type\": \"SEE_ALSO\",\n" +
                                    "      \"text\": \"clothing Cool\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}")
                    );
        }

        @Test
        public void testFind_forExistNumericInName() throws Exception {
            String searchText = "2022";
            Cookie regionIdCookie = new Cookie("regionId", "1");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [\n" +
                                    "    {\n" +
                                    "      \"price\": 120000,\n" +
                                    "      \"name\": \"Macbook Air 2022\",\n" +
                                    "      \"url\": \"example.com/item8\",\n" +
                                    "      \"image\": \"i8\",\n" +
                                    "      \"itemId\": 8,\n" +
                                    "      \"cat\": \"electronics\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"categories\": [\n" +
                                    "    {\n" +
                                    "      \"name\": \"Electronics\",\n" +
                                    "      \"parentName\": \"Electronics\",\n" +
                                    "      \"url\": \"/cat/Electronics\",\n" +
                                    "      \"parentUrl\": \"/cat/Electronics\",\n" +
                                    "      \"image\": \"example.com/electronics\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"typeQueries\": [\n" +
                                    "    {\n" +
                                    "      \"type\": \"SEE_ALSO\",\n" +
                                    "      \"text\": \"electronics\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}")
                    );
        }

        @Test
        public void testFind_forBrand() throws Exception {
            String searchText = "Brand";
            Cookie regionIdCookie = new Cookie("regionId", "1");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [\n" +
                                    "    {\n" +
                                    "      \"price\": 500,\n" +
                                    "      \"name\": \"T-Shirt Y\",\n" +
                                    "      \"url\": \"example.com/item2\",\n" +
                                    "      \"image\": \"i2\",\n" +
                                    "      \"itemId\": 2,\n" +
                                    "      \"cat\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"categories\": [\n" +
                                    "    {\n" +
                                    "      \"name\": \"Fashion\",\n" +
                                    "      \"parentName\": \"Fashion\",\n" +
                                    "      \"url\": \"/cat/Fashion/brands/brand\",\n" +
                                    "      \"parentUrl\": \"/cat/Fashion\",\n" +
                                    "      \"image\": \"example.com/fashion\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"typeQueries\": [\n" +
                                    "    {\n" +
                                    "      \"type\": \"SEE_ALSO\",\n" +
                                    "      \"text\": \"clothing Brand\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}")
                    );
        }

        @Test
        public void testFind_forCatalogueAndTypeAndItemName() throws Exception {
            String searchText = "fashion clothing shirt";
            Cookie regionIdCookie = new Cookie("regionId", "1");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [\n" +
                                    "    {\n" +
                                    "      \"price\": 500,\n" +
                                    "      \"name\": \"T-Shirt Y\",\n" +
                                    "      \"url\": \"example.com/item2\",\n" +
                                    "      \"image\": \"i2\",\n" +
                                    "      \"itemId\": 2,\n" +
                                    "      \"cat\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"categories\": [\n" +
                                    "    {\n" +
                                    "      \"name\": \"Fashion\",\n" +
                                    "      \"parentName\": \"Fashion\",\n" +
                                    "      \"url\": \"/cat/Fashion\",\n" +
                                    "      \"parentUrl\": \"/cat/Fashion\",\n" +
                                    "      \"image\": \"example.com/fashion\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"typeQueries\": [\n" +
                                    "    {\n" +
                                    "      \"type\": \"SEE_ALSO\",\n" +
                                    "      \"text\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}")
                    );
        }

        @Test
        public void testFind_forConvertedText() throws Exception {
            String searchText = "сдщерштп"; // clothing
            Cookie regionIdCookie = new Cookie("regionId", "1");

            mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                            .param("text", searchText)
                            .cookie(regionIdCookie))
                    .andExpectAll(
                            status().isOk(),
                            content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                            content().json("{\n" +
                                    "  \"items\": [\n" +
                                    "    {\n" +
                                    "      \"price\": 500,\n" +
                                    "      \"name\": \"T-Shirt Y\",\n" +
                                    "      \"url\": \"example.com/item2\",\n" +
                                    "      \"image\": \"i2\",\n" +
                                    "      \"itemId\": 2,\n" +
                                    "      \"cat\": \"clothing\"\n" +
                                    "    },\n" +
                                    "    {\n" +
                                    "      \"price\": 12000,\n" +
                                    "      \"name\": \"Shoes\",\n" +
                                    "      \"url\": \"example.com/item7\",\n" +
                                    "      \"image\": \"i7\",\n" +
                                    "      \"itemId\": 7,\n" +
                                    "      \"cat\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"categories\": [\n" +
                                    "    {\n" +
                                    "      \"name\": \"Fashion\",\n" +
                                    "      \"parentName\": \"Fashion\",\n" +
                                    "      \"url\": \"/cat/Fashion\",\n" +
                                    "      \"parentUrl\": \"/cat/Fashion\",\n" +
                                    "      \"image\": \"example.com/fashion\"\n" +
                                    "    }\n" +
                                    "  ],\n" +
                                    "  \"typeQueries\": [\n" +
                                    "    {\n" +
                                    "      \"type\": \"SEE_ALSO\",\n" +
                                    "      \"text\": \"clothing\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}")
                    );
        }
    }
}
