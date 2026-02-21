package com.sedin.presales.infrastructure.ai;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult;
import com.azure.ai.formrecognizer.documentanalysis.models.OperationResult;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIntelligenceServiceTest {

    @Mock
    private DocumentAnalysisClient documentAnalysisClient;

    @Mock
    private SyncPoller<OperationResult, AnalyzeResult> syncPoller;

    @Mock
    private AnalyzeResult analyzeResult;

    @InjectMocks
    private DocumentIntelligenceService documentIntelligenceService;

    @Test
    @DisplayName("extractText should return extracted content from document")
    void extractText_shouldReturnExtractedContent() {
        // Arrange
        String expectedText = "This is the extracted text from the document";
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());

        when(documentAnalysisClient.beginAnalyzeDocument(eq("prebuilt-read"), any(BinaryData.class)))
                .thenReturn(syncPoller);
        when(syncPoller.getFinalResult()).thenReturn(analyzeResult);
        when(analyzeResult.getContent()).thenReturn(expectedText);

        // Act
        String result = documentIntelligenceService.extractText(inputStream, "application/pdf");

        // Assert
        assertThat(result).isEqualTo(expectedText);
        verify(documentAnalysisClient).beginAnalyzeDocument(eq("prebuilt-read"), any(BinaryData.class));
        verify(syncPoller).getFinalResult();
    }

    @Test
    @DisplayName("extractText should return null when AnalyzeResult content is null")
    void extractText_shouldReturnNullContentGracefully() {
        // Arrange
        InputStream inputStream = new ByteArrayInputStream("some document bytes".getBytes());

        when(documentAnalysisClient.beginAnalyzeDocument(eq("prebuilt-read"), any(BinaryData.class)))
                .thenReturn(syncPoller);
        when(syncPoller.getFinalResult()).thenReturn(analyzeResult);
        when(analyzeResult.getContent()).thenReturn(null);

        // Act
        String result = documentIntelligenceService.extractText(inputStream, "application/pdf");

        // Assert
        assertThat(result).isNull();
        verify(documentAnalysisClient).beginAnalyzeDocument(eq("prebuilt-read"), any(BinaryData.class));
    }

    @Test
    @DisplayName("extractText should throw RuntimeException when InputStream throws IOException")
    void extractText_shouldThrowOnIOException() throws Exception {
        // Arrange
        InputStream mockStream = mock(InputStream.class);
        when(mockStream.readAllBytes()).thenThrow(new IOException("Read failed"));

        // Act & Assert
        assertThatThrownBy(() -> documentIntelligenceService.extractText(mockStream, "application/pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to read document");
    }

    @Test
    @DisplayName("extractText should handle empty document bytes")
    void extractText_shouldHandleEmptyDocument() {
        // Arrange
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        String expectedText = "";

        when(documentAnalysisClient.beginAnalyzeDocument(eq("prebuilt-read"), any(BinaryData.class)))
                .thenReturn(syncPoller);
        when(syncPoller.getFinalResult()).thenReturn(analyzeResult);
        when(analyzeResult.getContent()).thenReturn(expectedText);

        // Act
        String result = documentIntelligenceService.extractText(inputStream, "application/pdf");

        // Assert
        assertThat(result).isEmpty();
        verify(documentAnalysisClient).beginAnalyzeDocument(eq("prebuilt-read"), any(BinaryData.class));
    }
}
