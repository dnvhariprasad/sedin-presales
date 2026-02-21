package com.sedin.presales.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.CreateUserRequest;
import com.sedin.presales.application.dto.PagedResponse;
import com.sedin.presales.application.dto.UpdateUserRequest;
import com.sedin.presales.application.dto.UserDto;
import com.sedin.presales.application.exception.GlobalExceptionHandler;
import com.sedin.presales.application.service.UserService;
import com.sedin.presales.config.JwtTokenProvider;
import com.sedin.presales.config.TestSecurityConfig;
import com.sedin.presales.domain.enums.Role;
import com.sedin.presales.domain.enums.UserStatus;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private final UUID testId = UUID.randomUUID();

    private UserDto buildUserDto() {
        return UserDto.builder()
                .id(testId)
                .email("john@sedin.com")
                .displayName("John Doe")
                .role(Role.EDITOR)
                .status(UserStatus.ACTIVE)
                .avatarUrl("https://example.com/avatar.png")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void list_shouldReturn200() throws Exception {
        UserDto dto = buildUserDto();
        PagedResponse<UserDto> pagedResponse = PagedResponse.<UserDto>builder()
                .content(List.of(dto))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(userService.list(any(), any(), any(), any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].email").value("john@sedin.com"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getById_shouldReturn200() throws Exception {
        UserDto dto = buildUserDto();
        when(userService.getById(testId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/admin/users/{id}", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testId.toString()))
                .andExpect(jsonPath("$.data.email").value("john@sedin.com"));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("john@sedin.com")
                .displayName("John Doe")
                .role(Role.EDITOR)
                .build();
        UserDto dto = buildUserDto();

        when(userService.create(any(CreateUserRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("john@sedin.com"))
                .andExpect(jsonPath("$.message").value("User created successfully"));
    }

    @Test
    void create_shouldReturn400WhenEmailInvalid() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("not-an-email")
                .displayName("John Doe")
                .role(Role.EDITOR)
                .build();

        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void update_shouldReturn200() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .displayName("Jane Doe")
                .role(Role.ADMIN)
                .build();
        UserDto dto = buildUserDto();

        when(userService.update(eq(testId), any(UpdateUserRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/admin/users/{id}", testId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User updated successfully"));
    }

    @Test
    void deactivate_shouldReturn200() throws Exception {
        UserDto dto = buildUserDto();
        when(userService.deactivate(testId)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/admin/users/{id}/deactivate", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deactivated successfully"));
    }

    @Test
    void activate_shouldReturn200() throws Exception {
        UserDto dto = buildUserDto();
        when(userService.activate(testId)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/admin/users/{id}/activate", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User activated successfully"));
    }
}
