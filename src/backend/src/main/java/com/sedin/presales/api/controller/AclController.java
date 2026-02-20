package com.sedin.presales.api.controller;

import com.sedin.presales.config.audit.Audited;
import com.sedin.presales.application.dto.AclEntryDto;
import com.sedin.presales.application.dto.ApiResponse;
import com.sedin.presales.application.dto.BulkGrantAccessRequest;
import com.sedin.presales.application.dto.GrantAccessRequest;
import com.sedin.presales.application.service.AclService;
import com.sedin.presales.domain.enums.Permission;
import com.sedin.presales.domain.enums.ResourceType;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/acl")
@PreAuthorize("hasRole('ADMIN')")
public class AclController {

    private final AclService aclService;

    public AclController(AclService aclService) {
        this.aclService = aclService;
    }

    @Audited(action = "GRANT_ACCESS", resourceType = "ACL")
    @PostMapping
    public ResponseEntity<ApiResponse<AclEntryDto>> grantAccess(
            @Valid @RequestBody GrantAccessRequest request) {
        log.debug("REST request to grant access: {}", request);
        // TODO: Extract grantedBy from security context
        AclEntryDto dto = aclService.grantAccess(request, "system");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto, "Access granted successfully"));
    }

    @Audited(action = "BULK_GRANT_ACCESS", resourceType = "ACL")
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<AclEntryDto>>> bulkGrantAccess(
            @Valid @RequestBody BulkGrantAccessRequest request) {
        log.debug("REST request to bulk grant access: {}", request);
        List<AclEntryDto> dtos = aclService.bulkGrantAccess(request, "system");
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dtos, "Bulk access granted successfully"));
    }

    @Audited(action = "REVOKE_ACCESS", resourceType = "ACL")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> revokeAccess(@PathVariable UUID id) {
        log.debug("REST request to revoke access with id: {}", id);
        aclService.revokeAccess(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Access revoked successfully"));
    }

    @Audited(action = "REVOKE_ALL_ACCESS", resourceType = "ACL")
    @DeleteMapping("/resource/{resourceType}/{resourceId}")
    public ResponseEntity<ApiResponse<Void>> revokeAllAccess(
            @PathVariable ResourceType resourceType,
            @PathVariable UUID resourceId) {
        log.debug("REST request to revoke all access for {}:{}", resourceType, resourceId);
        aclService.revokeAllAccess(resourceType, resourceId);
        return ResponseEntity.ok(ApiResponse.success(null, "All access revoked successfully"));
    }

    @GetMapping("/resource/{resourceType}/{resourceId}")
    public ResponseEntity<ApiResponse<List<AclEntryDto>>> getAccessList(
            @PathVariable ResourceType resourceType,
            @PathVariable UUID resourceId) {
        log.debug("REST request to get access list for {}:{}", resourceType, resourceId);
        List<AclEntryDto> dtos = aclService.getAccessList(resourceType, resourceId);
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/user/{userId}/resource/{resourceType}/{resourceId}")
    public ResponseEntity<ApiResponse<List<AclEntryDto>>> getUserPermissions(
            @PathVariable UUID userId,
            @PathVariable ResourceType resourceType,
            @PathVariable UUID resourceId) {
        log.debug("REST request to get permissions for user {} on {}:{}", userId, resourceType, resourceId);
        List<AclEntryDto> dtos = aclService.getUserPermissions(userId, resourceType, resourceId);
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkPermission(
            @RequestParam UUID userId,
            @RequestParam ResourceType resourceType,
            @RequestParam UUID resourceId,
            @RequestParam Permission permission) {
        log.debug("REST request to check permission for user {} on {}:{} with permission {}",
                userId, resourceType, resourceId, permission);
        boolean hasPermission = aclService.hasPermission(userId, resourceType, resourceId, permission);
        return ResponseEntity.ok(ApiResponse.success(hasPermission));
    }
}
