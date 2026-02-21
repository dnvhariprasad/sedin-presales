package com.sedin.presales.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCaseStudyAgentRequest {

    private String name;
    private String description;
    private Object templateConfig;
}
