package com.sedin.presales.application.dto;

import com.sedin.presales.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequest {
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String displayName;
    @NotNull
    private Role role;
    private String avatarUrl;
    private String password;
}
