package com.beacon.service.handler

import com.beacon.exception.NotificationException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import spock.lang.Specification

class NotificationExceptionHandlerSpec extends Specification {

    NotificationExceptionHandler handler = new NotificationExceptionHandler()
    HttpServletRequest request = Stub() { getRequestURI() >> "/api/v1/notifications" }

    def "maps NotificationDispatchException to a 503 SERVICE_UNAVAILABLE error response"() {
        given:
        def exception = new NotificationException.NotificationDispatchException(
                "Failed to send notification for welcome at EMAIL. Please try again later.")

        when:
        def response = handler.handleNotificationDispatchException(exception, request)

        then:
        response.statusCode == HttpStatus.SERVICE_UNAVAILABLE
        response.body.errorCode == "NOTIFICATION_DISPATCH_FAILED"
        response.body.errorMessage == "Failed to send notification for welcome at EMAIL. Please try again later."
        response.body.path == "/api/v1/notifications"
    }

    def "maps NotificationNotAllowed to a 403 FORBIDDEN error response"() {
        given:
        def exception = new NotificationException.NotificationNotAllowed(
                "User with externalId: ext-1 has disabled all notifications for type welcome on channel EMAIL")

        when:
        def response = handler.handleNotificationNotAllowedException(exception, request)

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        response.body.errorCode == "NOTIFICATION_NOT_ALLOWED"
        response.body.errorMessage == "User with externalId: ext-1 has disabled all notifications for type welcome on channel EMAIL"
        response.body.path == "/api/v1/notifications"
    }

    def "maps BulkNotificationJobNotFound to a 404 NOT_FOUND error response"() {
        given:
        def jobId = UUID.randomUUID()
        def exception = new NotificationException.BulkNotificationJobNotFound(
                "No bulk notification job with id ${jobId} exists.")

        when:
        def response = handler.handleBulkNotificationJobNotFoundException(exception, request)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.errorCode == "BULK_NOTIFICATION_JOB_NOT_FOUND"
        response.body.errorMessage == "No bulk notification job with id ${jobId} exists."
        response.body.path == "/api/v1/notifications"
    }
}
