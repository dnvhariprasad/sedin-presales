package com.sedin.presales.application.service;

import com.azure.search.documents.SearchDocument;
import com.sedin.presales.domain.entity.Document;
import com.sedin.presales.domain.entity.DocumentMetadata;
import com.sedin.presales.domain.entity.DocumentType;
import com.sedin.presales.domain.entity.DocumentVersion;
import com.sedin.presales.domain.entity.Domain;
import com.sedin.presales.domain.entity.Industry;
import com.sedin.presales.domain.enums.DocumentStatus;
import com.sedin.presales.domain.repository.DocumentMetadataRepository;
import com.sedin.presales.domain.repository.DocumentRepository;
import com.sedin.presales.domain.repository.DocumentVersionRepository;
import com.sedin.presales.infrastructure.ai.DocumentIntelligenceService;
import com.sedin.presales.infrastructure.ai.EmbeddingService;
import com.sedin.presales.infrastructure.search.AzureSearchService;
import com.sedin.presales.infrastructure.storage.BlobStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexingServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private DocumentMetadataRepository documentMetadataRepository;

    @Mock
    private BlobStorageService blobStorageService;

    @Mock
    private DocumentIntelligenceService documentIntelligenceService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private AzureSearchService azureSearchService;

    @InjectMocks
    private IndexingService indexingService;

    @Captor
    private ArgumentCaptor<List<SearchDocument>> searchDocumentsCaptor;

    @Test
    @DisplayName("indexDocument should extract text, chunk, embed, and upload to search index")
    void indexDocument_shouldExtractTextChunkEmbedAndUpload() {
        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        DocumentType documentType = DocumentType.builder()
                .name("Case Study")
                .build();

        Domain domain = Domain.builder()
                .name("Healthcare")
                .build();

        Industry industry = Industry.builder()
                .name("Pharma")
                .build();

        Document document = Document.builder()
                .title("Test Document")
                .customerName("Acme Corp")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(1)
                .ragIndexed(false)
                .documentType(documentType)
                .build();
        document.setId(documentId);
        document.setCreatedAt(Instant.now());

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(1)
                .filePath("documents/" + documentId + "/1/test.pdf")
                .fileName("test.pdf")
                .contentType("application/pdf")
                .build();
        version.setId(versionId);

        DocumentMetadata metadata = DocumentMetadata.builder()
                .document(document)
                .domain(domain)
                .industry(industry)
                .build();

        InputStream fileStream = new ByteArrayInputStream("file content".getBytes());
        // Text shorter than chunk size so we get 1 chunk
        String extractedText = "This is the extracted text from the document for indexing purposes.";

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(documentId, 1))
                .thenReturn(Optional.of(version));
        when(blobStorageService.download("documents", version.getFilePath())).thenReturn(fileStream);
        when(documentIntelligenceService.extractText(fileStream, "application/pdf")).thenReturn(extractedText);
        when(embeddingService.generateEmbeddings(anyList()))
                .thenReturn(List.of(List.of(0.1f, 0.2f, 0.3f)));
        when(documentMetadataRepository.findByDocumentId(documentId)).thenReturn(Optional.of(metadata));
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        indexingService.indexDocument(documentId);

        verify(blobStorageService).download("documents", version.getFilePath());
        verify(documentIntelligenceService).extractText(fileStream, "application/pdf");
        verify(embeddingService).generateEmbeddings(anyList());
        verify(azureSearchService).deleteDocumentChunks(documentId.toString());
        verify(azureSearchService).uploadDocuments(searchDocumentsCaptor.capture());

        List<SearchDocument> uploadedDocs = searchDocumentsCaptor.getValue();
        assertThat(uploadedDocs).isNotEmpty();
        assertThat(uploadedDocs.get(0).get("documentId")).isEqualTo(documentId.toString());
        assertThat(uploadedDocs.get(0).get("title")).isEqualTo("Test Document");
        assertThat(uploadedDocs.get(0).get("customerName")).isEqualTo("Acme Corp");
        assertThat(uploadedDocs.get(0).get("domain")).isEqualTo("Healthcare");
        assertThat(uploadedDocs.get(0).get("industry")).isEqualTo("Pharma");
        assertThat(uploadedDocs.get(0).get("documentType")).isEqualTo("Case Study");

        verify(documentRepository).save(document);
        assertThat(document.getRagIndexed()).isTrue();
    }

    @Test
    @DisplayName("indexDocument should skip when no text is extracted")
    void indexDocument_shouldSkipWhenNoTextExtracted() {
        UUID documentId = UUID.randomUUID();

        Document document = Document.builder()
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .currentVersionNumber(1)
                .ragIndexed(false)
                .build();
        document.setId(documentId);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(1)
                .filePath("documents/" + documentId + "/1/test.pdf")
                .contentType("application/pdf")
                .build();
        version.setId(UUID.randomUUID());

        InputStream fileStream = new ByteArrayInputStream(new byte[0]);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentVersionRepository.findByDocumentIdAndVersionNumber(documentId, 1))
                .thenReturn(Optional.of(version));
        when(blobStorageService.download("documents", version.getFilePath())).thenReturn(fileStream);
        when(documentIntelligenceService.extractText(fileStream, "application/pdf")).thenReturn("");

        indexingService.indexDocument(documentId);

        verify(azureSearchService, never()).uploadDocuments(anyList());
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    @DisplayName("removeFromIndex should delete chunks and update ragIndexed flag")
    void removeFromIndex_shouldDeleteChunksAndUpdateFlag() {
        UUID documentId = UUID.randomUUID();

        Document document = Document.builder()
                .title("Test Document")
                .status(DocumentStatus.ACTIVE)
                .ragIndexed(true)
                .build();
        document.setId(documentId);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        indexingService.removeFromIndex(documentId);

        verify(azureSearchService).deleteDocumentChunks(documentId.toString());
        verify(documentRepository).save(document);
        assertThat(document.getRagIndexed()).isFalse();
    }

    @Test
    @DisplayName("chunkText should split text correctly with overlap")
    void chunkText_shouldSplitTextCorrectly() {
        // 20 chars, chunk size 10, overlap 3 => chunks at [0,10), [7,17), [14,20)
        String text = "01234567890123456789";

        List<String> chunks = indexingService.chunkText(text, 10, 3);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).isEqualTo("0123456789");
        assertThat(chunks.get(1)).isEqualTo("7890123456");
        assertThat(chunks.get(2)).isEqualTo("456789");
    }

    @Test
    @DisplayName("chunkText should return empty list for null input")
    void chunkText_shouldReturnEmptyForNullInput() {
        List<String> chunks = indexingService.chunkText(null, 1000, 100);
        assertThat(chunks).isEmpty();
    }
}
