package com.sedin.presales.config;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureSearchConfig {

    @Value("${azure.search.endpoint}")
    private String endpoint;

    @Value("${azure.search.api-key}")
    private String apiKey;

    @Value("${azure.search.index-name}")
    private String indexName;

    @Bean
    public SearchClient searchClient() {
        return new SearchClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .indexName(indexName)
                .buildClient();
    }

    @Bean
    public SearchIndexClient searchIndexClient() {
        return new SearchIndexClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .buildClient();
    }
}
