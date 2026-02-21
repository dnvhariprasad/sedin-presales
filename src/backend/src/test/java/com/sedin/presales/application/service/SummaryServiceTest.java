package com.sedin.presales.application.service;

import com.sedin.presales.application.dto.SummaryResponseDto;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.entity.Rendition;
import com.sedin.presales.domain.enums.RenditionStatus;
import com.sedin.presales.domain.enums.RenditionType;
import com.sedin.presales.domain.repository.DocumentRepository;
import com.sedin.presales.domain.repository.DocumentVersionRepository;
import com.sedin.presales.domain.repository.RenditionRepository;
import com.sedin.presales.infrastructure.ai.DocumentIntelligenceService;
import com.sedin.presales.infrastructure.ai.SummarizationService;
import com.sedin.presales.infrastructure.storage.BlobStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private RenditionRepository renditionRepository;

    @Mock
    private DocumentIntelligenceService documentIntelligenceService;

    @Mock
    private SummarizationService summarizationService;

    @Mock
    private BlobStorageService blobStorageService;

    @InjectMocks
    private SummaryService summaryService;

    private final UUID documentId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(summaryService, "containerSummaries", "summaries");
    }

    private Document buildDocument() {
        Document doc = Document.builder()
                .title("Test Case Study")
                .currentVersionNumber(1)
                .build();
        doc.setId(documentId);
        return doc;
    }

    private DocumentVersion buildDocumentVersion() {
        Document doc = buildDocument();
        return DocumentVersion.builder()
                .id(versionId)
                .document(doc)
                .filePath("documents/docId/1/file.pptx")
                .contentType("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                .versionNumber(1)
                .build();
    }

    private Rendition buildSummaryRendition(RenditionStatus status) {
        return Rendition.builder()
                .id(UUID.randomUUID())
                .renditionType(RenditionType.SUMMARY)
                .status(status)
                .filePath("summaries/" + versionId + "/summary.txt")
                .fileSize(256L)
                .documentVersion(buildDocumentVersion())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("processSummary should extract text, summarize, upload, and complete")
    void processSummary_shouldExtractTextAndGenerateSummary() throws Exception {
        DocumentVersion version = buildDocumentVersion();
        String extractedText = "This is the extracted document content.";
        String summaryText = "This is a summary of the document.";
        byte[] summaryBytes = summaryText.getBytes(StandardCharsets.UTF_8);

        when(documentVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.SUMMARY))
                .thenReturn(Optional.empty());
        when(renditionRepository.save(any(Rendition.class))).thenAnswer(invocation -> {
            Rendition r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(blobStorageService.download("documents", version.getFilePath()))
                .thenReturn(new ByteArrayInputStream("original-content".getBytes()));
        when(documentIntelligenceService.extractText(any(InputStream.class), eq(version.getContentType())))
                .thenReturn(extractedText);
        when(summarizationService.summarize(extractedText, "Test Case Study"))
                .thenReturn(summaryText);

        summaryService.processSummary(versionId);

        ArgumentCaptor<Rendition> captor = ArgumentCaptor.forClass(Rendition.class);
        verify(renditionRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        Rendition finalRendition = captor.getAllValues().get(2);
        assertThat(finalRendition.getStatus()).isEqualTo(RenditionStatus.COMPLETED);
        assertThat(finalRendition.getFilePath()).contains("summaries/");
        assertThat(finalRendition.getFileSize()).isEqualTo((long) summaryBytes.length);

        verify(blobStorageService).upload(eq("summaries"), anyString(),
                any(InputStream.class), eq((long) summaryBytes.length), eq("text/plain"));
    }

    @Test
    @DisplayName("processSummary should skip when summary already completed")
    void processSummary_shouldSkipWhenAlreadyCompleted() {
        DocumentVersion version = buildDocumentVersion();
        Rendition completedRendition = buildSummaryRendition(RenditionStatus.COMPLETED);

        when(documentVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.SUMMARY))
                .thenReturn(Optional.of(completedRendition));

        summaryService.processSummary(versionId);

        verify(renditionRepository, never()).save(any(Rendition.class));
        verify(blobStorageService, never()).download(anyString(), anyString());
    }

    @Test
    @DisplayName("processSummary should set FAILED status on extraction error")
    void processSummary_shouldSetFailedOnExtractionError() throws Exception {
        DocumentVersion version = buildDocumentVersion();

        when(documentVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.SUMMARY))
                .thenReturn(Optional.empty());
        when(renditionRepository.save(any(Rendition.class))).thenAnswer(invocation -> {
            Rendition r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(blobStorageService.download("documents", version.getFilePath()))
                .thenReturn(new ByteArrayInputStream("content".getBytes()));
        when(documentIntelligenceService.extractText(any(InputStream.class), eq(version.getContentType())))
                .thenThrow(new RuntimeException("Extraction service unavailable"));

        summaryService.processSummary(versionId);

        ArgumentCaptor<Rendition> captor = ArgumentCaptor.forClass(Rendition.class);
        verify(renditionRepository, org.mockito.Mockito.atLeast(3)).save(captor.capture());
        Rendition lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(RenditionStatus.FAILED);
        assertThat(lastSaved.getErrorMessage()).isEqualTo("Extraction service unavailable");
    }

    @Test
    @DisplayName("processSummary should set FAILED when no text extracted")
    void processSummary_shouldSetFailedWhenNoTextExtracted() throws Exception {
        DocumentVersion version = buildDocumentVersion();

        when(documentVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.SUMMARY))
                .thenReturn(Optional.empty());
        when(renditionRepository.save(any(Rendition.class))).thenAnswer(invocation -> {
            Rendition r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(blobStorageService.download("documents", version.getFilePath()))
                .thenReturn(new ByteArrayInputStream("content".getBytes()));
        when(documentIntelligenceService.extractText(any(InputStream.class), eq(version.getContentType())))
                .thenReturn("");

        summaryService.processSummary(versionId);

        ArgumentCaptor<Rendition> captor = ArgumentCaptor.forClass(Rendition.class);
        verify(renditionRepository, org.mockito.Mockito.atLeast(3)).save(captor.capture());
        Rendition lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(RenditionStatus.FAILED);
        assertThat(lastSaved.getErrorMessage()).isEqualTo("No text could be extracted from the document");
    }

    @Test
    @DisplayName("getSummaryStatus should return COMPLETED with summary text")
    void getSummaryStatus_shouldReturnCompletedWithText() {
        Document document = buildDocument();
        DocumentVersion version = buildDocumentVersion();
        Rendition completedRendition = buildSummaryRendition(RenditionStatus.COMPLETED);
        String summaryText = "This is the summary text.";

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(documentId, 1))
                .thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.SUMMARY))
                .thenReturn(Optional.of(completedRendition));
        when(blobStorageService.download("summaries", completedRendition.getFilePath()))
                .thenReturn(new ByteArrayInputStream(summaryText.getBytes(StandardCharsets.UTF_8)));

        SummaryResponseDto result = summaryService.getSummaryStatus(documentId);

        assertThat(result.getDocumentId()).isEqualTo(documentId);
        assertThat(result.getStatus()).isEqualTo(RenditionStatus.COMPLETED);
        assertThat(result.getSummary()).isEqualTo(summaryText);
        assertThat(result.getMessage()).isEqualTo("Summary is available");
    }

    @Test
    @DisplayName("getSummaryStatus should return PROCESSING when summary is being generated")
    void getSummaryStatus_shouldReturnPendingWhenProcessing() {
        Document document = buildDocument();
        DocumentVersion version = buildDocumentVersion();
        Rendition processingRendition = buildSummaryRendition(RenditionStatus.PROCESSING);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(documentId, 1))
                .thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.SUMMARY))
                .thenReturn(Optional.of(processingRendition));

        SummaryResponseDto result = summaryService.getSummaryStatus(documentId);

        assertThat(result.getDocumentId()).isEqualTo(documentId);
        assertThat(result.getStatus()).isEqualTo(RenditionStatus.PROCESSING);
        assertThat(result.getMessage()).isEqualTo("Summary is being generated");
    }

    @Test
    @DisplayName("getSummaryStatus should trigger generation when no summary exists and return PENDING")
    void getSummaryStatus_shouldTriggerGenerationWhenNoSummaryExists() {
        Document document = buildDocument();
        DocumentVersion version = buildDocumentVersion();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(documentId, 1))
                .thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.SUMMARY))
                .thenReturn(Optional.empty());
        // generateSummary will call processSummary synchronously in unit test context.
        // processSummary will call documentVersionRepository.findById which we need to mock.
        // The generateSummary method catches all exceptions, so even if processSummary fails, it won't propagate.
        when(documentVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
        // processSummary will find no existing rendition (already mocked above as empty),
        // then try to save/download/etc. Let the save work and download fail gracefully.
        when(renditionRepository.save(any(Rendition.class))).thenAnswer(invocation -> {
            Rendition r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(blobStorageService.download(eq("documents"), anyString()))
                .thenThrow(new RuntimeException("Not set up for this test"));

        SummaryResponseDto result = summaryService.getSummaryStatus(documentId);

        assertThat(result.getDocumentId()).isEqualTo(documentId);
        assertThat(result.getStatus()).isEqualTo(RenditionStatus.PENDING);
        assertThat(result.getMessage()).isEqualTo("Summary generation has been initiated");
    }

    @Test
    @DisplayName("regenerateSummary should delete existing rendition and return PENDING")
    void regenerateSummary_shouldDeleteExistingAndTrigger() {
        Document document = buildDocument();
        DocumentVersion version = buildDocumentVersion();
        Rendition existingRendition = buildSummaryRendition(RenditionStatus.COMPLETED);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(documentId, 1))
                .thenReturn(Optional.of(version));
        when(renditionRepository.findByDocumentVersionIdAndRenditionType(versionId, RenditionType.SUMMARY))
                .thenReturn(Optional.of(existingRendition));
        // generateSummary will be called synchronously; processSummary will call
        // findByDocumentVersionIdAndRenditionType again and see COMPLETED (same mock),
        // so it will skip processing. Only need findById for version lookup.
        when(documentVersionRepository.findById(versionId)).thenReturn(Optional.of(version));

        SummaryResponseDto result = summaryService.regenerateSummary(documentId);

        verify(renditionRepository).delete(existingRendition);
        assertThat(result.getDocumentId()).isEqualTo(documentId);
        assertThat(result.getStatus()).isEqualTo(RenditionStatus.PENDING);
        assertThat(result.getMessage()).isEqualTo("Summary regeneration has been initiated");
    }
}
