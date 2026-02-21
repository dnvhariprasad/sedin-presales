package com.sedin.presales.config;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "testSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm123456";
    private static final long EXPIRATION_MS = 3600000L;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtTokenProvider.generateToken("user-123", "test@sedin.com", "ADMIN", "Test User");

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractClaims_shouldContainCorrectClaims() {
        String token = jwtTokenProvider.generateToken("user-123", "test@sedin.com", "ADMIN", "Test User");

        Claims claims = jwtTokenProvider.extractClaims(token);

        assertThat(claims.getSubject()).isEqualTo("user-123");
        assertThat(claims.get("email", String.class)).isEqualTo("test@sedin.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("displayName", String.class)).isEqualTo("Test User");
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtTokenProvider.generateToken("user-123", "test@sedin.com", "ADMIN", "Test User");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(TEST_SECRET, -1000L);
        String token = shortLivedProvider.generateToken("user-123", "test@sedin.com", "ADMIN", "Test User");

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalseForMalformedToken() {
        assertThat(jwtTokenProvider.validateToken("not.a.valid.jwt.token")).isFalse();
    }
}
