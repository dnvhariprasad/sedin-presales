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
public class CreateDocumentMetadataRequest {

    private UUID domainId;
    private UUID industryId;
    private UUID businessUnitId;
    private UUID sbuId;
    private List<UUID> technologyIds;
}
