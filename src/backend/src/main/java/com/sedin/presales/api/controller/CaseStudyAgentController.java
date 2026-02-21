package com.sedin.presales.api.controller;

import com.sedin.presales.config.audit.Audited;
import com.sedin.presales.application.dto.ApiResponse;
import com.sedin.presales.application.dto.CaseStudyAgentDto;
import com.sedin.presales.application.dto.CreateCaseStudyAgentRequest;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateCaseStudyAgentRequest;
import com.sedin.presales.application.service.CaseStudyAgentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/case-study-agents")
@PreAuthorize("hasRole('ADMIN')")
public class CaseStudyAgentController {

    private final CaseStudyAgentService caseStudyAgentService;

    public CaseStudyAgentController(CaseStudyAgentService caseStudyAgentService) {
        this.caseStudyAgentService = caseStudyAgentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CaseStudyAgentDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("REST request to list case study agents");
        int safePage = Math.max(page, 0);
        int cappedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, cappedSize);
        PagedResponse<CaseStudyAgentDto> response = caseStudyAgentService.list(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseStudyAgentDto>> getById(@PathVariable UUID id) {
        log.debug("REST request to get case study agent with id: {}", id);
        CaseStudyAgentDto dto = caseStudyAgentService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<CaseStudyAgentDto>> getActiveAgent() {
        log.debug("REST request to get active case study agent");
        CaseStudyAgentDto dto = caseStudyAgentService.getActiveAgent();
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @Audited(action = "CREATE_CASE_STUDY_AGENT", resourceType = "CASE_STUDY_AGENT")
    @PostMapping
    public ResponseEntity<ApiResponse<CaseStudyAgentDto>> create(
            @Valid @RequestBody CreateCaseStudyAgentRequest request) {
        log.debug("REST request to create case study agent");
        CaseStudyAgentDto dto = caseStudyAgentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto, "Created successfully"));
    }

    @Audited(action = "UPDATE_CASE_STUDY_AGENT", resourceType = "CASE_STUDY_AGENT")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseStudyAgentDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCaseStudyAgentRequest request) {
        log.debug("REST request to update case study agent with id: {}", id);
        CaseStudyAgentDto dto = caseStudyAgentService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "Updated successfully"));
    }

    @Audited(action = "ACTIVATE_CASE_STUDY_AGENT", resourceType = "CASE_STUDY_AGENT")
    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<CaseStudyAgentDto>> activate(@PathVariable UUID id) {
        log.debug("REST request to activate case study agent with id: {}", id);
        CaseStudyAgentDto dto = caseStudyAgentService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(dto, "Agent activated successfully"));
    }

    @Audited(action = "DEACTIVATE_CASE_STUDY_AGENT", resourceType = "CASE_STUDY_AGENT")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<CaseStudyAgentDto>> deactivate(@PathVariable UUID id) {
        log.debug("REST request to deactivate case study agent with id: {}", id);
        CaseStudyAgentDto dto = caseStudyAgentService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(dto, "Agent deactivated successfully"));
    }

    @Audited(action = "DELETE_CASE_STUDY_AGENT", resourceType = "CASE_STUDY_AGENT")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        log.debug("REST request to delete case study agent with id: {}", id);
        caseStudyAgentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted successfully"));
    }
}
