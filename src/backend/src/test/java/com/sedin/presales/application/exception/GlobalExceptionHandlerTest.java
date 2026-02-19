package com.sedin.presales.application.exception;

import com.sedin.presales.application.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleResourceNotFound should return 404 with NOT_FOUND code")
    void handleResourceNotFound_shouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Document", "id", "123");

        ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getError().getMessage()).contains("Document not found with id: '123'");
    }

    @Test
    @DisplayName("handleAccessDenied should return 403 with FORBIDDEN code")
    void handleAccessDenied_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("You do not have access");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("handleBadRequest should return 400 with BAD_REQUEST code")
    void handleBadRequest_shouldReturn400() {
        BadRequestException ex = new BadRequestException("Invalid input");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBadRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("handleDuplicateResource should return 409 with CONFLICT code")
    void handleDuplicateResource_shouldReturn409() {
        DuplicateResourceException ex = new DuplicateResourceException("Resource already exists");

        ResponseEntity<ApiResponse<Void>> response = handler.handleDuplicateResource(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("CONFLICT");
    }

    @Test
    @DisplayName("handleValidation should return 400 with field errors")
    void handleValidation_shouldReturn400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        org.springframework.validation.FieldError fieldError1 =
                new org.springframework.validation.FieldError("object", "title", "must not be blank");
        org.springframework.validation.FieldError fieldError2 =
                new org.springframework.validation.FieldError("object", "email", "must be valid");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().getError().getFieldErrors()).hasSize(2);
        assertThat(response.getBody().getError().getFieldErrors().get(0).getField()).isEqualTo("title");
        assertThat(response.getBody().getError().getFieldErrors().get(0).getMessage()).isEqualTo("must not be blank");
        assertThat(response.getBody().getError().getFieldErrors().get(1).getField()).isEqualTo("email");
    }

    @Test
    @DisplayName("handleMaxUploadSize should return 413")
    void handleMaxUploadSize_shouldReturn413() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1024);

        ResponseEntity<ApiResponse<Void>> response = handler.handleMaxUploadSize(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("PAYLOAD_TOO_LARGE");
    }

    @Test
    @DisplayName("handleGenericException should return 500 with INTERNAL_SERVER_ERROR code")
    void handleGenericException_shouldReturn500() {
        RuntimeException ex = new RuntimeException("Something went wrong");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
