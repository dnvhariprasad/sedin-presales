package com.sedin.presales.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private RestTemplate azureOpenAIRestTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EmbeddingService embeddingService;

    @Test
    @DisplayName("generateEmbedding should return correct float list for single text")
    void generateEmbedding_shouldReturnFloatList() throws Exception {
        // Arrange
        String inputText = "This is a test document about cloud computing";
        String responseJson = """
                {
                    "data": [
                        {
                            "embedding": [0.1, 0.2, 0.3, 0.4, 0.5]
                        }
                    ]
                }
                """;

        when(azureOpenAIRestTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseJson);

        ObjectMapper realMapper = new ObjectMapper();
        when(objectMapper.readTree(responseJson)).thenReturn(realMapper.readTree(responseJson));

        // Act
        List<Float> result = embeddingService.generateEmbedding(inputText);

        // Assert
        assertThat(result).hasSize(5);
        assertThat(result.get(0)).isEqualTo(0.1f);
        assertThat(result.get(1)).isEqualTo(0.2f);
        assertThat(result.get(2)).isEqualTo(0.3f);
        assertThat(result.get(3)).isEqualTo(0.4f);
        assertThat(result.get(4)).isEqualTo(0.5f);

        verify(azureOpenAIRestTemplate).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("generateEmbeddings should return multiple float lists for batch input")
    void generateEmbeddings_shouldReturnMultipleFloatLists() throws Exception {
        // Arrange
        List<String> inputTexts = List.of("First document", "Second document");
        String responseJson = """
                {
                    "data": [
                        {
                            "embedding": [0.1, 0.2, 0.3]
                        },
                        {
                            "embedding": [0.4, 0.5, 0.6]
                        }
                    ]
                }
                """;

        when(azureOpenAIRestTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseJson);

        ObjectMapper realMapper = new ObjectMapper();
        when(objectMapper.readTree(responseJson)).thenReturn(realMapper.readTree(responseJson));

        // Act
        List<List<Float>> result = embeddingService.generateEmbeddings(inputTexts);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(result.get(1)).containsExactly(0.4f, 0.5f, 0.6f);

        verify(azureOpenAIRestTemplate).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("generateEmbedding should throw RuntimeException on API failure")
    void generateEmbedding_shouldThrowOnFailure() {
        // Arrange
        when(azureOpenAIRestTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("API error"));

        // Act & Assert
        assertThatThrownBy(() -> embeddingService.generateEmbedding("test text"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate embedding");
    }

    @Test
    @DisplayName("generateEmbeddings should throw RuntimeException on API failure")
    void generateEmbeddings_shouldThrowOnFailure() {
        // Arrange
        when(azureOpenAIRestTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("API error"));

        // Act & Assert
        assertThatThrownBy(() -> embeddingService.generateEmbeddings(List.of("text1", "text2")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate batch embeddings");
    }
}
