package com.sedin.presales.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentMetadataDto {

    private UUID id;
    private IdNameDto domain;
    private IdNameDto industry;
    private IdNameDto businessUnit;
    private IdNameDto sbu;
    private List<IdNameDto> technologies;
}
