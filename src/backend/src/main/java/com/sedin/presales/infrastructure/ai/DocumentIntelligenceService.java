package com.sedin.presales.infrastructure.ai;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult;
import com.azure.ai.formrecognizer.documentanalysis.models.OperationResult;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class DocumentIntelligenceService {

    private static final String MODEL_ID = "prebuilt-read";

    private final DocumentAnalysisClient documentAnalysisClient;

    public DocumentIntelligenceService(DocumentAnalysisClient documentAnalysisClient) {
        this.documentAnalysisClient = documentAnalysisClient;
    }

    public String extractText(InputStream document, String contentType) {
        log.info("Starting text extraction using model '{}', contentType: {}", MODEL_ID, contentType);

        try {
            byte[] documentBytes = document.readAllBytes();
            BinaryData binaryData = BinaryData.fromBytes(documentBytes);

            SyncPoller<OperationResult, AnalyzeResult> poller =
                    documentAnalysisClient.beginAnalyzeDocument(MODEL_ID, binaryData);

            AnalyzeResult result = poller.getFinalResult();

            String extractedText = result.getContent();
            log.info("Text extraction completed. Extracted {} characters", extractedText != null ? extractedText.length() : 0);

            return extractedText;
        } catch (IOException e) {
            log.error("Failed to read document input stream for text extraction", e);
            throw new RuntimeException("Failed to read document for text extraction", e);
        }
    }
}
