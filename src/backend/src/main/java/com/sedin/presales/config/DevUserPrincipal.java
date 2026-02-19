package com.sedin.presales.config;

public final class DevUserPrincipal {

    private DevUserPrincipal() {
    }

    public static UserPrincipal create() {
        return UserPrincipal.builder()
                .userId("00000000-0000-0000-0000-000000000000")
                .email("dev@sedin.com")
                .displayName("Dev User")
                .role("ADMIN")
                .build();
    }
}
