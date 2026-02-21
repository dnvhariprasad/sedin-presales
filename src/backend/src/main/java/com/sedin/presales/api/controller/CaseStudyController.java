package com.sedin.presales.api.controller;

import com.sedin.presales.application.dto.ApiResponse;
import com.sedin.presales.application.dto.CaseStudyWizardRequest;
import com.sedin.presales.application.dto.CaseStudyWizardResponseDto;
import com.sedin.presales.application.dto.ValidationResultDto;
import com.sedin.presales.application.service.CaseStudyGenerationService;
import com.sedin.presales.application.service.CaseStudyValidationService;
import com.sedin.presales.config.audit.Audited;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/case-studies")
public class CaseStudyController {

    private final CaseStudyGenerationService generationService;
    private final CaseStudyValidationService validationService;

    public CaseStudyController(CaseStudyGenerationService generationService,
                                CaseStudyValidationService validationService) {
        this.generationService = generationService;
        this.validationService = validationService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @Audited(action = "GENERATE_CASE_STUDY", resourceType = "CASE_STUDY")
    public ResponseEntity<ApiResponse<CaseStudyWizardResponseDto>> generate(
            @Valid @RequestBody CaseStudyWizardRequest request) {
        log.info("Generating case study from wizard: {}", request.getTitle());
        CaseStudyWizardResponseDto response = generationService.generateCaseStudy(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Case study generated successfully"));
    }

    @GetMapping("/{documentVersionId}/validation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ValidationResultDto>> getValidation(
            @PathVariable UUID documentVersionId) {
        log.info("Getting validation result for version: {}", documentVersionId);
        ValidationResultDto result = validationService.getValidationResult(documentVersionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{documentVersionId}/revalidate")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "REVALIDATE_CASE_STUDY", resourceType = "CASE_STUDY")
    public ResponseEntity<ApiResponse<Void>> revalidate(@PathVariable UUID documentVersionId) {
        log.info("Triggering revalidation for version: {}", documentVersionId);
        validationService.validateCaseStudy(documentVersionId);
        return ResponseEntity.accepted()
                .body(ApiResponse.success(null, "Revalidation triggered"));
    }
}
