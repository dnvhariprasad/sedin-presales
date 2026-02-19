package com.sedin.presales.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FolderDto {

    private UUID id;
    private String name;
    private String description;
    private UUID parentId;
    private String parentName;
    private int childCount;
    private int documentCount;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
