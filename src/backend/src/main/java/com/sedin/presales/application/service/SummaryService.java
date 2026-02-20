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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class SummaryService {

    private static final String CONTAINER_DOCUMENTS = "documents";

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final RenditionRepository renditionRepository;
    private final DocumentIntelligenceService documentIntelligenceService;
    private final SummarizationService summarizationService;
    private final BlobStorageService blobStorageService;

    @Value("${azure.storage.container-summaries}")
    private String containerSummaries;

    public SummaryService(DocumentRepository documentRepository,
                          DocumentVersionRepository documentVersionRepository,
                          RenditionRepository renditionRepository,
                          DocumentIntelligenceService documentIntelligenceService,
                          SummarizationService summarizationService,
                          BlobStorageService blobStorageService) {
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.renditionRepository = renditionRepository;
        this.documentIntelligenceService = documentIntelligenceService;
        this.summarizationService = summarizationService;
        this.blobStorageService = blobStorageService;
    }

    @Async("renditionExecutor")
    public void generateSummary(UUID documentVersionId) {
        log.info("Starting async summary generation for document version: {}", documentVersionId);
        try {
            processSummary(documentVersionId);
        } catch (Exception e) {
            log.error("Failed to generate summary for document version: {}", documentVersionId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSummary(UUID documentVersionId) {
        DocumentVersion version = documentVersionRepository.findById(documentVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentVersion", "id", documentVersionId));

        // Check if SUMMARY rendition already exists and is COMPLETED
        Optional<Rendition> existingRendition = renditionRepository
                .findByDocumentVersionIdAndRenditionType(documentVersionId, RenditionType.SUMMARY);

        if (existingRendition.isPresent()) {
            Rendition existing = existingRendition.get();
            if (existing.getStatus() == RenditionStatus.COMPLETED) {
                log.info("Summary rendition already completed for document version: {}, skipping", documentVersionId);
                return;
            }
            // Delete existing PENDING/PROCESSING/FAILED rendition and regenerate
            log.info("Deleting existing {} summary rendition for document version: {}", existing.getStatus(), documentVersionId);
            renditionRepository.delete(existing);
            renditionRepository.flush();
        }

        // Create rendition record with PENDING status
        Rendition rendition = Rendition.builder()
                .renditionType(RenditionType.SUMMARY)
                .status(RenditionStatus.PENDING)
                .documentVersion(version)
                .build();
        rendition = renditionRepository.save(rendition);

        // Update to PROCESSING
        rendition.setStatus(RenditionStatus.PROCESSING);
        rendition = renditionRepository.save(rendition);

        try {
            // Download original file from blob storage
            log.info("Downloading original file from path: {}", version.getFilePath());
            InputStream originalFileStream = blobStorageService.download(CONTAINER_DOCUMENTS, version.getFilePath());

            // Extract text using Document Intelligence
            String extractedText = documentIntelligenceService.extractText(originalFileStream, version.getContentType());

            if (extractedText == null || extractedText.isBlank()) {
                throw new RuntimeException("No text could be extracted from the document");
            }

            // Generate summary using Azure OpenAI
            String documentTitle = version.getDocument().getTitle();
            String summary = summarizationService.summarize(extractedText, documentTitle);

            // Upload summary to blob storage
            byte[] summaryBytes = summary.getBytes(StandardCharsets.UTF_8);
            String blobPath = String.format("summaries/%s/summary.txt", documentVersionId);
            log.info("Uploading summary to path: {}", blobPath);
            blobStorageService.upload(
                    containerSummaries,
                    blobPath,
                    new ByteArrayInputStream(summaryBytes),
                    summaryBytes.length,
                    "text/plain"
            );

            // Update rendition as completed
            rendition.setStatus(RenditionStatus.COMPLETED);
            rendition.setFilePath(blobPath);
            rendition.setFileSize((long) summaryBytes.length);
            renditionRepository.save(rendition);

            log.info("Summary generation completed for document version: {}, size: {} bytes", documentVersionId, summaryBytes.length);

        } catch (Exception e) {
            log.error("Failed to generate summary for document version: {}", documentVersionId, e);
            rendition.setStatus(RenditionStatus.FAILED);
            rendition.setErrorMessage(e.getMessage());
            renditionRepository.save(rendition);
        }
    }

    public SummaryResponseDto getSummaryStatus(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        DocumentVersion currentVersion = documentVersionRepository
                .findByDocumentIdAndVersionNumber(documentId, document.getCurrentVersionNumber())
                .orElseThrow(() -> new ResourceNotFoundException("DocumentVersion", "documentId/versionNumber",
                        documentId + "/" + document.getCurrentVersionNumber()));

        Optional<Rendition> renditionOpt = renditionRepository
                .findByDocumentVersionIdAndRenditionType(currentVersion.getId(), RenditionType.SUMMARY);

        if (renditionOpt.isEmpty()) {
            // No summary exists — trigger generation
            generateSummary(currentVersion.getId());
            return SummaryResponseDto.builder()
                    .documentId(documentId)
                    .status(RenditionStatus.PENDING)
                    .message("Summary generation has been initiated")
                    .build();
        }

        Rendition rendition = renditionOpt.get();

        return switch (rendition.getStatus()) {
            case COMPLETED -> {
                String summaryText = downloadSummaryText(rendition.getFilePath());
                yield SummaryResponseDto.builder()
                        .documentId(documentId)
                        .summary(summaryText)
                        .status(RenditionStatus.COMPLETED)
                        .message("Summary is available")
                        .build();
            }
            case PENDING, PROCESSING -> SummaryResponseDto.builder()
                    .documentId(documentId)
                    .status(rendition.getStatus())
                    .message("Summary is being generated")
                    .build();
            case FAILED -> SummaryResponseDto.builder()
                    .documentId(documentId)
                    .status(RenditionStatus.FAILED)
                    .message("Summary generation failed: " + rendition.getErrorMessage())
                    .build();
        };
    }

    public SummaryResponseDto regenerateSummary(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        DocumentVersion currentVersion = documentVersionRepository
                .findByDocumentIdAndVersionNumber(documentId, document.getCurrentVersionNumber())
                .orElseThrow(() -> new ResourceNotFoundException("DocumentVersion", "documentId/versionNumber",
                        documentId + "/" + document.getCurrentVersionNumber()));

        // Delete existing summary rendition if present
        Optional<Rendition> existingRendition = renditionRepository
                .findByDocumentVersionIdAndRenditionType(currentVersion.getId(), RenditionType.SUMMARY);
        existingRendition.ifPresent(renditionRepository::delete);

        // Trigger async regeneration
        generateSummary(currentVersion.getId());

        return SummaryResponseDto.builder()
                .documentId(documentId)
                .status(RenditionStatus.PENDING)
                .message("Summary regeneration has been initiated")
                .build();
    }

    private String downloadSummaryText(String filePath) {
        try {
            InputStream inputStream = blobStorageService.download(containerSummaries, filePath);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to download summary from path: {}", filePath, e);
            return null;
        }
    }
}
