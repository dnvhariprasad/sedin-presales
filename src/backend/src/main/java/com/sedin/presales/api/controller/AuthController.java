package com.sedin.presales.api.controller;

import com.sedin.presales.application.dto.LoginRequest;
import com.sedin.presales.application.dto.LoginResponse;
import com.sedin.presales.application.service.AuthService;
import com.sedin.presales.config.CurrentUserService;
import com.sedin.presales.config.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        UserPrincipal principal = currentUserService.getCurrentUser();
        return ResponseEntity.ok(Map.of("data", Map.of(
                "userId", principal.getUserId(),
                "email", principal.getEmail(),
                "displayName", principal.getDisplayName(),
                "role", principal.getRole()
        )));
    }
}
