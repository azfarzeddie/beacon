package com.beacon.service.handler;

import com.beacon.model.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

/**
 * Last-resort handlers so every error leaves the API in the {@link ErrorResponse} shape.
 * Lowest precedence so the catch-all never shadows the domain-specific advices.
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class FallbackExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Data integrity violation on {}", request.getRequestURI(), e);
        return build(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "Request conflicts with existing data", request);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException e, HttpServletRequest request) {
        log.error("Data access failure on {}", request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "DATA_ACCESS_ERROR", "A database error occurred", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Parameter '" + e.getName() + "' has an invalid value", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e, HttpServletRequest request) throws Exception {
        // Framework exceptions that carry their own status (405, 404, 415...) keep Spring's handling
        if (e instanceof org.springframework.web.ErrorResponse || e instanceof org.springframework.web.server.ResponseStatusException) {
            throw e;
        }
        log.error("Unhandled exception on {}", request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .errorCode(code)
                .errorMessage(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
