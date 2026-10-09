package com.beacon.controller.advice;

import com.beacon.exception.NotificationException.BulkNotificationJobNotFound;
import com.beacon.exception.NotificationException.ChannelNotAvailableForUser;
import com.beacon.exception.NotificationException.NotificationDispatchException;
import com.beacon.exception.NotificationException.NotificationNotAllowed;
import com.beacon.exception.PreferenceException.PreferenceAlreadyExists;
import com.beacon.exception.PreferenceException.PreferenceNotFound;
import com.beacon.exception.TemplateException.TemplateAlreadyExists;
import com.beacon.exception.TemplateException.TemplateNotFound;
import com.beacon.exception.TemplateException.TemplateNotResolved;
import com.beacon.exception.UserException.UserAlreadyExistsException;
import com.beacon.exception.UserException.UserNotFoundException;
import com.beacon.model.response.ErrorResponse;
import com.beacon.model.response.ErrorResponse.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/**
 * Single place where exceptions become {@link ErrorResponse}s.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String RETRY_AFTER_SECONDS = "30";

    @ExceptionHandler({
            UserAlreadyExistsException.class, UserNotFoundException.class,
            PreferenceAlreadyExists.class, PreferenceNotFound.class,
            TemplateAlreadyExists.class, TemplateNotFound.class, TemplateNotResolved.class,
            NotificationDispatchException.class, NotificationNotAllowed.class,
            ChannelNotAvailableForUser.class, BulkNotificationJobNotFound.class
    })
    ResponseEntity<ErrorResponse> handleDomainException(RuntimeException e, HttpServletRequest request) {
        ApiError error = ApiError.of(e).orElseThrow(() -> e);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(error.getStatus());
        if (error.getStatus() == HttpStatus.SERVICE_UNAVAILABLE) {
            builder.header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS);
        }
        return builder.body(body(error.name(), e.getLocalizedMessage(), request, null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadableRequestException(HttpMessageNotReadableException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "Request body contains an invalid value", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<ValidationError> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(body("VALIDATION_ERROR", "Request validation failed", request, errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Parameter '" + e.getName() + "' has an invalid value", request);
    }

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

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e, HttpServletRequest request) throws Exception {
        // Framework exceptions that carry their own status (405, 404, 415...) keep Spring's handling
        if (e instanceof org.springframework.web.ErrorResponse || e instanceof ResponseStatusException) {
            throw e;
        }
        log.error("Unhandled exception on {}", request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(body(code, message, request, null));
    }

    private ErrorResponse body(String code, String message, HttpServletRequest request, List<ValidationError> validationErrors) {
        return ErrorResponse.builder()
                .errorCode(code)
                .errorMessage(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();
    }
}
