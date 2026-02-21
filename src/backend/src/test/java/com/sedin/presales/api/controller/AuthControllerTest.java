package com.sedin.presales.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sedin.presales.application.dto.LoginRequest;
import com.sedin.presales.application.dto.LoginResponse;
import com.sedin.presales.application.exception.GlobalExceptionHandler;
import com.sedin.presales.application.service.AuthService;
import com.sedin.presales.config.CurrentUserService;
import com.sedin.presales.config.JwtTokenProvider;
import com.sedin.presales.config.TestSecurityConfig;
import com.sedin.presales.config.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void login_shouldReturn200WithValidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@sedin.com")
                .password("password123")
                .build();

        LoginResponse response = LoginResponse.builder()
                .token("mock-jwt-token")
                .email("admin@sedin.com")
                .displayName("Admin User")
                .role("ADMIN")
                .expiresIn(3600000L)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.data.email").value("admin@sedin.com"))
                .andExpect(jsonPath("$.data.displayName").value("Admin User"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void login_shouldReturn400WithInvalidRequest() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("")
                .password("")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_shouldReturn200WithCurrentUser() throws Exception {
        UserPrincipal principal = UserPrincipal.builder()
                .userId("user-123")
                .email("admin@sedin.com")
                .displayName("Admin User")
                .role("ADMIN")
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(principal);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("user-123"))
                .andExpect(jsonPath("$.data.email").value("admin@sedin.com"))
                .andExpect(jsonPath("$.data.displayName").value("Admin User"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }
}
