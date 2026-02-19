package com.sedin.presales.application.dto;

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
public class MasterDto {

    private UUID id;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
