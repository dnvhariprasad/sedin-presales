package com.sedin.presales.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sedin.presales.domain.enums.RenditionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SummaryResponseDto {

    private UUID documentId;
    private String summary;
    private RenditionStatus status;
    private String message;
}
