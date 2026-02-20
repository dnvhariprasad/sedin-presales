package com.sedin.presales.application.dto;

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
public class SearchResultDto {

    private UUID documentId;
    private String title;
    private String customerName;
    private String snippet;
    private double score;
    private String domain;
    private String industry;
    private String documentType;
    private List<String> technologies;
}
