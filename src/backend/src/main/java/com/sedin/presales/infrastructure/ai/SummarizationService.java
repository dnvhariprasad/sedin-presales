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

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SummarizationService {

    private static final int MAX_TEXT_LENGTH = 100_000;
    private static final String API_VERSION = "2024-02-01";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${azure.openai.endpoint}")
    private String endpoint;

    @Value("${azure.openai.api-key}")
    private String apiKey;

    @Value("${azure.openai.chat-deployment}")
    private String chatDeployment;

    public SummarizationService(RestTemplate azureOpenAIRestTemplate, ObjectMapper objectMapper) {
        this.restTemplate = azureOpenAIRestTemplate;
        this.objectMapper = objectMapper;
    }

    public String summarize(String extractedText, String documentTitle) {
        log.info("Starting summarization for document: '{}', text length: {} chars", documentTitle, extractedText.length());

        String textToSummarize = extractedText;
        if (extractedText.length() > MAX_TEXT_LENGTH) {
            log.warn("Text exceeds {} chars, truncating from {} chars", MAX_TEXT_LENGTH, extractedText.length());
            textToSummarize = extractedText.substring(0, MAX_TEXT_LENGTH)
                    + "\n\n[Note: Document was truncated due to length. Summary is based on the first "
                    + MAX_TEXT_LENGTH + " characters.]";
        }

        try {
            String url = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
                    endpoint, chatDeployment, API_VERSION);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            Map<String, Object> systemMessage = Map.of(
                    "role", "system",
                    "content", "You are an AI assistant that creates concise, professional summaries of business documents. "
                            + "Focus on key points, technologies used, client industry, challenges, solutions, and outcomes."
            );

            Map<String, Object> userMessage = Map.of(
                    "role", "user",
                    "content", String.format("Summarize the following document titled '%s':\n\n%s", documentTitle, textToSummarize)
            );

            Map<String, Object> requestBody = Map.of(
                    "messages", List.of(systemMessage, userMessage),
                    "temperature", 0.3,
                    "max_tokens", 1000
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String responseJson = restTemplate.postForObject(url, request, String.class);
            JsonNode responseNode = objectMapper.readTree(responseJson);

            String summary = responseNode
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            log.info("Summarization completed for document: '{}', summary length: {} chars", documentTitle, summary.length());
            return summary;

        } catch (Exception e) {
            log.error("Failed to summarize document: '{}'", documentTitle, e);
            throw new RuntimeException("Failed to generate summary for document: " + documentTitle, e);
        }
    }
}
