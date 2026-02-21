package com.sedin.presales.application.dto;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResultDto {
    private UUID id;
    private UUID documentVersionId;
    private UUID agentId;
    private boolean isValid;
    private Object validationDetails;
    private Instant createdAt;
}
