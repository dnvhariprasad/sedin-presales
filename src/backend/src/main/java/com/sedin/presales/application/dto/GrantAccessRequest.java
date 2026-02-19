package com.sedin.presales.application.dto;

import com.sedin.presales.domain.enums.Permission;
import com.sedin.presales.domain.enums.ResourceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GrantAccessRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private ResourceType resourceType;

    @NotNull
    private UUID resourceId;

    @NotNull
    private Permission permission;
}
