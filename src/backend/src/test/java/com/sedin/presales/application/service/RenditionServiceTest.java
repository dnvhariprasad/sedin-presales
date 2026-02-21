package com.sedin.presales.application.service;

import com.sedin.presales.application.dto.RenditionDto;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.entity.Rendition;
import com.sedin.presales.domain.enums.RenditionStatus;
import com.sedin.presales.domain.enums.RenditionType;
import com.sedin.presales.domain.repository.DocumentVersionRepository;
import com.sedin.presales.domain.repository.RenditionRepository;
import com.sedin.presales.infrastructure.rendition.PdfRenditionService;
import com.sedin.presales.infrastructure.storage.BlobStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenditionServiceTest {

    @Mock
    private RenditionRepository renditionRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private PdfRenditionService pdfRenditionService;

    @Mock
    private BlobStorageService blobStorageService;

    @InjectMocks
    private RenditionService renditionService;

    private final UUID versionId = UUID.randomUUID();

    private DocumentVersion buildDocumentVersion() {
        return DocumentVersion.builder()
                .id(versionId)
                .filePath("documents/docId/1/file.pptx")
                .contentType("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                .build();
    }

    private Rendition buildRendition(RenditionStatus status) {
        return Rendition.builder()
                .id(UUID.randomUUID())
                .renditionType(RenditionType.PDF)
                .status(status)
                .filePath("renditions/" + versionId + "/document.pdf")
                .fileSize(1024L)
                .documentVersion(buildDocumentVersion())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("processRendition should create and complete PDF rendition on happy path")
    void processRendition_shouldCreateAndCompletePdfRendition() throws Exception {
        DocumentVersion version = buildDocumentVersion();
        byte[] pdfBytes = "fake-pdf-content".getBytes();
        InputStream fileStream = new ByteArrayInputStream("original-content".getBytes());

        when(documentVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.PDF))
                .thenReturn(Optional.empty());
        when(renditionRepository.save(any(Rendition.class))).thenAnswer(invocation -> {
            Rendition r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(blobStorageService.download("documents", version.getFilePath())).thenReturn(fileStream);
        when(pdfRenditionService.convertToPdf(any(InputStream.class), eq(version.getContentType())))
                .thenReturn(pdfBytes);

        renditionService.processRendition(versionId);

        ArgumentCaptor<Rendition> captor = ArgumentCaptor.forClass(Rendition.class);
        // save is called 3 times: PENDING, PROCESSING, COMPLETED
        verify(renditionRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        Rendition finalRendition = captor.getAllValues().get(2);
        assertThat(finalRendition.getStatus()).isEqualTo(RenditionStatus.COMPLETED);
        assertThat(finalRendition.getFilePath()).contains("renditions/");
        assertThat(finalRendition.getFileSize()).isEqualTo((long) pdfBytes.length);

        verify(blobStorageService).upload(eq("renditions"), anyString(),
                any(InputStream.class), eq((long) pdfBytes.length), eq("application/pdf"));
    }

    @Test
    @DisplayName("processRendition should skip when rendition already completed")
    void processRendition_shouldSkipWhenAlreadyCompleted() {
        DocumentVersion version = buildDocumentVersion();
        Rendition completedRendition = buildRendition(RenditionStatus.COMPLETED);

        when(documentVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.PDF))
                .thenReturn(Optional.of(completedRendition));

        renditionService.processRendition(versionId);

        verify(renditionRepository, never()).save(any(Rendition.class));
        verify(blobStorageService, never()).download(anyString(), anyString());
    }

    @Test
    @DisplayName("processRendition should delete existing failed rendition and regenerate")
    void processRendition_shouldDeleteExistingFailedAndRegenerate() throws Exception {
        DocumentVersion version = buildDocumentVersion();
        Rendition failedRendition = buildRendition(RenditionStatus.FAILED);
        byte[] pdfBytes = "new-pdf-content".getBytes();

        when(documentVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.PDF))
                .thenReturn(Optional.of(failedRendition));
        when(renditionRepository.save(any(Rendition.class))).thenAnswer(invocation -> {
            Rendition r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(blobStorageService.download("documents", version.getFilePath()))
                .thenReturn(new ByteArrayInputStream("content".getBytes()));
        when(pdfRenditionService.convertToPdf(any(InputStream.class), eq(version.getContentType())))
                .thenReturn(pdfBytes);

        renditionService.processRendition(versionId);

        verify(renditionRepository).delete(failedRendition);
        verify(renditionRepository).flush();
        ArgumentCaptor<Rendition> captor = ArgumentCaptor.forClass(Rendition.class);
        verify(renditionRepository, org.mockito.Mockito.atLeast(3)).save(captor.capture());
        Rendition lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(RenditionStatus.COMPLETED);
    }

    @Test
    @DisplayName("processRendition should set FAILED status on conversion error")
    void processRendition_shouldSetFailedOnConversionError() throws Exception {
        DocumentVersion version = buildDocumentVersion();

        when(documentVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.PDF))
                .thenReturn(Optional.empty());
        when(renditionRepository.save(any(Rendition.class))).thenAnswer(invocation -> {
            Rendition r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(blobStorageService.download("documents", version.getFilePath()))
                .thenReturn(new ByteArrayInputStream("content".getBytes()));
        when(pdfRenditionService.convertToPdf(any(InputStream.class), eq(version.getContentType())))
                .thenThrow(new RuntimeException("Conversion failed"));

        renditionService.processRendition(versionId);

        ArgumentCaptor<Rendition> captor = ArgumentCaptor.forClass(Rendition.class);
        verify(renditionRepository, org.mockito.Mockito.atLeast(3)).save(captor.capture());
        Rendition lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(RenditionStatus.FAILED);
        assertThat(lastSaved.getErrorMessage()).isEqualTo("Conversion failed");
    }

    @Test
    @DisplayName("processRendition should throw when document version not found")
    void processRendition_shouldThrowWhenVersionNotFound() {
        UUID missingId = UUID.randomUUID();
        when(documentVersionRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> renditionService.processRendition(missingId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getRendition should return RenditionDto when found")
    void getRendition_shouldReturnDto() {
        Rendition rendition = buildRendition(RenditionStatus.COMPLETED);

        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.PDF))
                .thenReturn(Optional.of(rendition));

        RenditionDto result = renditionService.getRendition(versionId, RenditionType.PDF);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(rendition.getId());
        assertThat(result.getRenditionType()).isEqualTo(RenditionType.PDF);
        assertThat(result.getStatus()).isEqualTo(RenditionStatus.COMPLETED);
        assertThat(result.getFilePath()).isEqualTo(rendition.getFilePath());
        assertThat(result.getFileSize()).isEqualTo(rendition.getFileSize());
    }

    @Test
    @DisplayName("getRenditions should return list of RenditionDtos")
    void getRenditions_shouldReturnList() {
        Rendition rendition1 = buildRendition(RenditionStatus.COMPLETED);
        Rendition rendition2 = Rendition.builder()
                .id(UUID.randomUUID())
                .renditionType(RenditionType.SUMMARY)
                .status(RenditionStatus.COMPLETED)
                .filePath("summaries/" + versionId + "/summary.txt")
                .fileSize(512L)
                .documentVersion(buildDocumentVersion())
                .createdAt(Instant.now())
                .build();

        when(renditionRepository.findByDocumentVersionId(versionId))
                .thenReturn(List.of(rendition1, rendition2));

        List<RenditionDto> results = renditionService.getRenditions(versionId);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getRenditionType()).isEqualTo(RenditionType.PDF);
        assertThat(results.get(1).getRenditionType()).isEqualTo(RenditionType.SUMMARY);
    }
}
