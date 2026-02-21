package com.sedin.presales.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.ValidationResultDto;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.CaseStudyAgent;
import com.sedin.presales.domain.entity.CaseStudyValidationResult;
import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.repository.CaseStudyAgentRepository;
import com.sedin.presales.domain.repository.CaseStudyValidationResultRepository;
import com.sedin.presales.domain.repository.DocumentVersionRepository;
import com.sedin.presales.infrastructure.ai.casestudy.CaseStudyContentExtractor;
import com.sedin.presales.infrastructure.ai.casestudy.CaseStudyContentValidator;
import com.sedin.presales.infrastructure.rendition.PptTextExtractor;
import com.sedin.presales.infrastructure.storage.BlobStorageService;
import com.sedin.presales.config.CurrentUserService;
import com.sedin.presales.config.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseStudyValidationServiceTest {

    @Mock
    private CaseStudyAgentRepository caseStudyAgentRepository;

    @Mock
    private CaseStudyValidationResultRepository validationResultRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private BlobStorageService blobStorageService;

    @Mock
    private PptTextExtractor pptTextExtractor;

    @Mock
    private CaseStudyContentExtractor contentExtractor;

    @Mock
    private CaseStudyContentValidator contentValidator;

    @Mock
    private CaseStudyFormattingService formattingService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AclService aclService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private CaseStudyValidationService validationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID documentVersionId = UUID.randomUUID();
    private final UUID agentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        validationService = new CaseStudyValidationService(
                caseStudyAgentRepository,
                validationResultRepository,
                documentVersionRepository,
                blobStorageService,
                pptTextExtractor,
                contentExtractor,
                contentValidator,
                formattingService,
                objectMapper,
                currentUserService,
                aclService,
                transactionManager
        );
    }

    private CaseStudyAgent buildActiveAgent() {
        CaseStudyAgent agent = CaseStudyAgent.builder()
                .name("Test Agent")
                .isActive(true)
                .templateConfig("{\"version\":\"1.0\",\"slideWidth\":13.33,\"slideHeight\":7.5,\"sections\":[{\"key\":\"title\",\"label\":\"Title\",\"required\":true,\"order\":1,\"type\":\"TEXT\",\"position\":{\"x\":0.5,\"y\":0.5,\"width\":12,\"height\":1}}]}")
                .build();
        // Set the id via reflection-safe approach since BaseEntity uses @SuperBuilder
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
    void validateCaseStudy_shouldSkipWhenNoActiveAgent() {
        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.empty());

        validationService.performValidation(documentVersionId);

        verify(validationResultRepository, never()).save(any());
    }

    @Test
    void performValidation_shouldSaveValidResult() {
        CaseStudyAgent agent = buildActiveAgent();
        DocumentVersion version = buildDocumentVersion();
        InputStream mockStream = new ByteArrayInputStream(new byte[0]);

        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(agent));
        when(documentVersionRepository.findById(documentVersionId)).thenReturn(Optional.of(version));
        when(blobStorageService.download("documents", version.getFilePath())).thenReturn(mockStream);
        when(pptTextExtractor.extractText(mockStream)).thenReturn("Sample PPT text content");
        when(contentExtractor.extractSections(anyString(), anyString())).thenReturn("{\"title\":\"Test Case Study\"}");
        when(contentValidator.validateContent(anyString(), anyString()))
                .thenReturn("{\"overallScore\":0.9,\"issues\":[]}");
        when(validationResultRepository.save(any(CaseStudyValidationResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        validationService.performValidation(documentVersionId);

        ArgumentCaptor<CaseStudyValidationResult> captor = ArgumentCaptor.forClass(CaseStudyValidationResult.class);
        verify(validationResultRepository).save(captor.capture());
        CaseStudyValidationResult saved = captor.getValue();

        assertThat(saved.getIsValid()).isTrue();
        assertThat(saved.getDocumentVersion()).isEqualTo(version);
        assertThat(saved.getAgent()).isEqualTo(agent);
        verify(formattingService, never()).formatCaseStudy(any(), anyString());
    }

    @Test
    void performValidation_shouldSaveInvalidAndTriggerFormatting() {
        CaseStudyAgent agent = buildActiveAgent();
        DocumentVersion version = buildDocumentVersion();
        InputStream mockStream = new ByteArrayInputStream(new byte[0]);

        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(agent));
        when(documentVersionRepository.findById(documentVersionId)).thenReturn(Optional.of(version));
        when(blobStorageService.download("documents", version.getFilePath())).thenReturn(mockStream);
        when(pptTextExtractor.extractText(mockStream)).thenReturn("Sample PPT text content");
        when(contentExtractor.extractSections(anyString(), anyString())).thenReturn("{\"title\":\"Incomplete\"}");
        when(contentValidator.validateContent(anyString(), anyString()))
                .thenReturn("{\"overallScore\":0.5,\"issues\":[{\"section\":\"title\",\"severity\":\"ERROR\",\"message\":\"Too short\"}]}");
        when(validationResultRepository.save(any(CaseStudyValidationResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        validationService.performValidation(documentVersionId);

        ArgumentCaptor<CaseStudyValidationResult> captor = ArgumentCaptor.forClass(CaseStudyValidationResult.class);
        verify(validationResultRepository).save(captor.capture());
        CaseStudyValidationResult saved = captor.getValue();

        assertThat(saved.getIsValid()).isFalse();
        verify(formattingService).formatCaseStudy(eq(documentVersionId), anyString());
    }

    @Test
    void performValidation_shouldHandleExtractionFailure() {
        CaseStudyAgent agent = buildActiveAgent();
        DocumentVersion version = buildDocumentVersion();
        InputStream mockStream = new ByteArrayInputStream(new byte[0]);

        when(caseStudyAgentRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(agent));
        when(documentVersionRepository.findById(documentVersionId)).thenReturn(Optional.of(version));
        when(blobStorageService.download("documents", version.getFilePath())).thenReturn(mockStream);
        when(pptTextExtractor.extractText(mockStream)).thenThrow(new RuntimeException("PPT extraction failed"));

        assertThatThrownBy(() -> validationService.performValidation(documentVersionId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("PPT extraction failed");

        verify(validationResultRepository, never()).save(any());
    }

    @Test
    void getValidationResult_shouldReturnDto() {
        // Mock admin user to bypass ACL checks
        UserPrincipal admin = UserPrincipal.builder()
                .userId(UUID.randomUUID().toString())
                .email("admin@test.com")
                .role("ADMIN")
                .build();
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        DocumentVersion version = buildDocumentVersion();
        CaseStudyAgent agent = buildActiveAgent();

        CaseStudyValidationResult result = CaseStudyValidationResult.builder()
                .id(UUID.randomUUID())
                .documentVersion(version)
                .agent(agent)
                .isValid(true)
                .validationDetails("{\"overallScore\":0.9,\"issues\":[]}")
                .createdAt(Instant.now())
                .build();

        when(validationResultRepository.findTopByDocumentVersionIdOrderByCreatedAtDesc(documentVersionId))
                .thenReturn(Optional.of(result));

        ValidationResultDto dto = validationService.getValidationResult(documentVersionId);

        assertThat(dto.getId()).isEqualTo(result.getId());
        assertThat(dto.getDocumentVersionId()).isEqualTo(documentVersionId);
        assertThat(dto.getAgentId()).isEqualTo(agentId);
        assertThat(dto.isValid()).isTrue();
        assertThat(dto.getValidationDetails()).isNotNull();
        assertThat(dto.getCreatedAt()).isNotNull();
    }

    @Test
    void getValidationResult_shouldThrowWhenNotFound() {
        when(validationResultRepository.findTopByDocumentVersionIdOrderByCreatedAtDesc(documentVersionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> validationService.getValidationResult(documentVersionId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
