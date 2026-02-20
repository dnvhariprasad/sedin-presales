package com.sedin.presales.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.CreateDocumentRequest;
import com.sedin.presales.application.dto.DocumentDetailDto;
import com.sedin.presales.application.dto.DocumentDto;
import com.sedin.presales.application.dto.DocumentVersionDto;
import com.sedin.presales.application.dto.IndexToggleResponseDto;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateDocumentRequest;
import com.sedin.presales.application.exception.GlobalExceptionHandler;
import com.sedin.presales.application.service.DocumentService;
import com.sedin.presales.config.TestSecurityConfig;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.enums.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = DocumentController.class,
        excludeAutoConfiguration = {OAuth2ResourceServerAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentService documentService;

    private final UUID testId = UUID.randomUUID();
    private final UUID documentTypeId = UUID.randomUUID();
    private final UUID folderId = UUID.randomUUID();

    private DocumentDto buildDocumentDto() {
        return DocumentDto.builder()
                .id(testId)
                .title("Test Document")
                .customerName("Test Customer")
                .documentDate(LocalDate.of(2025, 1, 15))
                .status(DocumentStatus.ACTIVE)
                .ragIndexed(false)
                .currentVersionNumber(1)
                .folderId(folderId)
                .folderName("Test Folder")
                .documentTypeId(documentTypeId)
                .documentTypeName("Proposal")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy("system")
                .build();
    }

    private DocumentDetailDto buildDocumentDetailDto() {
        return DocumentDetailDto.builder()
                .id(testId)
                .title("Test Document")
                .customerName("Test Customer")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(1)
                .createdAt(Instant.now())
                .build();
    }

    private DocumentVersionDto buildVersionDto() {
        return DocumentVersionDto.builder()
                .id(UUID.randomUUID())
                .versionNumber(1)
                .fileName("test.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void list_shouldReturnPagedDocuments() throws Exception {
        DocumentDto dto = buildDocumentDto();
        PagedResponse<DocumentDto> pagedResponse = PagedResponse.<DocumentDto>builder()
                .content(List.of(dto))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(documentService.list(any(), any(), any(), any(), any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/documents")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].title").value("Test Document"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getById_shouldReturnDocumentDetail() throws Exception {
        DocumentDetailDto dto = buildDocumentDetailDto();
        when(documentService.getById(testId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/documents/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testId.toString()))
                .andExpect(jsonPath("$.data.title").value("Test Document"));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        UpdateDocumentRequest request = UpdateDocumentRequest.builder()
                .title("Updated Title")
                .customerName("Updated Customer")
                .build();
        DocumentDto dto = buildDocumentDto();

        when(documentService.update(eq(testId), any(UpdateDocumentRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/documents/{id}", testId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document updated successfully"));
    }

    @Test
    void delete_shouldReturn200() throws Exception {
        doNothing().when(documentService).delete(testId);

        mockMvc.perform(delete("/api/v1/documents/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document archived successfully"));
    }

    @Test
    void getVersions_shouldReturnVersionList() throws Exception {
        DocumentVersionDto versionDto = buildVersionDto();
        when(documentService.getVersions(testId)).thenReturn(List.of(versionDto));

        mockMvc.perform(get("/api/v1/documents/{id}/versions", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].versionNumber").value(1))
                .andExpect(jsonPath("$.data[0].fileName").value("test.pdf"));
    }

    @Test
    void upload_shouldReturn201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "PDF content".getBytes());

        CreateDocumentRequest request = CreateDocumentRequest.builder()
                .title("Test Document")
                .documentTypeId(documentTypeId)
                .folderId(folderId)
                .build();

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));

        DocumentDto dto = buildDocumentDto();
        when(documentService.upload(any(), any(CreateDocumentRequest.class))).thenReturn(dto);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .file(requestPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Document"))
                .andExpect(jsonPath("$.message").value("Document uploaded successfully"));
    }

    @Test
    void uploadNewVersion_shouldReturn201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test-v2.pdf", "application/pdf", "PDF v2 content".getBytes());

        DocumentVersionDto versionDto = DocumentVersionDto.builder()
                .id(UUID.randomUUID())
                .versionNumber(2)
                .fileName("test-v2.pdf")
                .fileSize(2048L)
                .contentType("application/pdf")
                .createdAt(Instant.now())
                .build();

        when(documentService.uploadNewVersion(eq(testId), any(), any())).thenReturn(versionDto);

        mockMvc.perform(multipart("/api/v1/documents/{id}/versions", testId)
                        .file(file)
                        .param("changeNotes", "Updated version"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.versionNumber").value(2))
                .andExpect(jsonPath("$.message").value("New version uploaded successfully"));
    }

    @Test
    void downloadVersion_shouldReturnFile() throws Exception {
        byte[] fileContent = "file content bytes".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);

        DocumentVersion docVersion = DocumentVersion.builder()
                .id(UUID.randomUUID())
                .versionNumber(1)
                .fileName("test.pdf")
                .contentType("application/pdf")
                .build();

        when(documentService.getDocumentVersion(testId, 1)).thenReturn(docVersion);
        when(documentService.downloadVersion(testId, 1)).thenReturn(inputStream);

        mockMvc.perform(get("/api/v1/documents/{id}/versions/1/download", testId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void toggleRagIndex_shouldReturn200() throws Exception {
        IndexToggleResponseDto responseDto = IndexToggleResponseDto.builder()
                .documentId(testId)
                .ragIndexed(true)
                .message("Document queued for indexing")
                .build();

        when(documentService.toggleRagIndex(testId)).thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/documents/{id}/index-toggle", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId").value(testId.toString()))
                .andExpect(jsonPath("$.data.ragIndexed").value(true))
                .andExpect(jsonPath("$.message").value("Document queued for indexing"));
    }
}
