package com.sedin.presales.application.service;

import com.sedin.presales.application.dto.LoginRequest;
import com.sedin.presales.application.dto.LoginResponse;
import com.sedin.presales.application.exception.BadRequestException;
import com.sedin.presales.config.JwtTokenProvider;
import com.sedin.presales.domain.entity.User;
import com.sedin.presales.domain.enums.UserStatus;
import com.sedin.presales.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("User account is not active");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                user.getDisplayName()
        );

        log.debug("Login successful for email: {}", request.getEmail());

        return LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .build();
    }
}
