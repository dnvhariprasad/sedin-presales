package com.sedin.presales.infrastructure.search;

import com.azure.core.util.Context;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.models.HnswAlgorithmConfiguration;
import com.azure.search.documents.indexes.models.SearchField;
import com.azure.search.documents.indexes.models.SearchFieldDataType;
import com.azure.search.documents.indexes.models.SearchIndex;
import com.azure.search.documents.indexes.models.VectorSearch;
import com.azure.search.documents.indexes.models.VectorSearchAlgorithmConfiguration;
import com.azure.search.documents.indexes.models.VectorSearchProfile;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchResult;
import com.azure.search.documents.models.VectorSearchOptions;
import com.azure.search.documents.models.VectorizedQuery;
import com.azure.search.documents.util.SearchPagedIterable;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AzureSearchService {

    private final SearchClient searchClient;
    private final SearchIndexClient searchIndexClient;

    @Value("${azure.search.index-name}")
    private String indexName;

    @Value("${azure.search.auto-create-index:true}")
    private boolean autoCreateIndex;

    public AzureSearchService(SearchClient searchClient, SearchIndexClient searchIndexClient) {
        this.searchClient = searchClient;
        this.searchIndexClient = searchIndexClient;
    }

    @PostConstruct
    public void createOrUpdateIndex() {
        if (!autoCreateIndex) {
            log.info("Auto-create index is disabled, skipping index creation");
            return;
        }

        log.info("Creating or updating search index: {}", indexName);

        try {
            List<SearchField> fields = Arrays.asList(
                    new SearchField("id", SearchFieldDataType.STRING)
                            .setKey(true)
                            .setFilterable(true),
                    new SearchField("documentId", SearchFieldDataType.STRING)
                            .setFilterable(true),
                    new SearchField("versionId", SearchFieldDataType.STRING),
                    new SearchField("chunkIndex", SearchFieldDataType.INT32)
                            .setSortable(true),
                    new SearchField("title", SearchFieldDataType.STRING)
                            .setSearchable(true),
                    new SearchField("content", SearchFieldDataType.STRING)
                            .setSearchable(true),
                    new SearchField("contentVector", SearchFieldDataType.collection(SearchFieldDataType.SINGLE))
                            .setSearchable(true)
                            .setVectorSearchDimensions(1536)
                            .setVectorSearchProfileName("default-vector-profile"),
                    new SearchField("domain", SearchFieldDataType.STRING)
                            .setFilterable(true)
                            .setFacetable(true),
                    new SearchField("industry", SearchFieldDataType.STRING)
                            .setFilterable(true)
                            .setFacetable(true),
                    new SearchField("technologies", SearchFieldDataType.collection(SearchFieldDataType.STRING))
                            .setFilterable(true),
                    new SearchField("customerName", SearchFieldDataType.STRING)
                            .setFilterable(true)
                            .setSearchable(true),
                    new SearchField("businessUnit", SearchFieldDataType.STRING)
                            .setFilterable(true),
                    new SearchField("sbu", SearchFieldDataType.STRING)
                            .setFilterable(true),
                    new SearchField("documentType", SearchFieldDataType.STRING)
                            .setFilterable(true),
                    new SearchField("createdDate", SearchFieldDataType.DATE_TIME_OFFSET)
                            .setSortable(true)
            );

            VectorSearch vectorSearch = new VectorSearch()
                    .setAlgorithms(List.of(
                            new HnswAlgorithmConfiguration("default-hnsw")
                    ))
                    .setProfiles(List.of(
                            new VectorSearchProfile("default-vector-profile", "default-hnsw")
                    ));

            SearchIndex index = new SearchIndex(indexName)
                    .setFields(fields)
                    .setVectorSearch(vectorSearch);

            searchIndexClient.createOrUpdateIndex(index);

            log.info("Search index '{}' created/updated successfully", indexName);

        } catch (Exception e) {
            log.error("Failed to create or update search index: {}", indexName, e);
            throw new RuntimeException("Failed to create or update search index: " + indexName, e);
        }
    }

    /**
     * Upload documents (chunks) to the search index.
     *
     * @param documents the search documents to upload
     */
    public void uploadDocuments(List<SearchDocument> documents) {
        log.info("Uploading {} documents to search index: {}", documents.size(), indexName);

        try {
            searchClient.uploadDocuments(documents);
            log.info("Successfully uploaded {} documents to index: {}", documents.size(), indexName);
        } catch (Exception e) {
            log.error("Failed to upload documents to search index: {}", indexName, e);
            throw new RuntimeException("Failed to upload documents to search index", e);
        }
    }

    /**
     * Delete all chunks for a given document from the search index.
     *
     * @param documentId the document ID whose chunks should be deleted
     */
    public void deleteDocumentChunks(String documentId) {
        log.info("Deleting chunks for document: {} from index: {}", documentId, indexName);

        try {
            String filter = String.format("documentId eq '%s'", documentId);
            SearchOptions options = new SearchOptions()
                    .setFilter(filter)
                    .setSelect("id");

            SearchPagedIterable results = searchClient.search("*", options, Context.NONE);

            List<SearchDocument> documentsToDelete = new ArrayList<>();
            for (SearchResult result : results) {
                SearchDocument doc = new SearchDocument();
                doc.put("id", result.getDocument(SearchDocument.class).get("id"));
                documentsToDelete.add(doc);
            }

            if (!documentsToDelete.isEmpty()) {
                searchClient.deleteDocuments(documentsToDelete);
                log.info("Deleted {} chunks for document: {}", documentsToDelete.size(), documentId);
            } else {
                log.info("No chunks found for document: {}", documentId);
            }

        } catch (Exception e) {
            log.error("Failed to delete chunks for document: {}", documentId, e);
            throw new RuntimeException("Failed to delete document chunks for: " + documentId, e);
        }
    }

    /**
     * Perform a hybrid search (text + vector) against the search index.
     *
     * @param queryText   the text query for keyword search
     * @param queryVector the embedding vector for vector search
     * @param topK        the number of results to return
     * @param oDataFilter optional OData filter expression (can be null)
     * @return the search results
     */
    public SearchPagedIterable hybridSearch(String queryText, List<Float> queryVector, int topK, String oDataFilter) {
        log.info("Performing hybrid search, queryText length: {}, topK: {}, filter: {}",
                queryText != null ? queryText.length() : 0, topK, oDataFilter);

        try {
            SearchOptions options = new SearchOptions()
                    .setTop(topK)
                    .setSelect("documentId", "title", "content", "customerName", "domain",
                            "industry", "documentType", "chunkIndex", "technologies")
                    .setVectorSearchOptions(new VectorSearchOptions()
                            .setQueries(List.of(new VectorizedQuery(queryVector)
                                    .setKNearestNeighborsCount(topK)
                                    .setFields("contentVector"))));

            if (oDataFilter != null && !oDataFilter.isBlank()) {
                options.setFilter(oDataFilter);
            }

            SearchPagedIterable results = searchClient.search(queryText, options, Context.NONE);

            log.info("Hybrid search completed successfully");
            return results;

        } catch (Exception e) {
            log.error("Failed to perform hybrid search", e);
            throw new RuntimeException("Failed to perform hybrid search", e);
        }
    }
}
