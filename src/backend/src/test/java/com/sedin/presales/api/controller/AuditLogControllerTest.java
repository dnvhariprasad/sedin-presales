package com.sedin.presales.api.controller;

import com.sedin.presales.application.dto.AuditLogDto;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.exception.GlobalExceptionHandler;
import com.sedin.presales.application.service.AuditLogService;
import com.sedin.presales.config.JwtTokenProvider;
import com.sedin.presales.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditLogController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID testId = UUID.randomUUID();

    private AuditLogDto buildAuditLogDto() {
        return AuditLogDto.builder()
                .id(testId)
                .userEmail("admin@sedin.com")
                .action("CREATE_DOCUMENT")
                .resourceType("DOCUMENT")
                .resourceId(UUID.randomUUID())
                .details(Map.of("title", "Test Document"))
                .ipAddress("192.168.1.1")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void list_shouldReturn200() throws Exception {
        AuditLogDto dto = buildAuditLogDto();
        PagedResponse<AuditLogDto> pagedResponse = PagedResponse.<AuditLogDto>builder()
                .content(List.of(dto))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(auditLogService.list(any(), any(), any(), any(), any(), any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].action").value("CREATE_DOCUMENT"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void list_shouldReturn200WithFilters() throws Exception {
        AuditLogDto dto = buildAuditLogDto();
        PagedResponse<AuditLogDto> pagedResponse = PagedResponse.<AuditLogDto>builder()
                .content(List.of(dto))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(auditLogService.list(any(), any(), any(), any(), any(), any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("userEmail", "admin@sedin.com")
                        .param("action", "CREATE_DOCUMENT")
                        .param("resourceType", "DOCUMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].userEmail").value("admin@sedin.com"));
    }

    @Test
    void getById_shouldReturn200() throws Exception {
        AuditLogDto dto = buildAuditLogDto();
        when(auditLogService.getById(testId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/admin/audit-logs/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testId.toString()))
                .andExpect(jsonPath("$.data.action").value("CREATE_DOCUMENT"));
    }
}
