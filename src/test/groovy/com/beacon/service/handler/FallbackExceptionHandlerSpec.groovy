package com.beacon.service.handler

import jakarta.servlet.http.HttpServletRequest
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class FallbackExceptionHandlerSpec extends Specification {

    FallbackExceptionHandler handler = new FallbackExceptionHandler()
    HttpServletRequest request = Stub() { getRequestURI() >> "/api/v1/users" }

    def "maps DataIntegrityViolationException to a 409 CONFLICT error response"() {
        when:
        def response = handler.handleDataIntegrityViolationException(new DataIntegrityViolationException("dup"), request)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body.errorCode == "DATA_INTEGRITY_VIOLATION"
        response.body.path == "/api/v1/users"
    }

    def "maps DataAccessException to a 500 error response without leaking the cause"() {
        when:
        def response = handler.handleDataAccessException(new DataAccessResourceFailureException("jdbc:secret"), request)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body.errorCode == "DATA_ACCESS_ERROR"
        !response.body.errorMessage.contains("secret")
    }

    def "maps MethodArgumentTypeMismatchException to a 400 error response"() {
        given:
        def exception = new MethodArgumentTypeMismatchException("abc", Long, "id", null, null)

        when:
        def response = handler.handleTypeMismatchException(exception, request)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.errorCode == "INVALID_PARAMETER"
        response.body.errorMessage.contains("id")
    }

    def "maps unexpected exceptions to a generic 500 error response"() {
        when:
        def response = handler.handleUnexpectedException(new IllegalStateException("boom"), request)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body.errorCode == "INTERNAL_ERROR"
        !response.body.errorMessage.contains("boom")
    }

    def "rethrows exceptions that carry their own status"() {
        when:
        handler.handleUnexpectedException(new ResponseStatusException(HttpStatus.NOT_FOUND), request)

        then:
        thrown(ResponseStatusException)
    }
}
