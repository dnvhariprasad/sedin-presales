package com.sedin.presales.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummarizationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SummarizationService summarizationService;

    @BeforeEach
    void setUp() {
        summarizationService = new SummarizationService(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(summarizationService, "endpoint", "https://test.openai.azure.com");
        ReflectionTestUtils.setField(summarizationService, "apiKey", "test-key");
        ReflectionTestUtils.setField(summarizationService, "chatDeployment", "gpt-4o-mini");
    }

    @Test
    @DisplayName("summarize should return summary text from API response")
    void summarize_shouldReturnSummaryText() {
        // Arrange
        String responseJson = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "This is the AI-generated summary."
                            }
                        }
                    ]
                }
                """;

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseJson);

        // Act
        String result = summarizationService.summarize("Some document text content", "Test Document");

        // Assert
        assertThat(result).isEqualTo("This is the AI-generated summary.");
        verify(restTemplate).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("summarize should truncate text exceeding 100k characters")
    @SuppressWarnings("unchecked")
    void summarize_shouldTruncateLongText() {
        // Arrange
        String longText = "A".repeat(150_000);
        String responseJson = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "Summary of truncated document."
                            }
                        }
                    ]
                }
                """;

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseJson);

        // Act
        String result = summarizationService.summarize(longText, "Long Document");

        // Assert
        assertThat(result).isEqualTo("Summary of truncated document.");

        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), requestCaptor.capture(), eq(String.class));

        HttpEntity<Map<String, Object>> captured = requestCaptor.getValue();
        Map<String, Object> body = captured.getBody();
        assertThat(body).isNotNull();

        // The user message content should contain the truncation note
        var messages = (java.util.List<Map<String, Object>>) body.get("messages");
        String userContent = (String) messages.get(1).get("content");
        assertThat(userContent).contains("[Note: Document was truncated due to length");
        // Should not contain the full 150k text
        assertThat(userContent.length()).isLessThan(150_000);
    }

    @Test
    @DisplayName("summarize should throw RuntimeException on API error")
    void summarize_shouldThrowOnApiError() {
        // Arrange
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // Act & Assert
        assertThatThrownBy(() -> summarizationService.summarize("Some text", "Test Doc"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate summary for document: Test Doc");
    }

    @Test
    @DisplayName("summarize should include document title in prompt")
    @SuppressWarnings("unchecked")
    void summarize_shouldIncludeDocumentTitleInPrompt() {
        // Arrange
        String responseJson = """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "A brief summary."
                            }
                        }
                    ]
                }
                """;

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseJson);

        // Act
        summarizationService.summarize("Document content here", "Cloud Migration Case Study");

        // Assert
        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), requestCaptor.capture(), eq(String.class));

        HttpEntity<Map<String, Object>> captured = requestCaptor.getValue();
        Map<String, Object> body = captured.getBody();
        assertThat(body).isNotNull();

        var messages = (java.util.List<Map<String, Object>>) body.get("messages");
        String userContent = (String) messages.get(1).get("content");
        assertThat(userContent).contains("Cloud Migration Case Study");

        // Verify api-key header is set
        assertThat(captured.getHeaders().getFirst("api-key")).isEqualTo("test-key");
    }
}
