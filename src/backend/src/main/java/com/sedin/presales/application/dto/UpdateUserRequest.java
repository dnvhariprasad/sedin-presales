package com.sedin.presales.application.dto;

import com.sedin.presales.domain.enums.Role;
import com.sedin.presales.domain.enums.UserStatus;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {
    private String displayName;
    private Role role;
    private UserStatus status;
    private String avatarUrl;
}
