package com.beacon.service.handler;

import com.beacon.model.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

import static com.beacon.exception.NotificationException.BulkNotificationJobNotFound;
import static com.beacon.exception.NotificationException.NotificationDispatchException;
import static com.beacon.exception.NotificationException.NotificationNotAllowed;

@RestControllerAdvice
public class NotificationExceptionHandler {

    @ExceptionHandler(NotificationDispatchException.class)
    ResponseEntity<ErrorResponse> handleNotificationDispatchException(NotificationDispatchException e, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("NOTIFICATION_DISPATCH_FAILED")
                .errorMessage(e.getLocalizedMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(NotificationNotAllowed.class)
    ResponseEntity<ErrorResponse> handleNotificationNotAllowedException(NotificationNotAllowed e, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("NOTIFICATION_NOT_ALLOWED")
                .errorMessage(e.getLocalizedMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(BulkNotificationJobNotFound.class)
    ResponseEntity<ErrorResponse> handleBulkNotificationJobNotFoundException(BulkNotificationJobNotFound e, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("BULK_NOTIFICATION_JOB_NOT_FOUND")
                .errorMessage(e.getLocalizedMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
