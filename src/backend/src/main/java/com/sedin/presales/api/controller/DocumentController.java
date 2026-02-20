package com.sedin.presales.api.controller;

import com.sedin.presales.application.dto.ApiResponse;
import com.sedin.presales.application.dto.CompareViewDto;
import com.sedin.presales.application.dto.CreateDocumentRequest;
import com.sedin.presales.application.dto.DocumentDetailDto;
import com.sedin.presales.application.dto.DocumentDownloadDto;
import com.sedin.presales.application.dto.DocumentDto;
import com.sedin.presales.application.dto.DocumentVersionDto;
import com.sedin.presales.application.dto.DocumentViewDto;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateDocumentRequest;
import com.sedin.presales.application.service.DocumentService;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.enums.DocumentStatus;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentDto>> upload(
            @RequestParam("file") MultipartFile file,
            @Valid @RequestPart("request") CreateDocumentRequest request) {
        log.debug("POST /api/v1/documents - title: {}", request.getTitle());
        DocumentDto document = documentService.upload(file, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(document, "Document uploaded successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<DocumentDto>>> list(
            @RequestParam(required = false) UUID folderId,
            @RequestParam(required = false) UUID documentTypeId,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/v1/documents - folderId: {}, documentTypeId: {}, status: {}, search: {}, page: {}, size: {}",
                folderId, documentTypeId, status, search, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<DocumentDto> documents = documentService.list(pageable, folderId, documentTypeId, status, search);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentDetailDto>> getById(@PathVariable UUID id) {
        log.debug("GET /api/v1/documents/{}", id);
        DocumentDetailDto document = documentService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(document));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDocumentRequest request) {
        log.debug("PUT /api/v1/documents/{}", id);
        DocumentDto document = documentService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(document, "Document updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        log.debug("DELETE /api/v1/documents/{}", id);
        documentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Document archived successfully"));
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentVersionDto>> uploadNewVersion(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "changeNotes", required = false) String changeNotes) {
        log.debug("POST /api/v1/documents/{}/versions", id);
        DocumentVersionDto version = documentService.uploadNewVersion(id, file, changeNotes);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(version, "New version uploaded successfully"));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<DocumentVersionDto>>> getVersions(@PathVariable UUID id) {
        log.debug("GET /api/v1/documents/{}/versions", id);
        List<DocumentVersionDto> versions = documentService.getVersions(id);
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    @GetMapping("/{id}/versions/{versionNumber}/download")
    public ResponseEntity<InputStreamResource> downloadVersion(
            @PathVariable UUID id,
            @PathVariable Integer versionNumber) {
        log.debug("GET /api/v1/documents/{}/versions/{}/download", id, versionNumber);

        DocumentVersion docVersion = documentService.getDocumentVersion(id, versionNumber);
        InputStream inputStream = documentService.downloadVersion(id, versionNumber);

        String contentType = docVersion.getContentType() != null
                ? docVersion.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + docVersion.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(inputStream));
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<ApiResponse<DocumentViewDto>> getViewUrl(@PathVariable UUID id) {
        log.debug("GET /api/v1/documents/{}/view", id);
        DocumentViewDto viewDto = documentService.getViewUrl(id);
        return ResponseEntity.ok(ApiResponse.success(viewDto));
    }

    @GetMapping("/{id}/versions/{v1}/compare/{v2}")
    public ResponseEntity<ApiResponse<CompareViewDto>> getCompareUrls(
            @PathVariable UUID id,
            @PathVariable int v1,
            @PathVariable int v2) {
        log.debug("GET /api/v1/documents/{}/versions/{}/compare/{}", id, v1, v2);
        CompareViewDto compareDto = documentService.getCompareUrls(id, v1, v2);
        return ResponseEntity.ok(ApiResponse.success(compareDto));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<DocumentDownloadDto>> getDownloadUrl(@PathVariable UUID id) {
        log.debug("GET /api/v1/documents/{}/download", id);
        DocumentDownloadDto downloadDto = documentService.getDownloadUrl(id);
        return ResponseEntity.ok(ApiResponse.success(downloadDto));
    }
}
