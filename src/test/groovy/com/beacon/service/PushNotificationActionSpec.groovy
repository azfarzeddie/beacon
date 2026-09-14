package com.beacon.service

import com.beacon.model.NotificationContext
import com.beacon.service.push.PushProvider
import spock.lang.Specification

class PushNotificationActionSpec extends Specification {

    private PushProvider providerNamed(String name) {
        Stub(PushProvider) { getProviderName() >> name }
    }

    def "send dispatches to the configured provider for every device token and returns true when at least one succeeds"() {
        given:
        def firebase = Mock(PushProvider) { getProviderName() >> "firebase" }
        def action = new PushNotificationAction([firebase])
        action.pushProvider = "firebase"
        action.validateConfiguredProvider()

        def context = NotificationContext.builder()
                .message("Your order shipped!")
                .deviceTokens(["token-1", "token-2"])
                .build()

        when:
        boolean result = action.send(context)

        then:
        1 * firebase.send("token-1", "Your order shipped!") >> false
        1 * firebase.send("token-2", "Your order shipped!") >> true
        result
    }

    def "send returns false when every device token fails to send"() {
        given:
        def firebase = Mock(PushProvider) { getProviderName() >> "firebase" }
        def action = new PushNotificationAction([firebase])
        action.pushProvider = "firebase"
        action.validateConfiguredProvider()

        def context = NotificationContext.builder()
                .message("Hi")
                .deviceTokens(["token-1", "token-2"])
                .build()

        when:
        boolean result = action.send(context)

        then:
        1 * firebase.send("token-1", "Hi") >> false
        1 * firebase.send("token-2", "Hi") >> false
        !result
    }

    def "send returns false without contacting the provider when the user has no device tokens"() {
        given:
        def firebase = Mock(PushProvider) { getProviderName() >> "firebase" }
        def action = new PushNotificationAction([firebase])
        action.pushProvider = "firebase"
        action.validateConfiguredProvider()

        def context = NotificationContext.builder().message("Hi").deviceTokens([]).build()

        when:
        boolean result = action.send(context)

        then:
        0 * firebase.send(_, _)
        !result
    }

    def "getChannel returns PUSH"() {
        given:
        def action = new PushNotificationAction([providerNamed("firebase")])

        expect:
        action.getChannel() == com.beacon.model.Types.Channel.PUSH
    }

    def "validateConfiguredProvider does not throw when the configured provider is registered"() {
        given:
        def action = new PushNotificationAction([providerNamed("firebase"), providerNamed("aws"), providerNamed("onesignal")])
        action.pushProvider = "onesignal"

        when:
        action.validateConfiguredProvider()

        then:
        noExceptionThrown()
    }

    def "validateConfiguredProvider throws IllegalStateException when the configured provider is not registered"() {
        given:
        def action = new PushNotificationAction([providerNamed("firebase")])
        action.pushProvider = "unknown"

        when:
        action.validateConfiguredProvider()

        then:
        def e = thrown(IllegalStateException)
        e.message.contains("unknown")
        e.message.contains("firebase")
    }
}
