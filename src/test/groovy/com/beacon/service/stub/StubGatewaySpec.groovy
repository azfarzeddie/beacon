package com.beacon.service.stub

import com.beacon.model.Types.Channel
import org.springframework.mail.MailSendException
import org.springframework.mail.SimpleMailMessage
import spock.lang.Specification

class StubGatewaySpec extends Specification {

    StubProperties properties = new StubProperties()
    StubGateway gateway

    def setup() {
        // no delay unless a test asks for one
        [properties.email, properties.sms, properties.push].each { it.medianMs = 0 }
        gateway = new StubGateway(properties)
    }

    def "accepts and records a message per channel"() {
        when:
        boolean ok = new StubSmsProvider(gateway).send("+15550100", "hi")
        new StubPushProvider(gateway).send("token-1", "ping")

        then:
        ok
        gateway.find(Channel.SMS, null, 10)*.to() == ["+15550100"]
        gateway.find(null, "token-1", 10)*.body() == ["ping"]
        gateway.stats()[Channel.SMS].accepted() == 1
        gateway.stats()[Channel.PUSH].accepted() == 1
    }

    def "blocks for roughly the configured latency"() {
        given:
        properties.sms.medianMs = 80
        properties.sms.sigma = 0

        when:
        long start = System.nanoTime()
        gateway.accept(Channel.SMS, "+1", null, "x")
        long elapsedMs = (System.nanoTime() - start) / 1_000_000

        then:
        elapsedMs >= 75
        elapsedMs < 400
        gateway.find(Channel.SMS, null, 1)[0].latencyMs() == 80
    }

    def "latency is capped at maxMs"() {
        given:
        def profile = new StubProperties.Profile(100, 3.0, 120, 0)

        expect:
        (1..2000).every { StubGateway.sampleLatencyMs(profile) <= 120 }
    }

    def "failure rate of 1 rejects everything and counts failures"() {
        given:
        properties.push.failureRate = 1.0

        expect:
        !new StubPushProvider(gateway).send("t", "m")
        gateway.stats()[Channel.PUSH].failed() == 1
        gateway.find(Channel.PUSH, null, 1)[0].accepted() == false
    }

    def "retention is bounded and newest messages come first"() {
        given:
        properties.retain = 3

        when:
        (1..5).each { gateway.accept(Channel.SMS, "n$it", null, "m") }

        then:
        gateway.find(null, null, 10)*.to() == ["n5", "n4", "n3"]
        gateway.stats()[Channel.SMS].accepted() == 5
    }

    def "reset clears messages and stats"() {
        given:
        gateway.accept(Channel.EMAIL, "a@b.c", "s", "m")

        when:
        gateway.reset()

        then:
        gateway.find(null, null, 10).isEmpty()
        gateway.stats()[Channel.EMAIL].accepted() == 0
    }

    def "stub mail sender goes through the gateway and surfaces rejection as MailSendException"() {
        given:
        def sender = new StubMailSender(gateway)
        def mail = new SimpleMailMessage(from: "from@example.com", to: ["to@example.com"] as String[],
                subject: "Hello", text: "Body text")

        when:
        sender.send(mail)

        then:
        with(gateway.find(Channel.EMAIL, null, 1)[0]) {
            to() == "to@example.com"
            subject() == "Hello"
            body().trim() == "Body text"
        }

        when:
        properties.email.failureRate = 1.0
        sender.send(mail)

        then:
        thrown(MailSendException)
    }
}
