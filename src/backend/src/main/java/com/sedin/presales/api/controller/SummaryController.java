package com.sedin.presales.api.controller;

import com.sedin.presales.application.dto.ApiResponse;
import com.sedin.presales.application.dto.SummaryResponseDto;
import com.sedin.presales.application.service.SummaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents/{documentId}")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SummaryResponseDto>> getSummary(@PathVariable UUID documentId) {
        log.debug("GET /api/v1/documents/{}/summary", documentId);
        SummaryResponseDto summary = summaryService.getSummaryStatus(documentId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/summary/regenerate")
    public ResponseEntity<ApiResponse<SummaryResponseDto>> regenerateSummary(@PathVariable UUID documentId) {
        log.debug("POST /api/v1/documents/{}/summary/regenerate", documentId);
        SummaryResponseDto summary = summaryService.regenerateSummary(documentId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(summary, "Summary regeneration initiated"));
    }
}
