package com.sedin.presales.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.CreateFolderRequest;
import com.sedin.presales.application.dto.FolderDto;
import com.sedin.presales.application.exception.GlobalExceptionHandler;
import com.sedin.presales.application.service.FolderService;
import com.sedin.presales.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
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

@WebMvcTest(value = FolderController.class,
        excludeAutoConfiguration = {OAuth2ResourceServerAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class FolderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FolderService folderService;

    private final UUID testId = UUID.randomUUID();

    private FolderDto buildFolderDto() {
        return FolderDto.builder()
                .id(testId)
                .name("Test Folder")
                .description("Test Description")
                .childCount(0)
                .documentCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy("system")
                .build();
    }

    @Test
    void list_shouldReturnFolders() throws Exception {
        FolderDto dto = buildFolderDto();
        when(folderService.list(null)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Test Folder"));
    }

    @Test
    void list_shouldReturnChildFolders() throws Exception {
        UUID parentId = UUID.randomUUID();
        FolderDto dto = FolderDto.builder()
                .id(testId)
                .name("Child Folder")
                .parentId(parentId)
                .childCount(0)
                .documentCount(0)
                .createdAt(Instant.now())
                .build();
        when(folderService.list(parentId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/folders")
                        .param("parentId", parentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Child Folder"))
                .andExpect(jsonPath("$.data[0].parentId").value(parentId.toString()));
    }

    @Test
    void getById_shouldReturnFolder() throws Exception {
        FolderDto dto = buildFolderDto();
        when(folderService.getById(testId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/folders/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testId.toString()))
                .andExpect(jsonPath("$.data.name").value("Test Folder"));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Test Folder")
                .build();
        FolderDto dto = buildFolderDto();

        when(folderService.create(any(CreateFolderRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Folder"))
                .andExpect(jsonPath("$.message").value("Folder created successfully"));
    }

    @Test
    void create_shouldReturn400WhenNameBlank() throws Exception {
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("")
                .build();

        mockMvc.perform(post("/api/v1/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Updated Folder")
                .description("Updated Desc")
                .build();
        FolderDto dto = buildFolderDto();

        when(folderService.update(eq(testId), any(CreateFolderRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/folders/{id}", testId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Folder updated successfully"));
    }

    @Test
    void delete_shouldReturn200() throws Exception {
        doNothing().when(folderService).delete(testId);

        mockMvc.perform(delete("/api/v1/folders/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Folder deleted successfully"));
    }
}
