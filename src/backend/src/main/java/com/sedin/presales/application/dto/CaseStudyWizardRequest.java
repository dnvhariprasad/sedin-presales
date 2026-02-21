package com.sedin.presales.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaseStudyWizardRequest {
    @NotBlank
    private String title;
    private String customerName;
    @NotBlank
    private String customerOverview;
    @NotNull
    private List<String> challenges;
    @NotBlank
    private String solution;
    private List<String> technologies;
    @NotNull
    private List<String> results;
    @Builder.Default
    private boolean enhanceWithAi = false;
}
