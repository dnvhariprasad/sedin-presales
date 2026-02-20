package com.sedin.presales.application.service;

import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.models.SearchResult;
import com.azure.search.documents.util.SearchPagedIterable;
import com.sedin.presales.application.dto.*;
import com.sedin.presales.config.CurrentUserService;
import com.sedin.presales.config.UserPrincipal;
import com.sedin.presales.domain.enums.Permission;
import com.sedin.presales.domain.enums.ResourceType;
import com.sedin.presales.infrastructure.ai.EmbeddingService;
import com.sedin.presales.infrastructure.search.AzureSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchService {

    private static final String API_VERSION = "2024-02-01";

    private final EmbeddingService embeddingService;
    private final AzureSearchService azureSearchService;
    private final AclService aclService;
    private final CurrentUserService currentUserService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${azure.openai.endpoint}")
    private String openAiEndpoint;

    @Value("${azure.openai.api-key}")
    private String openAiApiKey;

    @Value("${azure.openai.chat-deployment}")
    private String chatDeployment;

    public SearchService(EmbeddingService embeddingService,
                         AzureSearchService azureSearchService,
                         AclService aclService,
                         CurrentUserService currentUserService,
                         RestTemplate azureOpenAIRestTemplate,
                         ObjectMapper objectMapper) {
        this.embeddingService = embeddingService;
        this.azureSearchService = azureSearchService;
        this.aclService = aclService;
        this.currentUserService = currentUserService;
        this.restTemplate = azureOpenAIRestTemplate;
        this.objectMapper = objectMapper;
    }

    public SearchResponseDto search(SearchRequestDto request) {
        log.info("Performing search for query: '{}'", request.getQuery());

        // Generate query embedding
        List<Float> queryVector = embeddingService.generateEmbedding(request.getQuery());

        // Fetch 3x topK to allow ACL filtering headroom
        int fetchSize = request.getTopK() * 3;

        // Execute hybrid search
        SearchPagedIterable searchResults = azureSearchService.hybridSearch(
                request.getQuery(), queryVector, fetchSize, buildODataFilter(request));

        // Collect results
        List<SearchResultDto> allResults = new ArrayList<>();
        for (SearchResult result : searchResults) {
            SearchDocument doc = result.getDocument(SearchDocument.class);
            allResults.add(mapToSearchResultDto(doc, result.getScore()));
        }

        // ACL post-filter
        List<SearchResultDto> filteredResults = applyAclFilter(allResults);

        // Trim to topK
        List<SearchResultDto> finalResults = filteredResults.stream()
                .limit(request.getTopK())
                .collect(Collectors.toList());

        // Build response
        SearchResponseDto.SearchResponseDtoBuilder responseBuilder = SearchResponseDto.builder()
                .results(finalResults)
                .totalCount(finalResults.size())
                .query(request.getQuery());

        // If RAG answer requested
        if (request.isIncludeRagAnswer() && !finalResults.isEmpty()) {
            RagResult ragResult = generateRagAnswer(request.getQuery(), finalResults);
            responseBuilder.ragAnswer(ragResult.answer);
            responseBuilder.sources(ragResult.sources);
        }

        return responseBuilder.build();
    }

    private List<SearchResultDto> applyAclFilter(List<SearchResultDto> results) {
        UserPrincipal currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUser.getRole());

        if (isAdmin) {
            return results;
        }

        Set<UUID> accessibleIds = aclService.getAccessibleResourceIds(
                UUID.fromString(currentUser.getUserId()), ResourceType.DOCUMENT, Permission.READ);

        return results.stream()
                .filter(r -> accessibleIds.contains(r.getDocumentId()))
                .collect(Collectors.toList());
    }

    private String buildODataFilter(SearchRequestDto request) {
        // Build OData filter from request optional fields
        // Note: these filter on the search index string fields, not DB UUIDs
        // For simplicity, no filters applied here — ACL post-filtering handles access control
        return null;
    }

    private SearchResultDto mapToSearchResultDto(SearchDocument doc, double score) {
        String documentIdStr = (String) doc.get("documentId");
        UUID documentId = documentIdStr != null ? UUID.fromString(documentIdStr) : null;

        @SuppressWarnings("unchecked")
        List<String> technologies = (List<String>) doc.get("technologies");

        return SearchResultDto.builder()
                .documentId(documentId)
                .title((String) doc.get("title"))
                .customerName((String) doc.get("customerName"))
                .snippet(truncateSnippet((String) doc.get("content"), 200))
                .score(score)
                .domain((String) doc.get("domain"))
                .industry((String) doc.get("industry"))
                .documentType((String) doc.get("documentType"))
                .technologies(technologies)
                .build();
    }

    private String truncateSnippet(String content, int maxLength) {
        if (content == null) return null;
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    private RagResult generateRagAnswer(String query, List<SearchResultDto> results) {
        log.info("Generating RAG answer for query: '{}'", query);

        try {
            // Build context from search results
            StringBuilder context = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                SearchResultDto r = results.get(i);
                context.append(String.format("[%d] Title: %s, Customer: %s\n%s\n\n",
                        i + 1, r.getTitle(), r.getCustomerName(), r.getSnippet()));
            }

            String systemPrompt = "You are a helpful assistant for a pre-sales team. " +
                    "Answer the user's question based ONLY on the provided document excerpts. " +
                    "Cite sources using [1], [2], etc. If the documents don't contain relevant information, say so.";

            String userPrompt = String.format("Documents:\n%s\n\nQuestion: %s", context, query);

            // Call Azure OpenAI chat API
            String url = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
                    openAiEndpoint, chatDeployment, API_VERSION);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", openAiApiKey);

            Map<String, Object> requestBody = Map.of(
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.3,
                    "max_tokens", 500
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            String responseJson = restTemplate.postForObject(url, request, String.class);

            JsonNode responseNode = objectMapper.readTree(responseJson);
            String answer = responseNode.path("choices").get(0).path("message").path("content").asText();

            // Build source citations
            List<SourceCitationDto> sources = results.stream()
                    .map(r -> SourceCitationDto.builder()
                            .documentId(r.getDocumentId())
                            .title(r.getTitle())
                            .snippet(r.getSnippet())
                            .build())
                    .collect(Collectors.toList());

            return new RagResult(answer, sources);

        } catch (Exception e) {
            log.error("Failed to generate RAG answer", e);
            return new RagResult("Unable to generate an AI answer at this time.", List.of());
        }
    }

    private record RagResult(String answer, List<SourceCitationDto> sources) {}
}
