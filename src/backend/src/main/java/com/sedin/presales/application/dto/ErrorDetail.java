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
public class ErrorDetail {

    private String code;
    private String message;
    private List<FieldError> fieldErrors;
}
