package com.sedin.presales.application.service;

import com.sedin.presales.domain.enums.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionEvaluatorTest {

    private final PermissionEvaluator permissionEvaluator = new PermissionEvaluator();

    @Test
    @DisplayName("should return true when granted permission is higher than required")
    void shouldReturnTrue_whenGrantedPermissionIsHigher() {
        boolean result = permissionEvaluator.isPermissionSufficient(Permission.ADMIN, Permission.READ);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return true when granted permission equals required")
    void shouldReturnTrue_whenGrantedEqualsRequired() {
        boolean result = permissionEvaluator.isPermissionSufficient(Permission.WRITE, Permission.WRITE);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should return false when granted permission is lower than required")
    void shouldReturnFalse_whenGrantedPermissionIsLower() {
        boolean result = permissionEvaluator.isPermissionSufficient(Permission.READ, Permission.WRITE);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("ADMIN should satisfy all permission levels")
    void shouldReturnTrue_adminSatisfiesAll() {
        assertThat(permissionEvaluator.isPermissionSufficient(Permission.ADMIN, Permission.READ)).isTrue();
        assertThat(permissionEvaluator.isPermissionSufficient(Permission.ADMIN, Permission.WRITE)).isTrue();
        assertThat(permissionEvaluator.isPermissionSufficient(Permission.ADMIN, Permission.ADMIN)).isTrue();
    }
}
