package com.sedin.presales.infrastructure.ai.casestudy;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CaseStudyContentValidator {
    @SystemMessage("You are a document quality validator for business case study presentations. " +
            "Analyze the extracted content against the template rules and return a JSON object with: " +
            "'issues' (array of objects with 'section', 'severity' (ERROR/WARNING), 'message'), " +
            "and 'overallScore' (0.0 to 1.0 where 1.0 is perfect).")
    @UserMessage("Validate this case study content against the template rules.\n\nContent:\n{{content}}\n\nRules:\n{{rules}}")
    String validateContent(@V("content") String extractedContent, @V("rules") String templateRulesJson);
}
