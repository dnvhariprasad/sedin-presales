package com.sedin.presales.api.controller;

import com.sedin.presales.application.dto.SummaryResponseDto;
import com.sedin.presales.application.exception.GlobalExceptionHandler;
import com.sedin.presales.application.service.SummaryService;
import com.sedin.presales.config.JwtTokenProvider;
import com.sedin.presales.config.TestSecurityConfig;
import com.sedin.presales.domain.enums.RenditionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SummaryController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SummaryService summaryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID documentId = UUID.randomUUID();

    @Test
    @DisplayName("GET /api/v1/documents/{id}/summary should return 200 with completed summary")
    void getSummary_shouldReturn200() throws Exception {
        SummaryResponseDto dto = SummaryResponseDto.builder()
                .documentId(documentId)
                .status(RenditionStatus.COMPLETED)
                .summary("This is the summary text.")
                .message("Summary is available")
                .build();

        when(summaryService.getSummaryStatus(documentId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/documents/{documentId}/summary", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.summary").value("This is the summary text."))
                .andExpect(jsonPath("$.data.message").value("Summary is available"));
    }

    @Test
    @DisplayName("POST /api/v1/documents/{id}/summary/regenerate should return 202")
    void regenerateSummary_shouldReturn202() throws Exception {
        SummaryResponseDto dto = SummaryResponseDto.builder()
                .documentId(documentId)
                .status(RenditionStatus.PENDING)
                .message("Summary regeneration has been initiated")
                .build();

        when(summaryService.regenerateSummary(documentId)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/documents/{documentId}/summary/regenerate", documentId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.message").value("Summary regeneration has been initiated"));
    }

    @Test
    @DisplayName("GET /api/v1/documents/{id}/summary should return pending status")
    void getSummary_shouldReturnPendingStatus() throws Exception {
        SummaryResponseDto dto = SummaryResponseDto.builder()
                .documentId(documentId)
                .status(RenditionStatus.PENDING)
                .message("Summary generation has been initiated")
                .build();

        when(summaryService.getSummaryStatus(documentId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/documents/{documentId}/summary", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.summary").doesNotExist())
                .andExpect(jsonPath("$.data.message").value("Summary generation has been initiated"));
    }
}
