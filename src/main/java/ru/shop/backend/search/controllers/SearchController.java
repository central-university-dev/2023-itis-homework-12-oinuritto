package ru.shop.backend.search.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.shop.backend.search.controllers.api.SearchApi;
import ru.shop.backend.search.model.SearchResult;
import ru.shop.backend.search.model.SearchResultElastic;
import ru.shop.backend.search.service.SearchService;

@RestController
@RequiredArgsConstructor
public class SearchController implements SearchApi {
    private final SearchService service;

    @Override
    public SearchResult find(String text, int regionId) {
        return service.getSearchResult(regionId, text);
    }

    @Override
    public ResponseEntity<SearchResultElastic> finds(String text) {
        return ResponseEntity.ok().body(service.getSearchResultElastic(text));
    }
}
