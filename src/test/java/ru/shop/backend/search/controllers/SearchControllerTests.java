package ru.shop.backend.search.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.shop.backend.search.model.*;
import ru.shop.backend.search.service.SearchService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(SearchController.class)
public class SearchControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private SearchService service;
    private SearchResult searchResult;
    private SearchResultElastic searchResultElastic;

    @BeforeEach
    public void init() {
        Item item = new Item(500, "Shirt", "url", "image", 2, "clothing");
        Category category = new Category("Fashion", "Fashion", "/cat/Fashion",
                "/cat/Fashion", "example.com/Fashion");
        TypeHelpText typeHelpText = new TypeHelpText(TypeOfQuery.SEE_ALSO, "clothing");

        ItemElastic itemElastic = new ItemElastic("Shirt", "fullText", 2L, 2L,
                "Fashion", "Brand", "clothing", "description");
        CatalogueElastic catalogueElastic = new CatalogueElastic(itemElastic.getCatalogue(), itemElastic.getCatalogueId(),
                List.of(itemElastic), itemElastic.getBrand());
        List<CatalogueElastic> list = new ArrayList<>();
        list.add(catalogueElastic);

        this.searchResultElastic = new SearchResultElastic(list);
        this.searchResult = new SearchResult(List.of(item), List.of(category), List.of(typeHelpText));
    }

    @Test
    public void testFind_forExistText_shouldReturnCorrectly() throws Exception {
        when(service.getSearchResult(any(), any())).thenReturn(searchResult);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                        .param("text", "someText")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(searchResult.getItems().size())))
                .andExpect(jsonPath("$.categories", hasSize(searchResult.getCategories().size())))
                .andExpect(jsonPath("$.typeQueries", hasSize(searchResult.getTypeQueries().size())));
    }

    @Test
    public void testFind_forNotExistText_shouldReturnEmptyResult() throws Exception {
        when(service.getSearchResult(any(), any())).thenReturn(
                new SearchResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyList()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/search")
                        .param("text", "someText")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.categories", hasSize(0)))
                .andExpect(jsonPath("$.typeQueries", hasSize(0)));
    }

    @Test
    public void testFinds_forExistText_shouldReturnCorrectly() throws Exception {
        when(service.getSearchResultElastic(any())).thenReturn(searchResultElastic);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/search/by")
                        .param("text", "someText")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result",
                        hasSize(searchResultElastic.getResult().size())))
                .andExpect(jsonPath("$.result[0].name",
                        is(searchResultElastic.getResult().get(0).getName())))
                .andExpect(jsonPath("$.result[0].items",
                        hasSize(searchResultElastic.getResult().get(0).getItems().size())))
                .andExpect(jsonPath("$.result[0].catalogueId",
                        is(searchResultElastic.getResult().get(0).getCatalogueId().intValue())))
                .andExpect(jsonPath("$.result[0].brand",
                        is(searchResultElastic.getResult().get(0).getBrand())));
    }

    @Test
    public void testFinds_forNotExistText_shouldReturnEmptyResult() throws Exception {
        when(service.getSearchResultElastic(any())).thenReturn(new SearchResultElastic(Collections.emptyList()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/search/by")
                        .param("text", "someText")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(0)));
    }

}
