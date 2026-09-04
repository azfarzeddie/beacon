package com.beacon.service.handler

import com.beacon.exception.UserException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import spock.lang.Specification

class UserExceptionHandlerSpec extends Specification {

    UserExceptionHandler handler = new UserExceptionHandler()
    HttpServletRequest request = Stub() { getRequestURI() >> "/api/v1/users" }

    def "maps UserAlreadyExistsException to a 409 CONFLICT error response"() {
        given:
        def exception = new UserException.UserAlreadyExistsException("A user with ID: ext-1 already exists.")

        when:
        def response = handler.handleUserAlreadyExists(exception, request)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body.errorCode == "USER_ALREADY_EXISTS"
        response.body.errorMessage == "A user with ID: ext-1 already exists."
        response.body.path == "/api/v1/users"
        response.body.timestamp != null
    }

    def "maps UserNotFoundException to a 404 NOT_FOUND error response"() {
        given:
        def exception = new UserException.UserNotFoundException("No user with ID: 1 found.")

        when:
        def response = handler.handleUserNotFound(exception, request)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.errorCode == "USER_NOT_FOUND"
        response.body.errorMessage == "No user with ID: 1 found."
        response.body.path == "/api/v1/users"
    }
}
