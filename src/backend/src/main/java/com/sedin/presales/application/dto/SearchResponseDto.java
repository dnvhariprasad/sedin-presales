package com.sedin.presales.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchResponseDto {

    private List<SearchResultDto> results;
    private long totalCount;
    private String query;
    private String ragAnswer;
    private List<SourceCitationDto> sources;
}
