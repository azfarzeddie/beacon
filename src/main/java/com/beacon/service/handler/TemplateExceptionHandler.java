package com.beacon.service.handler;

import com.beacon.exception.TemplateException.TemplateAlreadyExists;
import com.beacon.model.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class TemplateExceptionHandler {

    @ExceptionHandler(TemplateAlreadyExists.class)
    ResponseEntity<ErrorResponse> handleTemplateAlreadyExistsException(TemplateAlreadyExists e, HttpServletRequest request) {
            ErrorResponse response = ErrorResponse.builder()
                    .errorCode("TEMPLATE_ALREADY_EXISTS")
                    .errorMessage(e.getLocalizedMessage())
                    .timestamp(Instant.now())
                    .path(request.getRequestURI())
                    .build();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}
