package com.beacon.service.handler

import com.beacon.exception.TemplateException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import spock.lang.Specification

class TemplateExceptionHandlerSpec extends Specification {

    TemplateExceptionHandler handler = new TemplateExceptionHandler()
    HttpServletRequest request = Stub() { getRequestURI() >> "/api/v1/templates" }

    def "maps TemplateAlreadyExists to a 409 CONFLICT error response"() {
        given:
        def exception = new TemplateException.TemplateAlreadyExists("A template for welcome and EMAIL already exists.")

        when:
        def response = handler.handleTemplateAlreadyExistsException(exception, request)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body.errorCode == "TEMPLATE_ALREADY_EXISTS"
        response.body.errorMessage == "A template for welcome and EMAIL already exists."
        response.body.path == "/api/v1/templates"
    }

    def "maps TemplateNotFound to a 404 NOT_FOUND error response"() {
        given:
        def exception = new TemplateException.TemplateNotFound("No template found for welcome and EMAIL")

        when:
        def response = handler.handleTemplateNotFoundException(exception, request)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.errorCode == "TEMPLATE_NOT_FOUND"
        response.body.errorMessage == "No template found for welcome and EMAIL"
    }

    def "maps TemplateNotResolved to a 422 UNPROCESSABLE_ENTITY error response"() {
        given:
        def exception = new TemplateException.TemplateNotResolved("Failed to resolve template: welcome with variables: {}")

        when:
        def response = handler.handleTemplateNotResolvedException(exception, request)

        then:
        response.statusCode == HttpStatus.UNPROCESSABLE_ENTITY
        response.body.errorCode == "TEMPLATE_NOT_RESOLVED"
        response.body.errorMessage == "Failed to resolve template: welcome with variables: {}"
        response.body.path == "/api/v1/templates"
    }
}
