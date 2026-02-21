package com.sedin.presales.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.service.AiServices;
import com.sedin.presales.infrastructure.ai.casestudy.CaseStudyContentExtractor;
import com.sedin.presales.infrastructure.ai.casestudy.CaseStudyContentValidator;
import com.sedin.presales.infrastructure.ai.casestudy.CaseStudyContentEnhancer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatLanguageModel caseStudyChatModel(
            @Value("${azure.openai.endpoint}") String endpoint,
            @Value("${azure.openai.api-key}") String apiKey,
            @Value("${azure.openai.chat-deployment}") String deployment) {
        return AzureOpenAiChatModel.builder()
                .endpoint(endpoint)
                .apiKey(apiKey)
                .deploymentName(deployment)
                .temperature(0.3)
                .maxTokens(2000)
                .build();
    }

    @Bean
    public CaseStudyContentExtractor caseStudyContentExtractor(ChatLanguageModel caseStudyChatModel) {
        return AiServices.create(CaseStudyContentExtractor.class, caseStudyChatModel);
    }

    @Bean
    public CaseStudyContentValidator caseStudyContentValidator(ChatLanguageModel caseStudyChatModel) {
        return AiServices.create(CaseStudyContentValidator.class, caseStudyChatModel);
    }

    @Bean
    public CaseStudyContentEnhancer caseStudyContentEnhancer(ChatLanguageModel caseStudyChatModel) {
        return AiServices.create(CaseStudyContentEnhancer.class, caseStudyChatModel);
    }
}
