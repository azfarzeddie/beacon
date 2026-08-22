package com.beacon.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ErrorResponse {
    String errorCode;
    String errorMessage;
    Instant timestamp;
    String path;
    List<ValidationError> errors;

    @Data
    @AllArgsConstructor
    public static class ValidationError {
        String field;
        String message;
    }
}
