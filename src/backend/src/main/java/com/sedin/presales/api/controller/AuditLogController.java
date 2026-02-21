package com.sedin.presales.api.controller;

import com.sedin.presales.application.dto.ApiResponse;
import com.sedin.presales.application.dto.AuditLogDto;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate) {
        log.debug("REST request to list audit logs with userEmail={}, action={}, resourceType={}", userEmail, action, resourceType);
        int safePage = Math.max(page, 0);
        int cappedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, cappedSize);
        PagedResponse<AuditLogDto> response = auditLogService.list(pageable, userEmail, action, resourceType, fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogDto>> getById(@PathVariable UUID id) {
        log.debug("REST request to get audit log by id: {}", id);
        AuditLogDto dto = auditLogService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
