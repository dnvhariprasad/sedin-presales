package com.sedin.presales.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.CaseStudyWizardRequest;
import com.sedin.presales.application.dto.CaseStudyWizardResponseDto;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.CaseStudyAgent;
import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.entity.DocumentType;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.repository.CaseStudyAgentRepository;
import com.sedin.presales.domain.repository.DocumentRepository;
import com.sedin.presales.domain.repository.DocumentTypeRepository;
import com.sedin.presales.domain.repository.DocumentVersionRepository;
import com.sedin.presales.infrastructure.ai.casestudy.CaseStudyContentEnhancer;
import com.sedin.presales.infrastructure.rendition.PptTemplateBuilder;
import com.sedin.presales.infrastructure.storage.BlobStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseStudyGenerationServiceTest {

    @Mock
    private CaseStudyAgentRepository caseStudyAgentRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private BlobStorageService blobStorageService;

    @Mock
    private PptTemplateBuilder pptTemplateBuilder;

    @Mock
    private CaseStudyContentEnhancer contentEnhancer;

    @Mock
    private RenditionService renditionService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CaseStudyGenerationService caseStudyGenerationService;

    private static final String TEMPLATE_CONFIG_JSON = """
            {
                "slideWidth": 13.33,
                "slideHeight": 7.5,
                "sections": [
                    {"key": "title", "label": "Title", "type": "TEXT", "order": 1,
                     "position": {"x": 0.5, "y": 0.5, "width": 12, "height": 1}},
                    {"key": "customerOverview", "label": "Customer Overview", "type": "TEXT", "order": 2,
                     "position": {"x": 0.5, "y": 2.0, "width": 12, "height": 1.5}}
                ],
                "branding": {
                    "primaryColor": "#003366",
                    "accentColor": "#FF6600",
                    "fontFamily": "Arial",
                    "headingFontFamily": "Arial",
                    "headingFontSize": 24,
                    "bodyFontSize": 14,
                    "bulletFontSize": 12
                },
                "footerText": "Confidential"
            }
            """;

    private CaseStudyWizardRequest buildRequest(boolean enhanceWithAi) {
        return CaseStudyWizardRequest.builder()
                .title("Test Case Study")
                .customerName("Acme Corp")
                .customerOverview("Acme Corp is a global leader")
                .challenges(List.of("Legacy system", "Scale issues"))
                .solution("Cloud migration with microservices")
                .technologies(List.of("Java", "Azure"))
                .results(List.of("50% cost reduction", "99.9% uptime"))
                .enhanceWithAi(enhanceWithAi)
                .build();
    }

    private CaseStudyAgent buildAgent() {
        CaseStudyAgent agent = new CaseStudyAgent();
        agent.setId(UUID.randomUUID());
        agent.setName("Test Agent");
        agent.setTemplateConfig(TEMPLATE_CONFIG_JSON);
        agent.setIsActive(true);
        return agent;
    }

    private DocumentType buildDocumentType() {
        DocumentType dt = new DocumentType();
        dt.setId(UUID.randomUUID());
        dt.setName("Case Study");
        return dt;
    }

    @Test
    void generateCaseStudy_shouldCreateDocumentAndVersion() {
        CaseStudyWizardRequest request = buildRequest(false);
        CaseStudyAgent agent = buildAgent();
        DocumentType docType = buildDocumentType();
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        Document savedDoc = Document.builder()
                .title(request.getTitle())
                .currentVersionNumber(1)
                .build();
        savedDoc.setId(docId);

        DocumentVersion savedVersion = DocumentVersion.builder()
                .versionNumber(1)
                .document(savedDoc)
                .build();
        savedVersion.setId(versionId);

        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(agent));
        when(pptTemplateBuilder.buildPresentation(any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(documentTypeRepository.findByName("Case Study")).thenReturn(Optional.of(docType));
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        when(blobStorageService.upload(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("blob-url");
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);

        CaseStudyWizardResponseDto response = caseStudyGenerationService.generateCaseStudy(request);

        assertThat(response).isNotNull();
        assertThat(response.getDocumentId()).isEqualTo(docId);
        assertThat(response.getDocumentVersionId()).isEqualTo(versionId);
        assertThat(response.getMessage()).isEqualTo("Case study generated successfully");

        verify(documentRepository).save(any(Document.class));
        verify(documentVersionRepository).save(any(DocumentVersion.class));
        verify(blobStorageService).upload(eq("documents"), anyString(), any(InputStream.class), anyLong(), anyString());
    }

    @Test
    void generateCaseStudy_shouldEnhanceWithAiWhenFlagTrue() {
        CaseStudyWizardRequest request = buildRequest(true);
        CaseStudyAgent agent = buildAgent();
        DocumentType docType = buildDocumentType();
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        Document savedDoc = Document.builder()
                .title(request.getTitle())
                .currentVersionNumber(1)
                .build();
        savedDoc.setId(docId);

        DocumentVersion savedVersion = DocumentVersion.builder()
                .versionNumber(1)
                .document(savedDoc)
                .build();
        savedVersion.setId(versionId);

        String enhancedJson = """
                {"title":"Enhanced Title","customerOverview":"Enhanced overview",
                 "challenges":["Enhanced challenge"],"solution":"Enhanced solution",
                 "results":["Enhanced result"]}
                """;

        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(agent));
        when(contentEnhancer.enhanceContent(anyString())).thenReturn(enhancedJson);
        when(pptTemplateBuilder.buildPresentation(any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(documentTypeRepository.findByName("Case Study")).thenReturn(Optional.of(docType));
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        when(blobStorageService.upload(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("blob-url");
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);

        caseStudyGenerationService.generateCaseStudy(request);

        verify(contentEnhancer).enhanceContent(anyString());
    }

    @Test
    void generateCaseStudy_shouldSkipAiWhenFlagFalse() {
        CaseStudyWizardRequest request = buildRequest(false);
        CaseStudyAgent agent = buildAgent();
        DocumentType docType = buildDocumentType();
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        Document savedDoc = Document.builder()
                .title(request.getTitle())
                .currentVersionNumber(1)
                .build();
        savedDoc.setId(docId);

        DocumentVersion savedVersion = DocumentVersion.builder()
                .versionNumber(1)
                .document(savedDoc)
                .build();
        savedVersion.setId(versionId);

        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(agent));
        when(pptTemplateBuilder.buildPresentation(any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(documentTypeRepository.findByName("Case Study")).thenReturn(Optional.of(docType));
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        when(blobStorageService.upload(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("blob-url");
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);

        caseStudyGenerationService.generateCaseStudy(request);

        verify(contentEnhancer, never()).enhanceContent(anyString());
    }

    @Test
    void generateCaseStudy_shouldThrowWhenNoActiveAgent() {
        CaseStudyWizardRequest request = buildRequest(false);

        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caseStudyGenerationService.generateCaseStudy(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generateCaseStudy_shouldTriggerPdfRendition() {
        CaseStudyWizardRequest request = buildRequest(false);
        CaseStudyAgent agent = buildAgent();
        DocumentType docType = buildDocumentType();
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        Document savedDoc = Document.builder()
                .title(request.getTitle())
                .currentVersionNumber(1)
                .build();
        savedDoc.setId(docId);

        DocumentVersion savedVersion = DocumentVersion.builder()
                .versionNumber(1)
                .document(savedDoc)
                .build();
        savedVersion.setId(versionId);

        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(agent));
        when(pptTemplateBuilder.buildPresentation(any(), any())).thenReturn(new byte[]{1, 2, 3});
        when(documentTypeRepository.findByName("Case Study")).thenReturn(Optional.of(docType));
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        when(blobStorageService.upload(anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("blob-url");
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);

        caseStudyGenerationService.generateCaseStudy(request);

        verify(renditionService).generatePdfRendition(versionId);
    }
}
