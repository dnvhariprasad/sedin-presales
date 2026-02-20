package com.sedin.presales.application.service;

import com.azure.search.documents.SearchDocument;
import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.entity.DocumentMetadata;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.repository.DocumentMetadataRepository;
import com.sedin.presales.domain.repository.DocumentRepository;
import com.sedin.presales.domain.repository.DocumentVersionRepository;
import com.sedin.presales.infrastructure.ai.DocumentIntelligenceService;
import com.sedin.presales.infrastructure.ai.EmbeddingService;
import com.sedin.presales.infrastructure.search.AzureSearchService;
import com.sedin.presales.infrastructure.storage.BlobStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IndexingService {

    private static final String CONTAINER_NAME = "documents";
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 100;

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final BlobStorageService blobStorageService;
    private final DocumentIntelligenceService documentIntelligenceService;
    private final EmbeddingService embeddingService;
    private final AzureSearchService azureSearchService;

    public IndexingService(DocumentRepository documentRepository,
                           DocumentVersionRepository documentVersionRepository,
                           DocumentMetadataRepository documentMetadataRepository,
                           BlobStorageService blobStorageService,
                           DocumentIntelligenceService documentIntelligenceService,
                           EmbeddingService embeddingService,
                           AzureSearchService azureSearchService) {
        this.documentRepository = documentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.documentMetadataRepository = documentMetadataRepository;
        this.blobStorageService = blobStorageService;
        this.documentIntelligenceService = documentIntelligenceService;
        this.embeddingService = embeddingService;
        this.azureSearchService = azureSearchService;
    }

    @Async("indexingExecutor")
    @Transactional
    public void indexDocument(UUID documentId) {
        log.info("Starting indexing for document: {}", documentId);
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

            DocumentVersion version = documentVersionRepository
                    .findByDocumentIdAndVersionNumber(documentId, document.getCurrentVersionNumber())
                    .orElseThrow(() -> new RuntimeException("Version not found for document: " + documentId));

            // Download file and extract text
            InputStream fileStream = blobStorageService.download(CONTAINER_NAME, version.getFilePath());
            String extractedText = documentIntelligenceService.extractText(fileStream, version.getContentType());

            if (extractedText == null || extractedText.isBlank()) {
                log.warn("No text extracted for document: {}, skipping indexing", documentId);
                return;
            }

            // Chunk text
            List<String> chunks = chunkText(extractedText, CHUNK_SIZE, CHUNK_OVERLAP);
            log.info("Document {} split into {} chunks", documentId, chunks.size());

            // Generate embeddings for all chunks
            List<List<Float>> embeddings = embeddingService.generateEmbeddings(chunks);

            // Build metadata context
            Optional<DocumentMetadata> metadataOpt = documentMetadataRepository.findByDocumentId(documentId);

            // Delete existing chunks first
            azureSearchService.deleteDocumentChunks(documentId.toString());

            // Build search documents
            List<SearchDocument> searchDocuments = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                SearchDocument searchDoc = new SearchDocument();
                searchDoc.put("id", documentId + "_chunk_" + i);
                searchDoc.put("documentId", documentId.toString());
                searchDoc.put("versionId", version.getId().toString());
                searchDoc.put("chunkIndex", i);
                searchDoc.put("title", document.getTitle());
                searchDoc.put("content", chunks.get(i));
                searchDoc.put("contentVector", embeddings.get(i));
                searchDoc.put("customerName", document.getCustomerName());

                if (document.getDocumentType() != null) {
                    searchDoc.put("documentType", document.getDocumentType().getName());
                }

                metadataOpt.ifPresent(metadata -> {
                    if (metadata.getDomain() != null) searchDoc.put("domain", metadata.getDomain().getName());
                    if (metadata.getIndustry() != null) searchDoc.put("industry", metadata.getIndustry().getName());
                    if (metadata.getBusinessUnit() != null) searchDoc.put("businessUnit", metadata.getBusinessUnit().getName());
                    if (metadata.getSbu() != null) searchDoc.put("sbu", metadata.getSbu().getName());
                    if (metadata.getTechnologies() != null && !metadata.getTechnologies().isEmpty()) {
                        searchDoc.put("technologies", metadata.getTechnologies().stream()
                                .map(t -> t.getName()).collect(Collectors.toList()));
                    }
                });

                searchDoc.put("createdDate", document.getCreatedAt());
                searchDocuments.add(searchDoc);
            }

            // Upload to search index
            azureSearchService.uploadDocuments(searchDocuments);

            // Update rag_indexed flag
            document.setRagIndexed(true);
            documentRepository.save(document);

            log.info("Successfully indexed document: {} with {} chunks", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Failed to index document: {}", documentId, e);
        }
    }

    @Async("indexingExecutor")
    @Transactional
    public void removeFromIndex(UUID documentId) {
        log.info("Removing document from index: {}", documentId);
        try {
            azureSearchService.deleteDocumentChunks(documentId.toString());

            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
            document.setRagIndexed(false);
            documentRepository.save(document);

            log.info("Successfully removed document from index: {}", documentId);
        } catch (Exception e) {
            log.error("Failed to remove document from index: {}", documentId, e);
        }
    }

    // Visible for testing
    List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += chunkSize - overlap;
        }
        return chunks;
    }
}
