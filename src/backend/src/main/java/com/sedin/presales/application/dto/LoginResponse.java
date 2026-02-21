package com.sedin.presales.application.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private String displayName;
    private String role;
    private long expiresIn;
}
