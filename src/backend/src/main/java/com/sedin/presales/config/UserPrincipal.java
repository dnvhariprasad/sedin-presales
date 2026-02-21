package com.sedin.presales.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal {

    private final String userId;
    private final String email;
    private final String displayName;
    private final String role;
}
