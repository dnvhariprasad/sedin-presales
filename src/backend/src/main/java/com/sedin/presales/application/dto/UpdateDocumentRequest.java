package com.sedin.presales.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateDocumentRequest {

    private String title;
    private String customerName;
    private LocalDate documentDate;
    private UUID documentTypeId;
    private UUID folderId;
    private CreateDocumentMetadataRequest metadata;
}
