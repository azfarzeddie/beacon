package com.beacon.service

import com.beacon.exception.NotificationException
import com.beacon.exception.TemplateException
import com.beacon.exception.UserException
import com.beacon.model.NotificationContext
import com.beacon.model.entity.Template
import com.beacon.model.entity.User
import com.beacon.model.request.SendNotificationRequest
import com.beacon.repository.TemplateRepository
import com.beacon.repository.UserRepository
import com.beacon.service.factory.NotificationActionFactory
import spock.lang.Specification
import spock.lang.Subject

import static com.beacon.model.Types.Channel

class NotificationServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    TemplateRepository templateRepository = Mock()
    NotificationActionFactory notificationActionFactory = Mock()
    TemplateService templateService = Mock()

    @Subject
    NotificationService notificationService =
            new NotificationService(userRepository, notificationActionFactory, templateRepository, templateService)

    private static User aUser() {
        new User(id: 1L, externalId: "ext-1", name: "Ada", email: "ada@example.com", phone: "555-0100")
    }

    private static Template aTemplate() {
        new Template(channel: Channel.EMAIL, notificationType: "welcome", body: "Hello {{name}}", subject: "Welcome!")
    }

    def "sendSingleNotification resolves the template and dispatches it through the matching channel"() {
        given:
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.EMAIL,
                notificationType: "welcome",
                templateVariables: [name: "Ada"],
                subject: "Welcome!"
        )
        def action = Mock(NotificationAction)

        when:
        notificationService.sendSingleNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(aTemplate())
        1 * templateService.resolveTemplate("Hello {{name}}", [name: "Ada"]) >> "Hello Ada"
        1 * notificationActionFactory.getAction(Channel.EMAIL) >> action
        1 * action.send({ NotificationContext ctx ->
            ctx.name == "Ada" && ctx.email == "ada@example.com" && ctx.phone == "555-0100" &&
                    ctx.message == "Hello Ada" && ctx.subject == "Welcome!"
        }) >> true
        noExceptionThrown()
    }

    def "sendSingleNotification throws UserNotFoundException when the user does not exist"() {
        given:
        def request = new SendNotificationRequest(userExternalId: "missing", channel: Channel.EMAIL, notificationType: "welcome")

        when:
        notificationService.sendSingleNotification(request)

        then:
        1 * userRepository.findByExternalId("missing") >> Optional.empty()
        0 * templateRepository.findByNotificationTypeAndChannel(*_)
        thrown(UserException.UserNotFoundException)
    }

    def "sendSingleNotification throws TemplateNotFound when no template matches the type and channel"() {
        given:
        def request = new SendNotificationRequest(userExternalId: "ext-1", channel: Channel.SMS, notificationType: "otp")

        when:
        notificationService.sendSingleNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * templateRepository.findByNotificationTypeAndChannel("otp", Channel.SMS) >> Optional.empty()
        0 * notificationActionFactory.getAction(_)
        thrown(TemplateException.TemplateNotFound)
    }

    def "sendSingleNotification wraps a template resolution failure as TemplateNotResolved"() {
        given:
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.EMAIL,
                notificationType: "welcome",
                templateVariables: [:]
        )

        when:
        notificationService.sendSingleNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(aTemplate())
        1 * templateService.resolveTemplate(*_) >> { throw new IllegalArgumentException("Unresolved template variable: name") }
        0 * notificationActionFactory.getAction(_)
        thrown(TemplateException.TemplateNotResolved)
    }

    def "sendSingleNotification throws NotificationDispatchException when the channel fails to send"() {
        given:
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.EMAIL,
                notificationType: "welcome",
                templateVariables: [name: "Ada"]
        )
        def action = Mock(NotificationAction)

        when:
        notificationService.sendSingleNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(aTemplate())
        1 * templateService.resolveTemplate(*_) >> "Hello Ada"
        1 * notificationActionFactory.getAction(Channel.EMAIL) >> action
        1 * action.send(_) >> false
        thrown(NotificationException.NotificationDispatchException)
    }
}
