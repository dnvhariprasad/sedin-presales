package com.sedin.presales.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.SearchRequestDto;
import com.sedin.presales.application.dto.SearchResponseDto;
import com.sedin.presales.application.dto.SearchResultDto;
import com.sedin.presales.application.dto.SourceCitationDto;
import com.sedin.presales.application.exception.GlobalExceptionHandler;
import com.sedin.presales.application.service.SearchService;
import com.sedin.presales.config.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SearchController.class,
        excludeAutoConfiguration = {OAuth2ResourceServerAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SearchService searchService;

    private final UUID testDocumentId = UUID.randomUUID();

    @Test
    @DisplayName("POST /api/v1/search should return 200 with results")
    void search_shouldReturn200WithResults() throws Exception {
        // Arrange
        SearchRequestDto request = SearchRequestDto.builder()
                .query("case study")
                .topK(10)
                .build();

        SearchResultDto resultDto = SearchResultDto.builder()
                .documentId(testDocumentId)
                .title("Test Case Study")
                .customerName("Acme Corp")
                .snippet("This is a case study snippet...")
                .score(0.95)
                .domain("Technology")
                .industry("Finance")
                .documentType("Case Study")
                .technologies(List.of("Java", "Spring"))
                .build();

        SearchResponseDto responseDto = SearchResponseDto.builder()
                .results(List.of(resultDto))
                .totalCount(1)
                .query("case study")
                .build();

        when(searchService.search(any(SearchRequestDto.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results").isArray())
                .andExpect(jsonPath("$.data.results[0].documentId").value(testDocumentId.toString()))
                .andExpect(jsonPath("$.data.results[0].title").value("Test Case Study"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.query").value("case study"));
    }

    @Test
    @DisplayName("POST /api/v1/search should return 200 with empty results")
    void search_shouldReturn200WithEmptyResults() throws Exception {
        // Arrange
        SearchRequestDto request = SearchRequestDto.builder()
                .query("nonexistent")
                .topK(10)
                .build();

        SearchResponseDto responseDto = SearchResponseDto.builder()
                .results(List.of())
                .totalCount(0)
                .query("nonexistent")
                .build();

        when(searchService.search(any(SearchRequestDto.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results").isEmpty())
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/search with RAG answer should return 200")
    void search_withRagAnswer_shouldReturn200() throws Exception {
        // Arrange
        SearchRequestDto request = SearchRequestDto.builder()
                .query("tell me about case studies")
                .topK(10)
                .includeRagAnswer(true)
                .build();

        SearchResultDto resultDto = SearchResultDto.builder()
                .documentId(testDocumentId)
                .title("Test Case Study")
                .customerName("Acme Corp")
                .snippet("This is a case study snippet...")
                .score(0.95)
                .domain("Technology")
                .industry("Finance")
                .documentType("Case Study")
                .technologies(List.of("Java", "Spring"))
                .build();

        SourceCitationDto sourceDto = SourceCitationDto.builder()
                .documentId(testDocumentId)
                .title("Test Case Study")
                .snippet("This is a case study snippet...")
                .build();

        SearchResponseDto responseDto = SearchResponseDto.builder()
                .results(List.of(resultDto))
                .totalCount(1)
                .query("tell me about case studies")
                .ragAnswer("Based on the documents, here is the answer [1].")
                .sources(List.of(sourceDto))
                .build();

        when(searchService.search(any(SearchRequestDto.class))).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results").isArray())
                .andExpect(jsonPath("$.data.results").isNotEmpty())
                .andExpect(jsonPath("$.data.ragAnswer").value("Based on the documents, here is the answer [1]."))
                .andExpect(jsonPath("$.data.sources").isArray())
                .andExpect(jsonPath("$.data.sources[0].documentId").value(testDocumentId.toString()))
                .andExpect(jsonPath("$.data.sources[0].title").value("Test Case Study"));
    }
}
