package com.sedin.presales.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtRoleConverterTest {

    @InjectMocks
    private JwtRoleConverter jwtRoleConverter;

    @Test
    @DisplayName("should convert JWT roles claim to ROLE_ prefixed authorities")
    void convert_shouldConvertRolesToPrefixedAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("roles", List.of("ADMIN"))
                .claim("preferred_username", "admin@sedin.com")
                .build();

        AbstractAuthenticationToken result = jwtRoleConverter.convert(jwt);

        assertThat(result).isNotNull();
        Collection<GrantedAuthority> authorities = result.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("should default to ROLE_VIEWER when no roles claim present")
    void convert_shouldDefaultToViewerWhenNoRolesClaim() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("preferred_username", "viewer@sedin.com")
                .build();

        AbstractAuthenticationToken result = jwtRoleConverter.convert(jwt);

        assertThat(result).isNotNull();
        Collection<GrantedAuthority> authorities = result.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_VIEWER");
    }

    @Test
    @DisplayName("should handle multiple roles")
    void convert_shouldHandleMultipleRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("roles", List.of("ADMIN", "EDITOR"))
                .claim("preferred_username", "superuser@sedin.com")
                .build();

        AbstractAuthenticationToken result = jwtRoleConverter.convert(jwt);

        assertThat(result).isNotNull();
        Collection<GrantedAuthority> authorities = result.getAuthorities();
        assertThat(authorities).hasSize(2);
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_EDITOR");
    }
}
