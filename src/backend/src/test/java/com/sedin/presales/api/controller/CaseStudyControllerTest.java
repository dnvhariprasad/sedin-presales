package com.sedin.presales.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.CaseStudyWizardRequest;
import com.sedin.presales.application.dto.CaseStudyWizardResponseDto;
import com.sedin.presales.application.dto.ValidationResultDto;
import com.sedin.presales.application.exception.GlobalExceptionHandler;
import com.sedin.presales.application.service.CaseStudyGenerationService;
import com.sedin.presales.application.service.CaseStudyValidationService;
import com.sedin.presales.config.JwtTokenProvider;
import com.sedin.presales.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CaseStudyController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class CaseStudyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CaseStudyGenerationService generationService;

    @MockitoBean
    private CaseStudyValidationService validationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID testVersionId = UUID.randomUUID();

    @Test
    void generate_shouldReturn201() throws Exception {
        CaseStudyWizardRequest request = CaseStudyWizardRequest.builder()
                .title("Test Case Study")
                .customerName("Acme Corp")
                .customerOverview("Acme Corp overview")
                .challenges(List.of("Challenge 1"))
                .solution("Solution description")
                .technologies(List.of("Java"))
                .results(List.of("50% improvement"))
                .enhanceWithAi(false)
                .build();

        CaseStudyWizardResponseDto response = CaseStudyWizardResponseDto.builder()
                .documentId(UUID.randomUUID())
                .documentVersionId(testVersionId)
                .message("Case study generated successfully")
                .build();

        when(generationService.generateCaseStudy(any(CaseStudyWizardRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/case-studies/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentVersionId").value(testVersionId.toString()))
                .andExpect(jsonPath("$.message").value("Case study generated successfully"));
    }

    @Test
    void generate_shouldReturn400WhenTitleBlank() throws Exception {
        CaseStudyWizardRequest request = CaseStudyWizardRequest.builder()
                .title("")
                .customerOverview("Overview")
                .challenges(List.of("Challenge 1"))
                .solution("Solution")
                .results(List.of("Result"))
                .build();

        mockMvc.perform(post("/api/v1/case-studies/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getValidation_shouldReturn200() throws Exception {
        ValidationResultDto dto = ValidationResultDto.builder()
                .id(UUID.randomUUID())
                .documentVersionId(testVersionId)
                .agentId(UUID.randomUUID())
                .isValid(true)
                .createdAt(Instant.now())
                .build();

        when(validationService.getValidationResult(testVersionId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/case-studies/{documentVersionId}/validation", testVersionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentVersionId").value(testVersionId.toString()));
    }

    @Test
    void revalidate_shouldReturn202() throws Exception {
        doNothing().when(validationService).validateCaseStudy(testVersionId);

        mockMvc.perform(post("/api/v1/case-studies/{documentVersionId}/revalidate", testVersionId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Revalidation triggered"));
    }
}
