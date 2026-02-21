package com.sedin.presales.application.dto;

import lombok.*;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaseStudyWizardResponseDto {
    private UUID documentId;
    private UUID documentVersionId;
    private String message;
}
