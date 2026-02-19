package com.sedin.presales.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.AclEntryDto;
import com.sedin.presales.application.dto.BulkGrantAccessRequest;
import com.sedin.presales.application.dto.GrantAccessRequest;
import com.sedin.presales.application.exception.GlobalExceptionHandler;
import com.sedin.presales.application.service.AclService;
import com.sedin.presales.config.TestSecurityConfig;
import com.sedin.presales.domain.enums.Permission;
import com.sedin.presales.domain.enums.ResourceType;
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

@WebMvcTest(value = AclController.class,
        excludeAutoConfiguration = {OAuth2ResourceServerAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class AclControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AclService aclService;

    private final UUID testId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID resourceId = UUID.randomUUID();

    private AclEntryDto buildAclEntryDto() {
        return AclEntryDto.builder()
                .id(testId)
                .resourceType(ResourceType.DOCUMENT)
                .resourceId(resourceId)
                .userId(userId)
                .userEmail("user@test.com")
                .userDisplayName("Test User")
                .permission(Permission.READ)
                .grantedBy("system")
                .grantedAt(Instant.now())
                .build();
    }

    @Test
    void grantAccess_shouldReturn201() throws Exception {
        GrantAccessRequest request = GrantAccessRequest.builder()
                .userId(userId)
                .resourceType(ResourceType.DOCUMENT)
                .resourceId(resourceId)
                .permission(Permission.READ)
                .build();

        AclEntryDto dto = buildAclEntryDto();
        when(aclService.grantAccess(any(GrantAccessRequest.class), eq("system"))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/acl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.permission").value("READ"))
                .andExpect(jsonPath("$.message").value("Access granted successfully"));
    }

    @Test
    void bulkGrantAccess_shouldReturn201() throws Exception {
        UUID userId2 = UUID.randomUUID();
        BulkGrantAccessRequest request = BulkGrantAccessRequest.builder()
                .userIds(List.of(userId, userId2))
                .resourceType(ResourceType.DOCUMENT)
                .resourceId(resourceId)
                .permission(Permission.WRITE)
                .build();

        AclEntryDto dto1 = buildAclEntryDto();
        AclEntryDto dto2 = AclEntryDto.builder()
                .id(UUID.randomUUID())
                .resourceType(ResourceType.DOCUMENT)
                .resourceId(resourceId)
                .userId(userId2)
                .permission(Permission.WRITE)
                .grantedBy("system")
                .grantedAt(Instant.now())
                .build();

        when(aclService.bulkGrantAccess(any(BulkGrantAccessRequest.class), eq("system")))
                .thenReturn(List.of(dto1, dto2));

        mockMvc.perform(post("/api/v1/acl/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.message").value("Bulk access granted successfully"));
    }

    @Test
    void revokeAccess_shouldReturn200() throws Exception {
        doNothing().when(aclService).revokeAccess(testId);

        mockMvc.perform(delete("/api/v1/acl/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Access revoked successfully"));
    }

    @Test
    void revokeAllAccess_shouldReturn200() throws Exception {
        doNothing().when(aclService).revokeAllAccess(ResourceType.DOCUMENT, resourceId);

        mockMvc.perform(delete("/api/v1/acl/resource/DOCUMENT/{resourceId}", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All access revoked successfully"));
    }

    @Test
    void getAccessList_shouldReturn200() throws Exception {
        AclEntryDto dto = buildAclEntryDto();
        when(aclService.getAccessList(ResourceType.DOCUMENT, resourceId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/acl/resource/DOCUMENT/{resourceId}", resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].permission").value("READ"));
    }

    @Test
    void getUserPermissions_shouldReturn200() throws Exception {
        AclEntryDto dto = buildAclEntryDto();
        when(aclService.getUserPermissions(userId, ResourceType.DOCUMENT, resourceId))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/acl/user/{userId}/resource/DOCUMENT/{resourceId}", userId, resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].userId").value(userId.toString()));
    }

    @Test
    void checkPermission_shouldReturn200() throws Exception {
        when(aclService.hasPermission(userId, ResourceType.DOCUMENT, resourceId, Permission.READ))
                .thenReturn(true);

        mockMvc.perform(get("/api/v1/acl/check")
                        .param("userId", userId.toString())
                        .param("resourceType", "DOCUMENT")
                        .param("resourceId", resourceId.toString())
                        .param("permission", "READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }
}
