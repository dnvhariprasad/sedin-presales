package com.sedin.presales.application.dto;

import com.sedin.presales.domain.enums.Permission;
import com.sedin.presales.domain.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AclEntryDto {

    private UUID id;
    private ResourceType resourceType;
    private UUID resourceId;
    private UUID userId;
    private String userEmail;
    private String userDisplayName;
    private Permission permission;
    private String grantedBy;
    private Instant grantedAt;
}
