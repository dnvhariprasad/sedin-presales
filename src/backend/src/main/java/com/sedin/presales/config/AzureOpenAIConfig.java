package com.sedin.presales.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AzureOpenAIConfig {

    @Bean
    public RestTemplate azureOpenAIRestTemplate() {
        return new RestTemplate();
    }
}
