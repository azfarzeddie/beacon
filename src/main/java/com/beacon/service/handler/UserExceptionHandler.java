package com.beacon.service.handler;

import com.beacon.model.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

import static com.beacon.exception.UserException.*;

@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException e, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("USER_ALREADY_EXISTS")
                .errorMessage(e.getMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .errorCode("USER_NOT_FOUND")
                .errorMessage(e.getMessage())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
