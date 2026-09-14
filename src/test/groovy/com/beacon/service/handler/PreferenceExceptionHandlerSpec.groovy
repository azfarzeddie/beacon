package com.beacon.service.handler

import com.beacon.exception.PreferenceException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import spock.lang.Specification

class PreferenceExceptionHandlerSpec extends Specification {

    PreferenceExceptionHandler handler = new PreferenceExceptionHandler()
    HttpServletRequest request = Stub() { getRequestURI() >> "/api/v1/preferences" }

    def "maps PreferenceAlreadyExists to a 409 CONFLICT error response"() {
        given:
        def exception = new PreferenceException.PreferenceAlreadyExists("A notification preference for welcome and EMAIL already exists.")

        when:
        def response = handler.handlePreferenceAlreadyExistsException(exception, request)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body.errorCode == "PREFERENCE_ALREADY_EXISTS"
        response.body.errorMessage == "A notification preference for welcome and EMAIL already exists."
        response.body.path == "/api/v1/preferences"
    }

    def "maps PreferenceNotFound to a 404 NOT_FOUND error response"() {
        given:
        def exception = new PreferenceException.PreferenceNotFound("No preference found for user with ID: ext-1")

        when:
        def response = handler.handlePreferenceNotFoundException(exception, request)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.errorCode == "PREFERENCE_NOT_FOUND"
        response.body.errorMessage == "No preference found for user with ID: ext-1"
        response.body.path == "/api/v1/preferences"
    }
}
