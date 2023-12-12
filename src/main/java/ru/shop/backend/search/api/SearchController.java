package ru.shop.backend.search.api;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.shop.backend.search.model.SearchResult;
import ru.shop.backend.search.model.SearchResultElastic;
import ru.shop.backend.search.service.SearchService;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Поиск", description = "Методы поиска")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService service;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Возвращает результаты поиска для всплывающего окна",
                content = {@Content(mediaType = "application/json",
                        schema = @Schema(implementation = SearchResult.class))}),
        @ApiResponse(responseCode = "400", description = "Ошибка обработки",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Регион не найден",
                content = @Content)})
    @GetMapping
    public SearchResult find(@Parameter(description = "Поисковый запрос")
                             @RequestParam String text,
                             @CookieValue(name="regionId", defaultValue="1") int regionId){
        return service.getSearchResult(regionId, text);
    }
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Возвращает результаты поиска",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = SearchResultElastic.class))}),
            @ApiResponse(responseCode = "400", description = "Ошибка обработки",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Регион не найден",
                    content = @Content)})
    @RequestMapping(method = GET, value = "/by", produces = "application/json;charset=UTF-8")
    public ResponseEntity<SearchResultElastic> finds(@Parameter(description = "Поисковый запрос")
                                                     @RequestParam String text) {
        return ResponseEntity.ok().body(service.getSearchResultElastic(text));
    }
}
