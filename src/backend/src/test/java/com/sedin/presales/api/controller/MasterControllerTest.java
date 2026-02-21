package com.sedin.presales.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.CreateMasterRequest;
import com.sedin.presales.application.dto.MasterDto;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateMasterRequest;
import com.sedin.presales.application.exception.GlobalExceptionHandler;
import com.sedin.presales.application.exception.ResourceNotFoundException;
import com.sedin.presales.application.service.MasterService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MasterController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class MasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MasterService masterService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID testId = UUID.randomUUID();

    private MasterDto buildMasterDto() {
        return MasterDto.builder()
                .id(testId)
                .name("Test Domain")
                .description("Test Description")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void list_shouldReturnPagedResponse() throws Exception {
        MasterDto dto = buildMasterDto();
        PagedResponse<MasterDto> pagedResponse = PagedResponse.<MasterDto>builder()
                .content(List.of(dto))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(masterService.list(eq("domains"), any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/masters/domains")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].name").value("Test Domain"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void getById_shouldReturnMasterDto() throws Exception {
        MasterDto dto = buildMasterDto();
        when(masterService.getById("domains", testId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/masters/domains/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testId.toString()))
                .andExpect(jsonPath("$.data.name").value("Test Domain"));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(masterService.getById("domains", testId))
                .thenThrow(new ResourceNotFoundException("domains", "id", testId));

        mockMvc.perform(get("/api/v1/masters/domains/{id}", testId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        CreateMasterRequest request = CreateMasterRequest.builder()
                .name("Test")
                .description("Desc")
                .build();
        MasterDto dto = buildMasterDto();

        when(masterService.create(eq("domains"), any(CreateMasterRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/masters/domains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Domain"))
                .andExpect(jsonPath("$.message").value("Created successfully"));
    }

    @Test
    void create_shouldReturn400WhenNameBlank() throws Exception {
        CreateMasterRequest request = CreateMasterRequest.builder()
                .name("")
                .description("Desc")
                .build();

        mockMvc.perform(post("/api/v1/masters/domains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        UpdateMasterRequest request = UpdateMasterRequest.builder()
                .name("Updated")
                .description("Updated Desc")
                .build();
        MasterDto dto = buildMasterDto();

        when(masterService.update(eq("domains"), eq(testId), any(UpdateMasterRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/masters/domains/{id}", testId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Updated successfully"));
    }

    @Test
    void delete_shouldReturn200() throws Exception {
        doNothing().when(masterService).delete("domains", testId);

        mockMvc.perform(delete("/api/v1/masters/domains/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Deleted successfully"));
    }
}
