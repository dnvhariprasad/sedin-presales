package com.sedin.presales.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmbeddingService {

    private static final String API_VERSION = "2024-02-01";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${azure.openai.endpoint}")
    private String endpoint;

    @Value("${azure.openai.api-key}")
    private String apiKey;

    @Value("${azure.openai.embedding-deployment}")
    private String embeddingDeployment;

    public EmbeddingService(RestTemplate azureOpenAIRestTemplate, ObjectMapper objectMapper) {
        this.restTemplate = azureOpenAIRestTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Generate an embedding vector for a single text input.
     *
     * @param text the text to embed
     * @return a list of 1536 floats representing the embedding vector
     */
    public List<Float> generateEmbedding(String text) {
        log.info("Generating embedding for text of length: {} chars", text.length());

        try {
            String url = String.format("%s/openai/deployments/%s/embeddings?api-version=%s",
                    endpoint, embeddingDeployment, API_VERSION);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            Map<String, Object> requestBody = Map.of("input", text);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String responseJson = restTemplate.postForObject(url, request, String.class);
            JsonNode responseNode = objectMapper.readTree(responseJson);

            JsonNode embeddingArray = responseNode.path("data").get(0).path("embedding");
            List<Float> embedding = new ArrayList<>();
            for (JsonNode value : embeddingArray) {
                embedding.add(value.floatValue());
            }

            log.info("Embedding generated successfully, dimensions: {}", embedding.size());
            return embedding;

        } catch (Exception e) {
            log.error("Failed to generate embedding for text of length: {}", text.length(), e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    /**
     * Generate embedding vectors for multiple text inputs in a single API call.
     *
     * @param texts the list of texts to embed
     * @return a list of embedding vectors, one per input text
     */
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        log.info("Generating embeddings for {} texts", texts.size());

        try {
            String url = String.format("%s/openai/deployments/%s/embeddings?api-version=%s",
                    endpoint, embeddingDeployment, API_VERSION);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            Map<String, Object> requestBody = Map.of("input", texts);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String responseJson = restTemplate.postForObject(url, request, String.class);
            JsonNode responseNode = objectMapper.readTree(responseJson);

            JsonNode dataArray = responseNode.path("data");
            List<List<Float>> embeddings = new ArrayList<>();
            for (JsonNode dataItem : dataArray) {
                JsonNode embeddingArray = dataItem.path("embedding");
                List<Float> embedding = new ArrayList<>();
                for (JsonNode value : embeddingArray) {
                    embedding.add(value.floatValue());
                }
                embeddings.add(embedding);
            }

            log.info("Batch embeddings generated successfully, count: {}", embeddings.size());
            return embeddings;

        } catch (Exception e) {
            log.error("Failed to generate batch embeddings for {} texts", texts.size(), e);
            throw new RuntimeException("Failed to generate batch embeddings", e);
        }
    }
}
