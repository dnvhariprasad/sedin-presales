package com.sedin.presales.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequestDto {

    private String query;
    private UUID domainId;
    private UUID industryId;
    private UUID documentTypeId;
    private UUID businessUnitId;

    @Min(1)
    @Max(50)
    @Builder.Default
    private int topK = 10;

    @Builder.Default
    private boolean includeRagAnswer = false;
}
