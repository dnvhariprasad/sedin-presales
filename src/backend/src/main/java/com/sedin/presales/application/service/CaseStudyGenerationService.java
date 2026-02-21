package com.sedin.presales.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.CaseStudyWizardRequest;
import com.sedin.presales.application.dto.CaseStudyWizardResponseDto;
import com.sedin.presales.application.dto.templateconfig.TemplateConfig;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.CaseStudyAgent;
import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.entity.DocumentType;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.enums.DocumentStatus;
import com.sedin.presales.domain.repository.CaseStudyAgentRepository;
import com.sedin.presales.domain.repository.DocumentRepository;
import com.sedin.presales.domain.repository.DocumentTypeRepository;
import com.sedin.presales.domain.repository.DocumentVersionRepository;
import com.sedin.presales.infrastructure.ai.casestudy.CaseStudyContentEnhancer;
import com.sedin.presales.infrastructure.rendition.PptTemplateBuilder;
import com.sedin.presales.infrastructure.storage.BlobStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class CaseStudyGenerationService {

    private static final String CONTAINER_DOCUMENTS = "documents";

    private final CaseStudyAgentRepository caseStudyAgentRepository;
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final BlobStorageService blobStorageService;
    private final PptTemplateBuilder pptTemplateBuilder;
    private final CaseStudyContentEnhancer contentEnhancer;
    private final RenditionService renditionService;
    private final ObjectMapper objectMapper;

    public CaseStudyGenerationService(
            CaseStudyAgentRepository caseStudyAgentRepository,
            DocumentRepository documentRepository,
            DocumentTypeRepository documentTypeRepository,
            DocumentVersionRepository documentVersionRepository,
            BlobStorageService blobStorageService,
            PptTemplateBuilder pptTemplateBuilder,
            CaseStudyContentEnhancer contentEnhancer,
            RenditionService renditionService,
            ObjectMapper objectMapper) {
        this.caseStudyAgentRepository = caseStudyAgentRepository;
        this.documentRepository = documentRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.blobStorageService = blobStorageService;
        this.pptTemplateBuilder = pptTemplateBuilder;
        this.contentEnhancer = contentEnhancer;
        this.renditionService = renditionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CaseStudyWizardResponseDto generateCaseStudy(CaseStudyWizardRequest request) {
        log.info("Generating case study from wizard for: {}", request.getTitle());

        // Get active agent
        CaseStudyAgent agent = caseStudyAgentRepository.findFirstByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("CaseStudyAgent", "status", "active"));

        // Parse template config
        TemplateConfig templateConfig;
        try {
            templateConfig = objectMapper.readValue(agent.getTemplateConfig(), TemplateConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse template config for agent: {}", agent.getId(), e);
            throw new RuntimeException("Failed to parse template config", e);
        }

        // Build content map from wizard request
        Map<String, Object> contentMap = buildContentMap(request);

        // Optionally enhance with AI
        if (request.isEnhanceWithAi()) {
            log.info("Enhancing case study content with AI");
            try {
                String contentJson = objectMapper.writeValueAsString(contentMap);
                String enhancedJson = contentEnhancer.enhanceContent(contentJson);
                Map<String, Object> enhanced = objectMapper.readValue(enhancedJson,
                        objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
                contentMap = enhanced;
                log.info("Content enhanced successfully");
            } catch (Exception e) {
                log.warn("AI enhancement failed, using original content", e);
            }
        }

        // Build PPT
        byte[] pptBytes = pptTemplateBuilder.buildPresentation(templateConfig, contentMap);

        // Find "Case Study" document type
        DocumentType caseStudyType = documentTypeRepository.findByName("Case Study")
                .orElseThrow(() -> new ResourceNotFoundException("DocumentType", "name", "Case Study"));

        // Create Document
        Document document = Document.builder()
                .title(request.getTitle())
                .customerName(request.getCustomerName())
                .status(DocumentStatus.ACTIVE)
                .ragIndexed(false)
                .currentVersionNumber(1)
                .documentType(caseStudyType)
                .build();
        Document savedDocument = documentRepository.save(document);

        // Upload PPT to blob storage
        String fileName = sanitizeFileName(request.getTitle()) + ".pptx";
        String blobPath = String.format("documents/%s/1/%s", savedDocument.getId(), fileName);
        blobStorageService.upload(
                CONTAINER_DOCUMENTS,
                blobPath,
                new ByteArrayInputStream(pptBytes),
                pptBytes.length,
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        );

        // Create DocumentVersion
        DocumentVersion version = DocumentVersion.builder()
                .document(savedDocument)
                .versionNumber(1)
                .filePath(blobPath)
                .fileName(fileName)
                .fileSize((long) pptBytes.length)
                .contentType("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                .build();
        DocumentVersion savedVersion = documentVersionRepository.save(version);

        // Trigger async PDF rendition
        renditionService.generatePdfRendition(savedVersion.getId());

        log.info("Case study generated: documentId={}, versionId={}", savedDocument.getId(), savedVersion.getId());

        return CaseStudyWizardResponseDto.builder()
                .documentId(savedDocument.getId())
                .documentVersionId(savedVersion.getId())
                .message("Case study generated successfully")
                .build();
    }

    private Map<String, Object> buildContentMap(CaseStudyWizardRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", request.getTitle());
        map.put("customerOverview", request.getCustomerOverview());
        map.put("challenges", request.getChallenges());
        map.put("solution", request.getSolution());
        if (request.getTechnologies() != null) {
            map.put("technologies", request.getTechnologies());
        }
        map.put("results", request.getResults());
        return map;
    }

    private String sanitizeFileName(String title) {
        return title.replaceAll("[^a-zA-Z0-9\\-_ ]", "").replaceAll("\\s+", "_").toLowerCase();
    }
}
