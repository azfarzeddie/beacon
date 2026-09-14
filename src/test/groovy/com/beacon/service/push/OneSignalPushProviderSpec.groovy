package com.beacon.service.push

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import spock.lang.Specification
import spock.lang.Subject

import java.nio.charset.StandardCharsets

/**
 * OneSignalPushProvider builds its own RestClient internally, so - as with
 * TwilioSmsProvider - this points it at a real, throwaway HTTP server and
 * inspects the request it actually sent.
 */
class OneSignalPushProviderSpec extends Specification {

    HttpServer server
    HttpExchange capturedExchange
    String capturedBody
    int responseStatus = 200

    @Subject
    OneSignalPushProvider oneSignalPushProvider

    def setup() {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0)
        server.createContext("/") { HttpExchange exchange ->
            capturedExchange = exchange
            capturedBody = exchange.requestBody.getText(StandardCharsets.UTF_8.name())
            exchange.sendResponseHeaders(responseStatus, -1)
            exchange.close()
        }
        server.start()

        oneSignalPushProvider = new OneSignalPushProvider(
                "http://localhost:" + server.address.port,
                "app-123",
                "reskey-456"
        )
    }

    def cleanup() {
        server.stop(0)
    }

    def "getProviderName returns onesignal"() {
        expect:
        oneSignalPushProvider.getProviderName() == "onesignal"
    }

    def "send posts the notification to the OneSignal notifications endpoint and returns true on success"() {
        given:
        responseStatus = 200

        when:
        boolean result = oneSignalPushProvider.send("subscription-123", "Your order shipped!")

        then:
        result
        capturedExchange.requestURI.path == "/notifications"
        capturedExchange.requestMethod == "POST"
        capturedExchange.requestHeaders.getFirst("Authorization") == "Key reskey-456"
        capturedExchange.requestHeaders.getFirst("Content-Type").contains("application/json")
        capturedBody.contains('"app_id":"app-123"')
        capturedBody.contains('"include_subscription_ids":["subscription-123"]')
        capturedBody.contains('"contents":{"en":"Your order shipped!"}')
    }

    def "send returns false when OneSignal responds with an error status"() {
        given:
        responseStatus = 400

        when:
        boolean result = oneSignalPushProvider.send("subscription-123", "Hi")

        then:
        !result
    }

    def "send returns false when the server is unreachable"() {
        given:
        server.stop(0)

        when:
        boolean result = oneSignalPushProvider.send("subscription-123", "Hi")

        then:
        !result
    }
}
