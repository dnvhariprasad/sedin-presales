package com.sedin.presales.api.controller;

import com.sedin.presales.application.dto.ApiResponse;
import com.sedin.presales.application.dto.SearchRequestDto;
import com.sedin.presales.application.dto.SearchResponseDto;
import com.sedin.presales.application.service.SearchService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SearchResponseDto>> search(@Valid @RequestBody SearchRequestDto request) {
        log.debug("POST /api/v1/search - query: '{}'", request.getQuery());
        SearchResponseDto response = searchService.search(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
