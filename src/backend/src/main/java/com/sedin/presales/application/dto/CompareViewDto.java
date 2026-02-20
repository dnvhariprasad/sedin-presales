package com.sedin.presales.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class CompareViewDto {

    private UUID documentId;
    private Integer version1Number;
    private String version1Url;
    private String version1Status;
    private Integer version2Number;
    private String version2Url;
    private String version2Status;
}
