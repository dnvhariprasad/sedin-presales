package com.sedin.presales.infrastructure.ai.casestudy;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CaseStudyContentEnhancer {
    @SystemMessage("You are a professional B2B copywriter specializing in technology case studies. " +
            "Enhance the provided case study content to be more professional, concise, and impactful. " +
            "Preserve all factual information. Return the enhanced content as a JSON object with the same section keys.")
    @UserMessage("Enhance this case study content while preserving factual accuracy:\n{{content}}")
    String enhanceContent(@V("content") String rawContent);
}
