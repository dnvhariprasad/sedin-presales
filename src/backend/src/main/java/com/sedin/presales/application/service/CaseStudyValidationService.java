package com.sedin.presales.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.ValidationResultDto;
import com.sedin.presales.application.dto.templateconfig.TemplateConfig;
import com.sedin.presales.application.dto.templateconfig.SectionConfig;
import com.sedin.presales.application.exception.AccessDeniedException;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.config.CurrentUserService;
import com.sedin.presales.config.UserPrincipal;
import com.sedin.presales.domain.entity.CaseStudyAgent;
import com.sedin.presales.domain.entity.CaseStudyValidationResult;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.enums.Permission;
import com.sedin.presales.domain.enums.ResourceType;
import com.sedin.presales.domain.repository.CaseStudyAgentRepository;
import com.sedin.presales.domain.repository.CaseStudyValidationResultRepository;
import com.sedin.presales.domain.repository.DocumentVersionRepository;
import com.sedin.presales.infrastructure.ai.casestudy.CaseStudyContentExtractor;
import com.sedin.presales.infrastructure.ai.casestudy.CaseStudyContentValidator;
import com.sedin.presales.infrastructure.rendition.PptTextExtractor;
import com.sedin.presales.infrastructure.storage.BlobStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CaseStudyValidationService {

    private static final String CONTAINER_DOCUMENTS = "documents";

    private final CaseStudyAgentRepository caseStudyAgentRepository;
    private final CaseStudyValidationResultRepository validationResultRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final BlobStorageService blobStorageService;
    private final PptTextExtractor pptTextExtractor;
    private final CaseStudyContentExtractor contentExtractor;
    private final CaseStudyContentValidator contentValidator;
    private final CaseStudyFormattingService formattingService;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    private final AclService aclService;
    private final TransactionTemplate transactionTemplate;

    public CaseStudyValidationService(
            CaseStudyAgentRepository caseStudyAgentRepository,
            CaseStudyValidationResultRepository validationResultRepository,
            DocumentVersionRepository documentVersionRepository,
            BlobStorageService blobStorageService,
            PptTextExtractor pptTextExtractor,
            CaseStudyContentExtractor contentExtractor,
            CaseStudyContentValidator contentValidator,
            CaseStudyFormattingService formattingService,
            ObjectMapper objectMapper,
            CurrentUserService currentUserService,
            AclService aclService,
            PlatformTransactionManager transactionManager) {
        this.caseStudyAgentRepository = caseStudyAgentRepository;
        this.validationResultRepository = validationResultRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.blobStorageService = blobStorageService;
        this.pptTextExtractor = pptTextExtractor;
        this.contentExtractor = contentExtractor;
        this.contentValidator = contentValidator;
        this.formattingService = formattingService;
        this.objectMapper = objectMapper;
        this.currentUserService = currentUserService;
        this.aclService = aclService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Async("caseStudyExecutor")
    public void validateCaseStudy(UUID documentVersionId) {
        log.info("Starting async case study validation for version: {}", documentVersionId);
        try {
            transactionTemplate.executeWithoutResult(status -> performValidation(documentVersionId));
        } catch (Exception e) {
            log.error("Failed to validate case study for version: {}", documentVersionId, e);
        }
    }

    void performValidation(UUID documentVersionId) {
        // Get active agent
        Optional<CaseStudyAgent> activeAgentOpt = caseStudyAgentRepository.findFirstByIsActiveTrue();
        if (activeAgentOpt.isEmpty()) {
            log.info("No active case study agent found, skipping validation for version: {}", documentVersionId);
            return;
        }

        CaseStudyAgent agent = activeAgentOpt.get();
        DocumentVersion version = documentVersionRepository.findById(documentVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentVersion", "id", documentVersionId));

        // Parse template config
        TemplateConfig templateConfig;
        try {
            templateConfig = objectMapper.readValue(agent.getTemplateConfig(), TemplateConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse template config for agent: {}", agent.getId(), e);
            return;
        }

        // Check for null/empty sections
        if (templateConfig.getSections() == null || templateConfig.getSections().isEmpty()) {
            log.warn("Template config has no sections defined for agent: {}", agent.getId());
            return;
        }

        // Download and extract text from PPT
        String pptText;
        try (InputStream pptStream = blobStorageService.download(CONTAINER_DOCUMENTS, version.getFilePath())) {
            pptText = pptTextExtractor.extractText(pptStream);
        } catch (IOException e) {
            log.warn("Failed to close PPT stream for version: {}", documentVersionId, e);
            throw new RuntimeException("Failed to process PPT file", e);
        }

        // Extract sections using AI
        String sectionKeys = templateConfig.getSections().stream()
                .map(SectionConfig::getKey)
                .collect(Collectors.joining(", "));

        String extractedContent = contentExtractor.extractSections(pptText, sectionKeys);
        log.info("Extracted content from PPT for version: {}", documentVersionId);

        // Build rules JSON from template config
        String rulesJson;
        try {
            rulesJson = objectMapper.writeValueAsString(templateConfig.getSections());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize template rules", e);
            return;
        }

        // Validate using AI
        String validationResult = contentValidator.validateContent(extractedContent, rulesJson);
        log.info("Validation completed for version: {}", documentVersionId);

        // Determine if valid (check overallScore >= 0.7)
        boolean isValid = false;
        try {
            var resultNode = objectMapper.readTree(validationResult);
            double score = resultNode.path("overallScore").asDouble(0.0);
            isValid = score >= 0.7;
        } catch (Exception e) {
            log.warn("Failed to parse validation score, defaulting to invalid", e);
        }

        // Save validation result
        CaseStudyValidationResult result = CaseStudyValidationResult.builder()
                .documentVersion(version)
                .agent(agent)
                .isValid(isValid)
                .validationDetails(validationResult)
                .build();
        validationResultRepository.save(result);

        log.info("Validation result saved for version: {}, isValid: {}", documentVersionId, isValid);

        // If invalid, trigger formatting
        if (!isValid) {
            log.info("Case study is invalid, triggering formatting for version: {}", documentVersionId);
            formattingService.formatCaseStudy(documentVersionId, extractedContent);
        }
    }

    public ValidationResultDto getValidationResult(UUID documentVersionId) {
        CaseStudyValidationResult result = validationResultRepository
                .findTopByDocumentVersionIdOrderByCreatedAtDesc(documentVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("ValidationResult", "documentVersionId", documentVersionId));

        // Enforce read access on the parent document
        UUID documentId = result.getDocumentVersion().getDocument().getId();
        enforceReadAccess(documentId);

        return toDto(result);
    }

    private void enforceReadAccess(UUID documentId) {
        UserPrincipal user = currentUserService.getCurrentUser();
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return;
        }
        boolean hasAccess = aclService.hasPermission(
                UUID.fromString(user.getUserId()), ResourceType.DOCUMENT, documentId, Permission.READ);
        if (!hasAccess) {
            throw new AccessDeniedException("You do not have read access to this document");
        }
    }

    private ValidationResultDto toDto(CaseStudyValidationResult result) {
        Object details = null;
        if (result.getValidationDetails() != null) {
            try {
                details = objectMapper.readValue(result.getValidationDetails(), Object.class);
            } catch (JsonProcessingException e) {
                details = result.getValidationDetails();
            }
        }
        return ValidationResultDto.builder()
                .id(result.getId())
                .documentVersionId(result.getDocumentVersion().getId())
                .agentId(result.getAgent().getId())
                .isValid(result.getIsValid())
                .validationDetails(details)
                .createdAt(result.getCreatedAt())
                .build();
    }
}
