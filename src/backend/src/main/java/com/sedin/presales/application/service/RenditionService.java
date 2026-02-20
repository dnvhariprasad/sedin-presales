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
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class RenditionService {

    private static final String CONTAINER_DOCUMENTS = "documents";
    private static final String CONTAINER_RENDITIONS = "renditions";

    private final RenditionRepository renditionRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final PdfRenditionService pdfRenditionService;
    private final BlobStorageService blobStorageService;

    public RenditionService(RenditionRepository renditionRepository,
                            DocumentVersionRepository documentVersionRepository,
                            PdfRenditionService pdfRenditionService,
                            BlobStorageService blobStorageService) {
        this.renditionRepository = renditionRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.pdfRenditionService = pdfRenditionService;
        this.blobStorageService = blobStorageService;
    }

    @Async("renditionExecutor")
    public void generatePdfRendition(UUID documentVersionId) {
        log.info("Starting async PDF rendition generation for document version: {}", documentVersionId);
        try {
            processRendition(documentVersionId);
        } catch (Exception e) {
            log.error("Failed to generate PDF rendition for document version: {}", documentVersionId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processRendition(UUID documentVersionId) {
        DocumentVersion version = documentVersionRepository.findById(documentVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentVersion", "id", documentVersionId));

        // Check if a PDF rendition already exists
        Optional<Rendition> existingRendition = renditionRepository
                .findByDocumentVersionIdAndRenditionType(documentVersionId, RenditionType.PDF);

        if (existingRendition.isPresent()) {
            Rendition existing = existingRendition.get();
            if (existing.getStatus() == RenditionStatus.COMPLETED) {
                log.info("PDF rendition already completed for document version: {}, skipping", documentVersionId);
                return;
            }
            // Delete existing PENDING/PROCESSING/FAILED rendition and regenerate
            log.info("Deleting existing {} PDF rendition for document version: {}", existing.getStatus(), documentVersionId);
            renditionRepository.delete(existing);
            renditionRepository.flush();
        }

        // Create rendition record with PENDING status
        Rendition rendition = Rendition.builder()
                .renditionType(RenditionType.PDF)
                .status(RenditionStatus.PENDING)
                .documentVersion(version)
                .build();
        rendition = renditionRepository.save(rendition);

        // Update to PROCESSING
        rendition.setStatus(RenditionStatus.PROCESSING);
        rendition = renditionRepository.save(rendition);

        try {
            // Download original file
            log.info("Downloading original file from path: {}", version.getFilePath());
            InputStream originalFileStream = blobStorageService.download(CONTAINER_DOCUMENTS, version.getFilePath());

            // Convert to PDF
            byte[] pdfBytes = pdfRenditionService.convertToPdf(originalFileStream, version.getContentType());

            // Upload PDF rendition to blob storage
            String blobPath = String.format("renditions/%s/document.pdf", documentVersionId);
            log.info("Uploading PDF rendition to path: {}", blobPath);
            blobStorageService.upload(
                    CONTAINER_RENDITIONS,
                    blobPath,
                    new ByteArrayInputStream(pdfBytes),
                    pdfBytes.length,
                    "application/pdf"
            );

            // Update rendition as completed
            rendition.setStatus(RenditionStatus.COMPLETED);
            rendition.setFilePath(blobPath);
            rendition.setFileSize((long) pdfBytes.length);
            renditionRepository.save(rendition);

            log.info("PDF rendition completed for document version: {}, size: {} bytes", documentVersionId, pdfBytes.length);
        } catch (Exception e) {
            log.error("Failed to generate PDF rendition for document version: {}", documentVersionId, e);
            rendition.setStatus(RenditionStatus.FAILED);
            rendition.setErrorMessage(e.getMessage());
            renditionRepository.save(rendition);
        }
    }

    public RenditionDto getRendition(UUID documentVersionId, RenditionType type) {
        return renditionRepository.findByDocumentVersionIdAndRenditionType(documentVersionId, type)
                .map(this::toDto)
                .orElse(null);
    }

    public List<RenditionDto> getRenditions(UUID documentVersionId) {
        return renditionRepository.findByDocumentVersionId(documentVersionId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private RenditionDto toDto(Rendition rendition) {
        return RenditionDto.builder()
                .id(rendition.getId())
                .renditionType(rendition.getRenditionType())
                .status(rendition.getStatus())
                .filePath(rendition.getFilePath())
                .fileSize(rendition.getFileSize())
                .errorMessage(rendition.getErrorMessage())
                .createdAt(rendition.getCreatedAt())
                .build();
    }
}
