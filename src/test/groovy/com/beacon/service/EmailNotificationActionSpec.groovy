package com.beacon.service

import com.beacon.model.NotificationContext
import org.springframework.mail.MailSendException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import spock.lang.Specification
import spock.lang.Subject

import static com.beacon.model.Types.Channel

class EmailNotificationActionSpec extends Specification {

    JavaMailSender mailSender = Mock()

    @Subject
    EmailNotificationAction emailNotificationAction = new EmailNotificationAction(mailSender)

    def setup() {
        emailNotificationAction.sender = "noreply@beacon.io"
    }

    def "getChannel returns EMAIL"() {
        expect:
        emailNotificationAction.getChannel() == Channel.EMAIL
    }

    def "send builds a mail message from the notification context and dispatches it, returning true"() {
        given:
        def context = NotificationContext.builder()
                .name("Ada")
                .email("ada@example.com")
                .message("Hello Ada, welcome to Beacon!")
                .subject("Welcome!")
                .build()

        when:
        boolean result = emailNotificationAction.send(context)

        then:
        1 * mailSender.send({ SimpleMailMessage message ->
            message.from == "noreply@beacon.io" &&
                    message.to == ["ada@example.com"] as String[] &&
                    message.subject == "Welcome!" &&
                    message.text == "Hello Ada, welcome to Beacon!"
        })
        result
    }

    def "send returns false when the mail server rejects the message"() {
        given:
        def context = NotificationContext.builder()
                .name("Ada")
                .email("ada@example.com")
                .message("Hello Ada, welcome to Beacon!")
                .subject("Welcome!")
                .build()

        when:
        boolean result = emailNotificationAction.send(context)

        then:
        1 * mailSender.send(_ as SimpleMailMessage) >> { throw new MailSendException("Mail server unavailable") }
        !result
    }
}
