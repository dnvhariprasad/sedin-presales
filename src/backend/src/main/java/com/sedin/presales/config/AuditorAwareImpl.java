package com.sedin.presales.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorProvider")
public class AuditorAwareImpl implements AuditorAware<String> {

    private final CurrentUserService currentUserService;

    public AuditorAwareImpl(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        try {
            UserPrincipal principal = currentUserService.getCurrentUser();
            if (principal != null && principal.getEmail() != null) {
                return Optional.of(principal.getEmail());
            }
        } catch (Exception e) {
            // No security context (e.g., async tasks, system operations)
        }
        return Optional.of("system");
    }
}
