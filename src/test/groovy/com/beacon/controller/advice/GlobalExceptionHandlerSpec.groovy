package com.beacon.controller.advice

import com.beacon.exception.NotificationException
import com.beacon.exception.PreferenceException
import com.beacon.exception.TemplateException
import com.beacon.exception.UserException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Unroll

class GlobalExceptionHandlerSpec extends Specification {

    GlobalExceptionHandler handler = new GlobalExceptionHandler()
    HttpServletRequest request = Stub() { getRequestURI() >> "/api/v1/things" }

    @Unroll
    def "maps #exception.class.simpleName to #status with code #code"() {
        when:
        def response = handler.handleDomainException(exception, request)

        then:
        response.statusCode == status
        response.body.errorCode == code
        response.body.errorMessage == "msg"
        response.body.path == "/api/v1/things"

        where:
        exception                                                      | status                            | code
        new UserException.UserAlreadyExistsException("msg")            | HttpStatus.CONFLICT               | "USER_ALREADY_EXISTS"
        new UserException.UserNotFoundException("msg")                 | HttpStatus.NOT_FOUND              | "USER_NOT_FOUND"
        new PreferenceException.PreferenceAlreadyExists("msg")         | HttpStatus.CONFLICT               | "PREFERENCE_ALREADY_EXISTS"
        new PreferenceException.PreferenceNotFound("msg")              | HttpStatus.NOT_FOUND              | "PREFERENCE_NOT_FOUND"
        new TemplateException.TemplateAlreadyExists("msg")             | HttpStatus.CONFLICT               | "TEMPLATE_ALREADY_EXISTS"
        new TemplateException.TemplateNotFound("msg")                  | HttpStatus.NOT_FOUND              | "TEMPLATE_NOT_FOUND"
        new TemplateException.TemplateNotResolved("msg")               | HttpStatus.UNPROCESSABLE_ENTITY   | "TEMPLATE_NOT_RESOLVED"
        new NotificationException.NotificationDispatchException("msg") | HttpStatus.SERVICE_UNAVAILABLE    | "NOTIFICATION_DISPATCH_FAILED"
        new NotificationException.NotificationNotAllowed("msg")        | HttpStatus.FORBIDDEN              | "NOTIFICATION_NOT_ALLOWED"
        new NotificationException.ChannelNotAvailableForUser("msg")    | HttpStatus.UNPROCESSABLE_CONTENT  | "CHANNEL_NOT_AVAILABLE_FOR_USER"
        new NotificationException.BulkNotificationJobNotFound("msg")   | HttpStatus.NOT_FOUND              | "BULK_NOTIFICATION_JOB_NOT_FOUND"
    }

    def "adds Retry-After to the 503 only"() {
        expect:
        handler.handleDomainException(new NotificationException.NotificationDispatchException("m"), request)
                .headers.getFirst(HttpHeaders.RETRY_AFTER) == "30"
        handler.handleDomainException(new UserException.UserNotFoundException("m"), request)
                .headers.getFirst(HttpHeaders.RETRY_AFTER) == null
    }

    def "every ApiError exception type is registered on the domain handler"() {
        given:
        def registered = GlobalExceptionHandler.getDeclaredMethod(
                "handleDomainException", RuntimeException, HttpServletRequest)
                .getAnnotation(ExceptionHandler).value() as List

        expect:
        ApiError.values()*.exceptionType.every { it in registered }
        registered.size() == ApiError.values().length
    }

    def "maps HttpMessageNotReadableException to a 400 error response"() {
        when:
        def response = handler.handleUnreadableRequestException(
                new HttpMessageNotReadableException("bad json", Stub(HttpInputMessage)), request)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.errorCode == "INVALID_REQUEST_BODY"
        response.body.path == "/api/v1/things"
    }

    def "maps MethodArgumentNotValidException field errors into a validation error response"() {
        given:
        def bindingResult = Stub(BindingResult) {
            getFieldErrors() >> [
                    new FieldError("req", "name", "must not be empty"),
                    new FieldError("req", "email", "must be a well-formed email address")
            ]
        }

        when:
        def response = handler.handleValidationException(
                new MethodArgumentNotValidException(Stub(MethodParameter), bindingResult), request)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.errorCode == "VALIDATION_ERROR"
        response.body.validationErrors.size() == 2
        response.body.validationErrors.find { it.field == "name" }.message == "must not be empty"
    }

    def "maps MethodArgumentTypeMismatchException to a 400 error response"() {
        when:
        def response = handler.handleTypeMismatchException(
                new MethodArgumentTypeMismatchException("abc", Long, "id", null, null), request)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.errorCode == "INVALID_PARAMETER"
        response.body.errorMessage.contains("id")
    }

    def "maps DataIntegrityViolationException to a 409 error response"() {
        when:
        def response = handler.handleDataIntegrityViolationException(new DataIntegrityViolationException("dup"), request)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body.errorCode == "DATA_INTEGRITY_VIOLATION"
    }

    def "maps DataAccessException to a 500 error response without leaking the cause"() {
        when:
        def response = handler.handleDataAccessException(new DataAccessResourceFailureException("jdbc:secret"), request)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body.errorCode == "DATA_ACCESS_ERROR"
        !response.body.errorMessage.contains("secret")
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
