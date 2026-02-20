package com.sedin.presales.application.dto;

import com.sedin.presales.domain.enums.RenditionStatus;
import com.sedin.presales.domain.enums.RenditionType;
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
public class RenditionDto {

    private UUID id;
    private RenditionType renditionType;
    private RenditionStatus status;
    private String filePath;
    private Long fileSize;
    private String errorMessage;
    private Instant createdAt;
}
