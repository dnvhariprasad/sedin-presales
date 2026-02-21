package com.sedin.presales.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.templateconfig.TemplateConfig;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.domain.entity.CaseStudyAgent;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.entity.Rendition;
import com.sedin.presales.domain.enums.RenditionStatus;
import com.sedin.presales.domain.enums.RenditionType;
import com.sedin.presales.domain.repository.CaseStudyAgentRepository;
import com.sedin.presales.domain.repository.DocumentVersionRepository;
import com.sedin.presales.domain.repository.RenditionRepository;
import com.sedin.presales.infrastructure.rendition.PptTemplateBuilder;
import com.sedin.presales.infrastructure.storage.BlobStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class CaseStudyFormattingService {

    private static final String CONTAINER_RENDITIONS = "renditions";

    private final CaseStudyAgentRepository caseStudyAgentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final RenditionRepository renditionRepository;
    private final BlobStorageService blobStorageService;
    private final PptTemplateBuilder pptTemplateBuilder;
    private final ObjectMapper objectMapper;

    public CaseStudyFormattingService(
            CaseStudyAgentRepository caseStudyAgentRepository,
            DocumentVersionRepository documentVersionRepository,
            RenditionRepository renditionRepository,
            BlobStorageService blobStorageService,
            PptTemplateBuilder pptTemplateBuilder,
            ObjectMapper objectMapper) {
        this.caseStudyAgentRepository = caseStudyAgentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.renditionRepository = renditionRepository;
        this.blobStorageService = blobStorageService;
        this.pptTemplateBuilder = pptTemplateBuilder;
        this.objectMapper = objectMapper;
    }

    @Async("caseStudyExecutor")
    public void formatCaseStudy(UUID documentVersionId, String extractedContentJson) {
        log.info("Starting async case study formatting for version: {}", documentVersionId);
        try {
            performFormatting(documentVersionId, extractedContentJson);
        } catch (Exception e) {
            log.error("Failed to format case study for version: {}", documentVersionId, e);
        }
    }

    @Transactional
    public void performFormatting(UUID documentVersionId, String extractedContentJson) {
        DocumentVersion version = documentVersionRepository.findById(documentVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentVersion", "id", documentVersionId));

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

        // Parse extracted content
        Map<String, Object> contentMap;
        try {
            contentMap = objectMapper.readValue(extractedContentJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse extracted content JSON", e);
            throw new RuntimeException("Failed to parse extracted content", e);
        }

        // Build the blob path upfront so it can be set on the rendition record
        String blobPath = String.format("renditions/%s/formatted.pptx", documentVersionId);

        // Create rendition record
        Rendition rendition = Rendition.builder()
                .renditionType(RenditionType.FORMATTED)
                .status(RenditionStatus.PROCESSING)
                .filePath(blobPath)
                .documentVersion(version)
                .build();
        rendition = renditionRepository.save(rendition);

        try {
            // Build PPT using template
            byte[] pptBytes = pptTemplateBuilder.buildPresentation(templateConfig, contentMap);

            // Upload to blob storage
            blobStorageService.upload(
                    CONTAINER_RENDITIONS,
                    blobPath,
                    new ByteArrayInputStream(pptBytes),
                    pptBytes.length,
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            );

            // Update rendition as completed
            rendition.setStatus(RenditionStatus.COMPLETED);
            rendition.setFileSize((long) pptBytes.length);
            renditionRepository.save(rendition);

            log.info("Formatted case study rendition completed for version: {}, size: {} bytes", documentVersionId, pptBytes.length);

        } catch (Exception e) {
            log.error("Failed to create formatted rendition for version: {}", documentVersionId, e);
            rendition.setStatus(RenditionStatus.FAILED);
            rendition.setErrorMessage(e.getMessage());
            renditionRepository.save(rendition);
        }
    }
}
