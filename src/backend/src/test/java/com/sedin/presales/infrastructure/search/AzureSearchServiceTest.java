package com.sedin.presales.infrastructure.search;

import com.azure.core.util.Context;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.models.SearchIndex;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchResult;
import com.azure.search.documents.util.SearchPagedIterable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureSearchServiceTest {

    @Mock
    private SearchClient searchClient;

    @Mock
    private SearchIndexClient searchIndexClient;

    @InjectMocks
    private AzureSearchService azureSearchService;

    @Test
    @DisplayName("createOrUpdateIndex should create index with correct schema when auto-create is enabled")
    void createOrUpdateIndex_shouldCreateIndex() {
        // Arrange
        ReflectionTestUtils.setField(azureSearchService, "indexName", "presales-documents");
        ReflectionTestUtils.setField(azureSearchService, "autoCreateIndex", true);

        when(searchIndexClient.createOrUpdateIndex(any(SearchIndex.class)))
                .thenReturn(new SearchIndex("presales-documents"));

        // Act
        azureSearchService.createOrUpdateIndex();

        // Assert
        ArgumentCaptor<SearchIndex> indexCaptor = ArgumentCaptor.forClass(SearchIndex.class);
        verify(searchIndexClient).createOrUpdateIndex(indexCaptor.capture());

        SearchIndex capturedIndex = indexCaptor.getValue();
        assertThat(capturedIndex.getName()).isEqualTo("presales-documents");
        assertThat(capturedIndex.getFields()).isNotEmpty();
        assertThat(capturedIndex.getVectorSearch()).isNotNull();
    }

    @Test
    @DisplayName("createOrUpdateIndex should skip when auto-create is disabled")
    void createOrUpdateIndex_shouldSkipWhenDisabled() {
        // Arrange
        ReflectionTestUtils.setField(azureSearchService, "autoCreateIndex", false);

        // Act
        azureSearchService.createOrUpdateIndex();

        // Assert
        verify(searchIndexClient, never()).createOrUpdateIndex(any());
    }

    @Test
    @DisplayName("uploadDocuments should call searchClient.uploadDocuments")
    void uploadDocuments_shouldCallSearchClient() {
        // Arrange
        ReflectionTestUtils.setField(azureSearchService, "indexName", "presales-documents");

        SearchDocument doc1 = new SearchDocument();
        doc1.put("id", "chunk-1");
        doc1.put("content", "Test content");

        SearchDocument doc2 = new SearchDocument();
        doc2.put("id", "chunk-2");
        doc2.put("content", "More content");

        List<SearchDocument> documents = List.of(doc1, doc2);

        // Act
        azureSearchService.uploadDocuments(documents);

        // Assert
        verify(searchClient).uploadDocuments(documents);
    }

    @Test
    @DisplayName("deleteDocumentChunks should search and delete matching chunks")
    @SuppressWarnings("unchecked")
    void deleteDocumentChunks_shouldSearchAndDelete() {
        // Arrange
        ReflectionTestUtils.setField(azureSearchService, "indexName", "presales-documents");
        String documentId = "doc-123";

        SearchDocument resultDoc = new SearchDocument();
        resultDoc.put("id", "chunk-1");

        SearchResult searchResult = mock(SearchResult.class);
        when(searchResult.getDocument(SearchDocument.class)).thenReturn(resultDoc);

        SearchPagedIterable pagedIterable = mock(SearchPagedIterable.class);
        Iterator<SearchResult> iterator = mock(Iterator.class);
        when(iterator.hasNext()).thenReturn(true, false);
        when(iterator.next()).thenReturn(searchResult);
        when(pagedIterable.iterator()).thenReturn(iterator);

        when(searchClient.search(eq("*"), any(SearchOptions.class), any(Context.class)))
                .thenReturn(pagedIterable);

        // Act
        azureSearchService.deleteDocumentChunks(documentId);

        // Assert
        verify(searchClient).search(eq("*"), any(SearchOptions.class), any(Context.class));
        verify(searchClient).deleteDocuments(any(List.class));
    }

    @Test
    @DisplayName("deleteDocumentChunks should not call delete when no chunks found")
    void deleteDocumentChunks_shouldNotDeleteWhenEmpty() {
        // Arrange
        ReflectionTestUtils.setField(azureSearchService, "indexName", "presales-documents");
        String documentId = "doc-999";

        SearchPagedIterable pagedIterable = mock(SearchPagedIterable.class);
        @SuppressWarnings("unchecked")
        Iterator<SearchResult> iterator = mock(Iterator.class);
        when(iterator.hasNext()).thenReturn(false);
        when(pagedIterable.iterator()).thenReturn(iterator);

        when(searchClient.search(eq("*"), any(SearchOptions.class), any(Context.class)))
                .thenReturn(pagedIterable);

        // Act
        azureSearchService.deleteDocumentChunks(documentId);

        // Assert
        verify(searchClient).search(eq("*"), any(SearchOptions.class), any(Context.class));
        verify(searchClient, never()).deleteDocuments(any(List.class));
    }

    @Test
    @DisplayName("hybridSearch should return search results with correct options")
    void hybridSearch_shouldReturnResults() {
        // Arrange
        String queryText = "cloud migration case study";
        List<Float> queryVector = List.of(0.1f, 0.2f, 0.3f);
        int topK = 5;
        String filter = "domain eq 'Healthcare'";

        SearchPagedIterable expectedResults = mock(SearchPagedIterable.class);
        when(searchClient.search(eq(queryText), any(SearchOptions.class), any(Context.class)))
                .thenReturn(expectedResults);

        // Act
        SearchPagedIterable result = azureSearchService.hybridSearch(queryText, queryVector, topK, filter);

        // Assert
        assertThat(result).isEqualTo(expectedResults);
        verify(searchClient).search(eq(queryText), any(SearchOptions.class), any(Context.class));
    }

    @Test
    @DisplayName("hybridSearch should work without OData filter")
    void hybridSearch_shouldWorkWithoutFilter() {
        // Arrange
        String queryText = "test query";
        List<Float> queryVector = List.of(0.1f, 0.2f);
        int topK = 10;

        SearchPagedIterable expectedResults = mock(SearchPagedIterable.class);
        when(searchClient.search(eq(queryText), any(SearchOptions.class), any(Context.class)))
                .thenReturn(expectedResults);

        // Act
        SearchPagedIterable result = azureSearchService.hybridSearch(queryText, queryVector, topK, null);

        // Assert
        assertThat(result).isEqualTo(expectedResults);
        verify(searchClient).search(eq(queryText), any(SearchOptions.class), any(Context.class));
    }

    @Test
    @DisplayName("uploadDocuments should throw RuntimeException on failure")
    void uploadDocuments_shouldThrowOnFailure() {
        // Arrange
        ReflectionTestUtils.setField(azureSearchService, "indexName", "presales-documents");

        when(searchClient.uploadDocuments(any()))
                .thenThrow(new RuntimeException("Upload failed"));

        // Act & Assert
        assertThatThrownBy(() -> azureSearchService.uploadDocuments(List.of(new SearchDocument())))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload documents");
    }
}
