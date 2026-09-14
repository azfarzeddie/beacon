package com.beacon.service.handler;

import com.beacon.model.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

import static com.beacon.exception.PreferenceException.PreferenceAlreadyExists;
import static com.beacon.exception.PreferenceException.PreferenceNotFound;

@RestControllerAdvice
public class PreferenceExceptionHandler {

    @ExceptionHandler(PreferenceAlreadyExists.class)
    ResponseEntity<ErrorResponse> handlePreferenceAlreadyExistsException(PreferenceAlreadyExists e, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("PREFERENCE_ALREADY_EXISTS")
                .errorMessage(e.getLocalizedMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(PreferenceNotFound.class)
    ResponseEntity<ErrorResponse> handlePreferenceNotFoundException(PreferenceNotFound e, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("PREFERENCE_NOT_FOUND")
                .errorMessage(e.getLocalizedMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
