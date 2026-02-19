package com.sedin.presales.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.oauth2.jwt.Jwt;

@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal {

    private final String userId;
    private final String email;
    private final String displayName;
    private final String role;

    public static UserPrincipal fromJwt(Jwt jwt) {
        return UserPrincipal.builder()
                .userId(jwt.getClaimAsString("oid"))
                .email(jwt.getClaimAsString("preferred_username"))
                .displayName(jwt.getClaimAsString("name"))
                .role(jwt.getClaimAsStringList("roles") != null && !jwt.getClaimAsStringList("roles").isEmpty()
                        ? jwt.getClaimAsStringList("roles").get(0)
                        : "USER")
                .build();
    }
}
