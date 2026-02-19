package com.sedin.presales.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateDocumentRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String customerName;

    private LocalDate documentDate;

    @NotNull(message = "Document type is required")
    private UUID documentTypeId;

    private UUID folderId;

    private CreateDocumentMetadataRequest metadata;
}
