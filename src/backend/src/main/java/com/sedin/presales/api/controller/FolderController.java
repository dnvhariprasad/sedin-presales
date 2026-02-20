package com.sedin.presales.api.controller;

import com.sedin.presales.config.audit.Audited;
import com.sedin.presales.application.dto.ApiResponse;
import com.sedin.presales.application.dto.CreateFolderRequest;
import com.sedin.presales.application.dto.FolderDto;
import com.sedin.presales.application.service.FolderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/folders")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FolderDto>>> list(
            @RequestParam(required = false) UUID parentId) {
        log.debug("GET /api/v1/folders - parentId: {}", parentId);
        List<FolderDto> folders = folderService.list(parentId);
        return ResponseEntity.ok(ApiResponse.success(folders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FolderDto>> getById(@PathVariable UUID id) {
        log.debug("GET /api/v1/folders/{}", id);
        FolderDto folder = folderService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(folder));
    }

    @Audited(action = "CREATE_FOLDER", resourceType = "FOLDER")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<FolderDto>> create(@Valid @RequestBody CreateFolderRequest request) {
        log.debug("POST /api/v1/folders - name: {}", request.getName());
        FolderDto folder = folderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(folder, "Folder created successfully"));
    }

    @Audited(action = "UPDATE_FOLDER", resourceType = "FOLDER")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<FolderDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateFolderRequest request) {
        log.debug("PUT /api/v1/folders/{}", id);
        FolderDto folder = folderService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(folder, "Folder updated successfully"));
    }

    @Audited(action = "DELETE_FOLDER", resourceType = "FOLDER")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        log.debug("DELETE /api/v1/folders/{}", id);
        folderService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Folder deleted successfully"));
    }
}
