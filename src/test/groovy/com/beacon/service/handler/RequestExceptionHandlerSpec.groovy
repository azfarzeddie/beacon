package com.beacon.service.handler

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import spock.lang.Specification

class RequestExceptionHandlerSpec extends Specification {

    RequestExceptionHandler handler = new RequestExceptionHandler()
    HttpServletRequest request = Stub() { getRequestURI() >> "/api/v1/users" }

    def "maps HttpMessageNotReadableException to a 400 BAD_REQUEST error response"() {
        given:
        def exception = new HttpMessageNotReadableException("bad json", Stub(HttpInputMessage))

        when:
        def response = handler.handleUnreadableRequestException(exception, request)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.errorCode == "INVALID_REQUEST_BODY"
        response.body.path == "/api/v1/users"
    }

    def "maps MethodArgumentNotValidException field errors into a validation error response"() {
        given:
        def bindingResult = Stub(BindingResult) {
            getFieldErrors() >> [
                    new FieldError("createUserRequest", "name", "must not be empty"),
                    new FieldError("createUserRequest", "email", "must be a well-formed email address")
            ]
        }
        def exception = new MethodArgumentNotValidException(Stub(MethodParameter), bindingResult)

        when:
        def response = handler.handleValidationException(exception, request)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.errorCode == "VALIDATION_ERROR"
        response.body.validationErrors.size() == 2
        response.body.validationErrors*.field.containsAll(["name", "email"])
        response.body.validationErrors.find { it.field == "name" }.message == "must not be empty"
    }
}
