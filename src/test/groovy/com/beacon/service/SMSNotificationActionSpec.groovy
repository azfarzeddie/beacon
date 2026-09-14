package com.beacon.service

import com.beacon.model.NotificationContext
import com.beacon.service.sms.SmsProvider
import spock.lang.Specification

class SMSNotificationActionSpec extends Specification {

    private SmsProvider providerNamed(String name) {
        Stub(SmsProvider) { getProviderName() >> name }
    }

    def "send dispatches to the configured provider with the phone and message, and returns its result"() {
        given:
        def twilio = Mock(SmsProvider) { getProviderName() >> "twilio" }
        def aws = providerNamed("aws")
        def action = new SMSNotificationAction([twilio, aws])
        action.smsProvider = "twilio"
        action.validateConfiguredProvider()

        def context = NotificationContext.builder().phone("+15550100").message("Your code is 123456").build()

        when:
        boolean result = action.send(context)

        then:
        1 * twilio.send("+15550100", "Your code is 123456") >> true
        result
    }

    def "send returns false when the configured provider fails to send"() {
        given:
        def twilio = Mock(SmsProvider) { getProviderName() >> "twilio" }
        def action = new SMSNotificationAction([twilio])
        action.smsProvider = "twilio"
        action.validateConfiguredProvider()

        def context = NotificationContext.builder().phone("+15550100").message("Hi").build()

        when:
        boolean result = action.send(context)

        then:
        1 * twilio.send("+15550100", "Hi") >> false
        !result
    }

    def "getChannel returns SMS"() {
        given:
        def action = new SMSNotificationAction([providerNamed("twilio")])

        expect:
        action.getChannel() == com.beacon.model.Types.Channel.SMS
    }

    def "validateConfiguredProvider does not throw when the configured provider is registered"() {
        given:
        def action = new SMSNotificationAction([providerNamed("twilio"), providerNamed("aws")])
        action.smsProvider = "aws"

        when:
        action.validateConfiguredProvider()

        then:
        noExceptionThrown()
    }

    def "validateConfiguredProvider throws IllegalStateException when the configured provider is not registered"() {
        given:
        def action = new SMSNotificationAction([providerNamed("twilio")])
        action.smsProvider = "vonage"

        when:
        action.validateConfiguredProvider()

        then:
        def e = thrown(IllegalStateException)
        e.message.contains("vonage")
        e.message.contains("twilio")
    }
}
