package com.sedin.presales.application.service;

import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.models.SearchResult;
import com.azure.search.documents.util.SearchPagedIterable;
import com.sedin.presales.application.dto.SearchRequestDto;
import com.sedin.presales.application.dto.SearchResponseDto;
import com.sedin.presales.config.CurrentUserService;
import com.sedin.presales.config.UserPrincipal;
import com.sedin.presales.domain.enums.Permission;
import com.sedin.presales.domain.enums.ResourceType;
import com.sedin.presales.infrastructure.ai.EmbeddingService;
import com.sedin.presales.infrastructure.search.AzureSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private AzureSearchService azureSearchService;

    @Mock
    private AclService aclService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private RestTemplate restTemplate;

    private SearchService searchService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID testDocumentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        searchService = new SearchService(
                embeddingService, azureSearchService, aclService,
                currentUserService, restTemplate, objectMapper);
    }

    private UserPrincipal adminUser() {
        return UserPrincipal.builder()
                .userId(UUID.randomUUID().toString())
                .email("admin@sedin.com")
                .displayName("Admin User")
                .role("ADMIN")
                .build();
    }

    private UserPrincipal editorUser(String userId) {
        return UserPrincipal.builder()
                .userId(userId)
                .email("editor@sedin.com")
                .displayName("Editor User")
                .role("EDITOR")
                .build();
    }

    private SearchPagedIterable mockSearchResults(UUID... documentIds) {
        SearchPagedIterable mockIterable = org.mockito.Mockito.mock(SearchPagedIterable.class);

        List<SearchResult> resultList = new ArrayList<>();
        for (UUID docId : documentIds) {
            SearchResult mockResult = org.mockito.Mockito.mock(SearchResult.class);
            SearchDocument mockDoc = new SearchDocument();
            mockDoc.put("documentId", docId.toString());
            mockDoc.put("title", "Test Document " + docId.toString().substring(0, 8));
            mockDoc.put("customerName", "Test Customer");
            mockDoc.put("content", "This is test content for the document.");
            mockDoc.put("domain", "Technology");
            mockDoc.put("industry", "Finance");
            mockDoc.put("documentType", "Case Study");
            mockDoc.put("technologies", List.of("Java", "Spring"));
            when(mockResult.getDocument(SearchDocument.class)).thenReturn(mockDoc);
            when(mockResult.getScore()).thenReturn(0.95);
            resultList.add(mockResult);
        }

        when(mockIterable.iterator()).thenReturn(resultList.iterator());
        return mockIterable;
    }

    private SearchPagedIterable emptySearchResults() {
        SearchPagedIterable mockIterable = org.mockito.Mockito.mock(SearchPagedIterable.class);
        when(mockIterable.iterator()).thenReturn(Collections.<SearchResult>emptyList().iterator());
        return mockIterable;
    }

    @Test
    @DisplayName("search should return filtered results for admin user")
    void search_shouldReturnFilteredResults() {
        // Arrange
        SearchRequestDto request = SearchRequestDto.builder()
                .query("test query")
                .topK(10)
                .build();

        SearchPagedIterable mockResults = mockSearchResults(testDocumentId);

        List<Float> mockVector = List.of(0.1f, 0.2f, 0.3f);
        when(embeddingService.generateEmbedding("test query")).thenReturn(mockVector);
        when(azureSearchService.hybridSearch(eq("test query"), eq(mockVector), eq(30), isNull()))
                .thenReturn(mockResults);
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());

        // Act
        SearchResponseDto response = searchService.search(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getDocumentId()).isEqualTo(testDocumentId);
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getQuery()).isEqualTo("test query");
        assertThat(response.getRagAnswer()).isNull();
    }

    @Test
    @DisplayName("search should apply ACL filter for non-admin user")
    void search_shouldApplyAclFilterForNonAdmin() {
        // Arrange
        UUID accessibleDocId = UUID.randomUUID();
        UUID inaccessibleDocId = UUID.randomUUID();
        String userId = UUID.randomUUID().toString();

        SearchRequestDto request = SearchRequestDto.builder()
                .query("test query")
                .topK(10)
                .build();

        SearchPagedIterable mockResults = mockSearchResults(accessibleDocId, inaccessibleDocId);

        List<Float> mockVector = List.of(0.1f, 0.2f, 0.3f);
        when(embeddingService.generateEmbedding("test query")).thenReturn(mockVector);
        when(azureSearchService.hybridSearch(eq("test query"), eq(mockVector), eq(30), isNull()))
                .thenReturn(mockResults);
        when(currentUserService.getCurrentUser()).thenReturn(editorUser(userId));
        when(aclService.getAccessibleResourceIds(UUID.fromString(userId), ResourceType.DOCUMENT, Permission.READ))
                .thenReturn(Set.of(accessibleDocId));

        // Act
        SearchResponseDto response = searchService.search(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getDocumentId()).isEqualTo(accessibleDocId);
        assertThat(response.getTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("search should return empty when no results")
    void search_shouldReturnEmptyWhenNoResults() {
        // Arrange
        SearchRequestDto request = SearchRequestDto.builder()
                .query("nonexistent query")
                .topK(10)
                .build();

        SearchPagedIterable emptyResults = emptySearchResults();

        List<Float> mockVector = List.of(0.1f, 0.2f, 0.3f);
        when(embeddingService.generateEmbedding("nonexistent query")).thenReturn(mockVector);
        when(azureSearchService.hybridSearch(eq("nonexistent query"), eq(mockVector), eq(30), isNull()))
                .thenReturn(emptyResults);
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());

        // Act
        SearchResponseDto response = searchService.search(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getResults()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);
        assertThat(response.getRagAnswer()).isNull();
    }

    @Test
    @DisplayName("search with RAG answer should include AI response")
    void search_withRagAnswer_shouldIncludeAiResponse() {
        // Arrange
        SearchRequestDto request = SearchRequestDto.builder()
                .query("tell me about case studies")
                .topK(10)
                .includeRagAnswer(true)
                .build();

        SearchPagedIterable mockResults = mockSearchResults(testDocumentId);

        List<Float> mockVector = List.of(0.1f, 0.2f, 0.3f);
        when(embeddingService.generateEmbedding("tell me about case studies")).thenReturn(mockVector);
        when(azureSearchService.hybridSearch(eq("tell me about case studies"), eq(mockVector), eq(30), isNull()))
                .thenReturn(mockResults);
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());

        // Mock the REST template call for Azure OpenAI
        String mockGptResponse = """
                {"choices":[{"message":{"content":"Based on the documents, here is the answer [1]."}}]}
                """;
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenReturn(mockGptResponse);

        // Act
        SearchResponseDto response = searchService.search(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getRagAnswer()).isEqualTo("Based on the documents, here is the answer [1].");
        assertThat(response.getSources()).hasSize(1);
        assertThat(response.getSources().get(0).getDocumentId()).isEqualTo(testDocumentId);
    }

    @Test
    @DisplayName("search RAG failure should return graceful fallback")
    void search_ragFailure_shouldReturnGracefulFallback() {
        // Arrange
        SearchRequestDto request = SearchRequestDto.builder()
                .query("tell me about case studies")
                .topK(10)
                .includeRagAnswer(true)
                .build();

        SearchPagedIterable mockResults = mockSearchResults(testDocumentId);

        List<Float> mockVector = List.of(0.1f, 0.2f, 0.3f);
        when(embeddingService.generateEmbedding("tell me about case studies")).thenReturn(mockVector);
        when(azureSearchService.hybridSearch(eq("tell me about case studies"), eq(mockVector), eq(30), isNull()))
                .thenReturn(mockResults);
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());

        // Mock the REST template to throw an exception
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("OpenAI service unavailable"));

        // Act
        SearchResponseDto response = searchService.search(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getRagAnswer()).isEqualTo("Unable to generate an AI answer at this time.");
        assertThat(response.getSources()).isEmpty();
    }
}
