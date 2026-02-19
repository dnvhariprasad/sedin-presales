package com.sedin.presales.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditorConfig {

    private final CurrentUserService currentUserService;

    public AuditorConfig(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            try {
                return Optional.ofNullable(currentUserService.getCurrentUserEmail());
            } catch (Exception e) {
                return Optional.of("system");
            }
        };
    }
}
