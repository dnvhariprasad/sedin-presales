package com.sedin.presales.application.service;

import com.sedin.presales.domain.enums.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionEvaluator {

    /**
     * Returns the numeric level of a permission for hierarchy comparison.
     * ADMIN > WRITE > READ
     */
    private int permissionLevel(Permission permission) {
        return switch (permission) {
            case READ -> 1;
            case WRITE -> 2;
            case ADMIN -> 3;
        };
    }

    /**
     * Checks if the granted permission is sufficient to satisfy the required permission.
     * ADMIN >= WRITE >= READ
     */
    public boolean isPermissionSufficient(Permission granted, Permission required) {
        return permissionLevel(granted) >= permissionLevel(required);
    }
}
