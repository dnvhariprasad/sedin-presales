package com.sedin.presales.application.dto;

import com.sedin.presales.domain.enums.Role;
import com.sedin.presales.domain.enums.UserStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private UUID id;
    private String email;
    private String displayName;
    private Role role;
    private UserStatus status;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
