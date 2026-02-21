package com.sedin.presales.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.CaseStudyAgentDto;
import com.sedin.presales.application.dto.CreateCaseStudyAgentRequest;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateCaseStudyAgentRequest;
import com.sedin.presales.application.exception.GlobalExceptionHandler;
import com.sedin.presales.application.service.CaseStudyAgentService;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CaseStudyAgentController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class CaseStudyAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CaseStudyAgentService caseStudyAgentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID testId = UUID.randomUUID();

    private CaseStudyAgentDto buildDto() {
        return CaseStudyAgentDto.builder()
                .id(testId)
                .name("Test Agent")
                .description("Test Description")
                .templateConfig(Map.of("version", "1.0"))
                .isActive(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void list_shouldReturn200() throws Exception {
        CaseStudyAgentDto dto = buildDto();
        PagedResponse<CaseStudyAgentDto> pagedResponse = PagedResponse.<CaseStudyAgentDto>builder()
                .content(List.of(dto))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(caseStudyAgentService.list(any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/admin/case-study-agents")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].name").value("Test Agent"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getById_shouldReturn200() throws Exception {
        CaseStudyAgentDto dto = buildDto();
        when(caseStudyAgentService.getById(testId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/admin/case-study-agents/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testId.toString()))
                .andExpect(jsonPath("$.data.name").value("Test Agent"));
    }

    @Test
    void getActive_shouldReturn200() throws Exception {
        CaseStudyAgentDto dto = buildDto();
        when(caseStudyAgentService.getActiveAgent()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/admin/case-study-agents/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Agent"));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        CreateCaseStudyAgentRequest request = CreateCaseStudyAgentRequest.builder()
                .name("Test Agent")
                .description("Test Description")
                .templateConfig(Map.of("version", "1.0"))
                .build();
        CaseStudyAgentDto dto = buildDto();

        when(caseStudyAgentService.create(any(CreateCaseStudyAgentRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/admin/case-study-agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Agent"))
                .andExpect(jsonPath("$.message").value("Created successfully"));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        UpdateCaseStudyAgentRequest request = UpdateCaseStudyAgentRequest.builder()
                .name("Updated Agent")
                .description("Updated Description")
                .build();
        CaseStudyAgentDto dto = buildDto();

        when(caseStudyAgentService.update(eq(testId), any(UpdateCaseStudyAgentRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/admin/case-study-agents/{id}", testId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Updated successfully"));
    }

    @Test
    void delete_shouldReturn200() throws Exception {
        doNothing().when(caseStudyAgentService).delete(testId);

        mockMvc.perform(delete("/api/v1/admin/case-study-agents/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Deleted successfully"));
    }

    @Test
    void activate_shouldReturn200() throws Exception {
        CaseStudyAgentDto dto = buildDto();
        when(caseStudyAgentService.activate(testId)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/admin/case-study-agents/{id}/activate", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Agent activated successfully"));
    }

    @Test
    void deactivate_shouldReturn200() throws Exception {
        CaseStudyAgentDto dto = buildDto();
        when(caseStudyAgentService.deactivate(testId)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/admin/case-study-agents/{id}/deactivate", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Agent deactivated successfully"));
    }
}
