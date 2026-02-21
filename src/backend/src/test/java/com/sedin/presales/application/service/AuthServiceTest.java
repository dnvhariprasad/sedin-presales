package com.sedin.presales.application.service;

import com.sedin.presales.application.dto.LoginRequest;
import com.sedin.presales.application.dto.LoginResponse;
import com.sedin.presales.application.exception.BadRequestException;
import com.sedin.presales.config.JwtTokenProvider;
import com.sedin.presales.domain.entity.User;
import com.sedin.presales.domain.enums.Role;
import com.sedin.presales.domain.enums.UserStatus;
import com.sedin.presales.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User buildUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@sedin.com");
        user.setDisplayName("Test User");
        user.setRole(Role.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("$2a$10$hashedpassword");
        return user;
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() {
        User user = buildUser();
        LoginRequest request = LoginRequest.builder()
                .email("test@sedin.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("test@sedin.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("mock-jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(3600000L);

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getEmail()).isEqualTo("test@sedin.com");
        assertThat(response.getDisplayName()).isEqualTo("Test User");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getExpiresIn()).isEqualTo(3600000L);
    }

    @Test
    void login_shouldThrowForWrongPassword() {
        User user = buildUser();
        LoginRequest request = LoginRequest.builder()
                .email("test@sedin.com")
                .password("wrongpassword")
                .build();

        when(userRepository.findByEmail("test@sedin.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_shouldThrowForNonexistentEmail() {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@sedin.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("nonexistent@sedin.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_shouldThrowForInactiveUser() {
        User user = buildUser();
        user.setStatus(UserStatus.INACTIVE);
        LoginRequest request = LoginRequest.builder()
                .email("test@sedin.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("test@sedin.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User account is not active");
    }
}
