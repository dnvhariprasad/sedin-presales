package com.sedin.presales.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.CaseStudyAgent;
import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.entity.Rendition;
import com.sedin.presales.domain.enums.RenditionStatus;
import com.sedin.presales.domain.enums.RenditionType;
import com.sedin.presales.domain.repository.CaseStudyAgentRepository;
import com.sedin.presales.domain.repository.DocumentVersionRepository;
import com.sedin.presales.domain.repository.RenditionRepository;
import com.sedin.presales.infrastructure.rendition.PptTemplateBuilder;
import com.sedin.presales.infrastructure.storage.BlobStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseStudyFormattingServiceTest {

    @Mock
    private CaseStudyAgentRepository caseStudyAgentRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private RenditionRepository renditionRepository;

    @Mock
    private BlobStorageService blobStorageService;

    @Mock
    private PptTemplateBuilder pptTemplateBuilder;

    private CaseStudyFormattingService formattingService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID documentVersionId = UUID.randomUUID();
    private final UUID agentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        formattingService = new CaseStudyFormattingService(
                caseStudyAgentRepository,
                documentVersionRepository,
                renditionRepository,
                blobStorageService,
                pptTemplateBuilder,
                objectMapper
        );
    }

    private CaseStudyAgent buildActiveAgent() {
        CaseStudyAgent agent = CaseStudyAgent.builder()
                .name("Test Agent")
                .isActive(true)
                .templateConfig("{\"version\":\"1.0\",\"slideWidth\":13.33,\"slideHeight\":7.5,\"sections\":[{\"key\":\"title\",\"label\":\"Title\",\"required\":true,\"order\":1,\"type\":\"TEXT\",\"position\":{\"x\":0.5,\"y\":0.5,\"width\":12,\"height\":1}}]}")
                .build();
        agent.setId(agentId);
        return agent;
    }

    private DocumentVersion buildDocumentVersion() {
        Document document = Document.builder()
                .id(UUID.randomUUID())
                .build();
        return DocumentVersion.builder()
                .id(documentVersionId)
                .document(document)
                .versionNumber(1)
                .filePath("documents/test.pptx")
                .fileName("test.pptx")
                .build();
    }

    @Test
    void performFormatting_shouldCreateFormattedRendition() {
        DocumentVersion version = buildDocumentVersion();
        CaseStudyAgent agent = buildActiveAgent();
        String extractedContentJson = "{\"title\":\"Test Case Study\"}";
        byte[] pptBytes = new byte[]{1, 2, 3, 4, 5};

        when(documentVersionRepository.findById(documentVersionId)).thenReturn(Optional.of(version));
        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(agent));
        when(pptTemplateBuilder.buildPresentation(any(), any())).thenReturn(pptBytes);
        when(renditionRepository.save(any(Rendition.class))).thenAnswer(invocation -> {
            Rendition r = invocation.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            return r;
        });

        formattingService.performFormatting(documentVersionId, extractedContentJson);

        ArgumentCaptor<Rendition> captor = ArgumentCaptor.forClass(Rendition.class);
        verify(renditionRepository, times(2)).save(captor.capture());

        // Second save should be the completed one
        Rendition completedRendition = captor.getAllValues().get(1);
        assertThat(completedRendition.getStatus()).isEqualTo(RenditionStatus.COMPLETED);
        assertThat(completedRendition.getRenditionType()).isEqualTo(RenditionType.FORMATTED);
        assertThat(completedRendition.getFileSize()).isEqualTo(5L);
    }

    @Test
    void performFormatting_shouldHandleAsposeError() {
        DocumentVersion version = buildDocumentVersion();
        CaseStudyAgent agent = buildActiveAgent();
        String extractedContentJson = "{\"title\":\"Test Case Study\"}";

        when(documentVersionRepository.findById(documentVersionId)).thenReturn(Optional.of(version));
        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(agent));
        when(pptTemplateBuilder.buildPresentation(any(), any()))
                .thenThrow(new RuntimeException("Aspose license expired"));
        when(renditionRepository.save(any(Rendition.class))).thenAnswer(invocation -> {
            Rendition r = invocation.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            return r;
        });

        formattingService.performFormatting(documentVersionId, extractedContentJson);

        ArgumentCaptor<Rendition> captor = ArgumentCaptor.forClass(Rendition.class);
        verify(renditionRepository, times(2)).save(captor.capture());

        Rendition failedRendition = captor.getAllValues().get(1);
        assertThat(failedRendition.getStatus()).isEqualTo(RenditionStatus.FAILED);
        assertThat(failedRendition.getErrorMessage()).contains("Aspose license expired");
    }

    @Test
    void performFormatting_shouldUploadToCorrectContainer() {
        DocumentVersion version = buildDocumentVersion();
        CaseStudyAgent agent = buildActiveAgent();
        String extractedContentJson = "{\"title\":\"Test Case Study\"}";
        byte[] pptBytes = new byte[]{1, 2, 3};

        when(documentVersionRepository.findById(documentVersionId)).thenReturn(Optional.of(version));
        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(agent));
        when(pptTemplateBuilder.buildPresentation(any(), any())).thenReturn(pptBytes);
        when(renditionRepository.save(any(Rendition.class))).thenAnswer(invocation -> {
            Rendition r = invocation.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            return r;
        });

        formattingService.performFormatting(documentVersionId, extractedContentJson);

        String expectedPath = String.format("renditions/%s/formatted.pptx", documentVersionId);
        verify(blobStorageService).upload(
                eq("renditions"),
                eq(expectedPath),
                any(InputStream.class),
                eq((long) pptBytes.length),
                eq("application/vnd.openxmlformats-officedocument.presentationml.presentation")
        );
    }
}
