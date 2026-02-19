package com.sedin.presales.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sedin.presales.domain.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentDto {

    private UUID id;
    private String title;
    private String customerName;
    private LocalDate documentDate;
    private DocumentStatus status;
    private Boolean ragIndexed;
    private Integer currentVersionNumber;
    private UUID folderId;
    private String folderName;
    private UUID documentTypeId;
    private String documentTypeName;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
