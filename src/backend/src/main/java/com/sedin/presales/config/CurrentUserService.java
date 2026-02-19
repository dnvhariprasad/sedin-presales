package com.sedin.presales.config;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

public interface CurrentUserService {

    UserPrincipal getCurrentUser();

    default String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }

    @Service
    @Profile("!dev")
    class ProdCurrentUserService implements CurrentUserService {

        @Override
        public UserPrincipal getCurrentUser() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return UserPrincipal.fromJwt(jwt);
        }
    }

    @Service
    @Profile("dev")
    class DevCurrentUserService implements CurrentUserService {

        @Override
        public UserPrincipal getCurrentUser() {
            return DevUserPrincipal.create();
        }
    }
}
