package com.sedin.presales.infrastructure.ai.casestudy;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CaseStudyContentExtractor {
    @SystemMessage("You are an expert at analyzing case study presentations and extracting structured content. " +
            "Extract the content into sections and return valid JSON with the section keys provided. " +
            "For bullet list sections, return an array of strings. For text sections, return a single string. " +
            "If a section is not found in the text, use null for its value.")
    @UserMessage("Extract structured content from this case study text into the following sections: {{sectionKeys}}\n\nText:\n{{text}}")
    String extractSections(@V("text") String pptText, @V("sectionKeys") String sectionKeys);
}
