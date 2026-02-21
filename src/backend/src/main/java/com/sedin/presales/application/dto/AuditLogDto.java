package com.sedin.presales.application.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogDto {
    private UUID id;
    private String userEmail;
    private String action;
    private String resourceType;
    private UUID resourceId;
    private Object details;
    private String ipAddress;
    private Instant createdAt;
}
