package com.beacon.service.sms

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import spock.lang.Specification
import spock.lang.Subject

import java.nio.charset.StandardCharsets

/**
 * TwilioSmsProvider builds its own RestClient internally, so rather than
 * mocking RestClient's fluent API, this spec points it at a real, throwaway
 * HTTP server and inspects the request it actually sent - exactly the use
 * case spring.sms.twilio.base-url was made configurable for.
 */
class TwilioSmsProviderSpec extends Specification {

    HttpServer server
    HttpExchange capturedExchange
    String capturedBody
    int responseStatus = 201

    @Subject
    TwilioSmsProvider twilioSmsProvider

    def setup() {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0)
        server.createContext("/") { HttpExchange exchange ->
            capturedExchange = exchange
            capturedBody = exchange.requestBody.getText(StandardCharsets.UTF_8.name())
            exchange.sendResponseHeaders(responseStatus, -1)
            exchange.close()
        }
        server.start()

        twilioSmsProvider = new TwilioSmsProvider(
                "http://localhost:" + server.address.port,
                "ACtest123",
                "authtoken123",
                "+15550199"
        )
    }

    def cleanup() {
        server.stop(0)
    }

    def "getProviderName returns twilio"() {
        expect:
        twilioSmsProvider.getProviderName() == "twilio"
    }

    def "send posts the message to the Twilio messages endpoint with basic auth and returns true on success"() {
        given:
        responseStatus = 201

        when:
        boolean result = twilioSmsProvider.send("+15550100", "Your code is 123456")

        then:
        result
        capturedExchange.requestURI.path == "/Accounts/ACtest123/Messages.json"
        capturedExchange.requestMethod == "POST"
        capturedExchange.requestHeaders.getFirst("Authorization") == "Basic " +
                Base64.encoder.encodeToString("ACtest123:authtoken123".getBytes(StandardCharsets.UTF_8))
        capturedBody.contains("To=%2B15550100")
        capturedBody.contains("From=%2B15550199")
        capturedBody.contains("Body=Your+code+is+123456")
    }

    def "send returns false when Twilio responds with an error status"() {
        given:
        responseStatus = 400

        when:
        boolean result = twilioSmsProvider.send("+15550100", "Your code is 123456")

        then:
        !result
    }

    def "send returns false when the server is unreachable"() {
        given:
        server.stop(0)

        when:
        boolean result = twilioSmsProvider.send("+15550100", "Your code is 123456")

        then:
        !result
    }
}
