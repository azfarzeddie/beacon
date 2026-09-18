package com.beacon.service

import com.beacon.exception.NotificationException
import com.beacon.exception.TemplateException
import com.beacon.exception.UserException
import com.beacon.model.MessageDetails
import com.beacon.model.NotificationContext
import com.beacon.model.entity.*
import com.beacon.model.request.SendNotificationRequest
import com.beacon.repository.*
import com.beacon.service.factory.NotificationActionFactory
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant

import static com.beacon.model.Types.*

class NotificationServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    TemplateRepository templateRepository = Mock()
    NotificationActionFactory notificationActionFactory = Mock()
    TemplateService templateService = Mock()
    PreferenceRepository preferenceRepository = Mock()
    BulkNotificationJobRepository jobRepository = Mock()
    NotificationActionRecordRepository actionRepository = Mock()

    @Subject
    NotificationService notificationService = new NotificationService(userRepository, notificationActionFactory,
            templateRepository, templateService, preferenceRepository, jobRepository, actionRepository)

    private static User aUser() {
        new User(id: 1L, externalId: "ext-1", name: "Ada", email: "ada@example.com", phone: "555-0100")
    }

    private static Template aTemplate() {
        new Template(channel: Channel.EMAIL, notificationType: "welcome", body: "Hello {{name}}", subject: "Welcome!")
    }

    def "sendNotification resolves the template and dispatches it through the matching channel"() {
        given:
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.EMAIL,
                notificationType: "welcome",
                templateVariables: [name: "Ada"]
        )
        def action = Mock(NotificationAction)

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "welcome", Channel.EMAIL) >> Optional.empty()
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(aTemplate())
        1 * templateService.resolveTemplate("Hello {{name}}", [name: "Ada"]) >> "Hello Ada"
        1 * templateService.resolveTemplate("Welcome!", [name: "Ada"]) >> "Welcome!"
        1 * notificationActionFactory.getAction(Channel.EMAIL) >> action
        1 * action.send({ NotificationContext ctx ->
            ctx.name == "Ada" && ctx.email == "ada@example.com" && ctx.phone == "555-0100" &&
                    ctx.message == "Hello Ada" && ctx.subject == "Welcome!"
        }) >> true
        noExceptionThrown()
    }

    def "sendNotification throws UserNotFoundException when the user does not exist"() {
        given:
        def request = new SendNotificationRequest(userExternalId: "missing", channel: Channel.EMAIL, notificationType: "welcome")

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("missing") >> Optional.empty()
        0 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(*_)
        0 * templateRepository.findByNotificationTypeAndChannel(*_)
        thrown(UserException.UserNotFoundException)
    }

    def "sendNotification throws TemplateNotFound when no template matches the type and channel"() {
        given:
        def request = new SendNotificationRequest(userExternalId: "ext-1", channel: Channel.SMS, notificationType: "otp")

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "otp", Channel.SMS) >> Optional.empty()
        1 * templateRepository.findByNotificationTypeAndChannel("otp", Channel.SMS) >> Optional.empty()
        0 * notificationActionFactory.getAction(_)
        thrown(TemplateException.TemplateNotFound)
    }

    def "sendNotification wraps a template resolution failure as TemplateNotResolved"() {
        given:
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.EMAIL,
                notificationType: "welcome",
                templateVariables: new HashMap<String, String>()
        )

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "welcome", Channel.EMAIL) >> Optional.empty()
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(aTemplate())
        1 * templateService.resolveTemplate(*_) >> { throw new IllegalArgumentException("Unresolved template variable: name") }
        0 * notificationActionFactory.getAction(_)
        thrown(TemplateException.TemplateNotResolved)
    }

    def "sendNotification throws NotificationDispatchException when the channel fails to send"() {
        given:
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.EMAIL,
                notificationType: "welcome",
                templateVariables: [name: "Ada"]
        )
        def action = Mock(NotificationAction)

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "welcome", Channel.EMAIL) >> Optional.empty()
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(aTemplate())
        1 * templateService.resolveTemplate("Hello {{name}}", [name: "Ada"]) >> "Hello Ada"
        1 * templateService.resolveTemplate("Welcome!", [name: "Ada"]) >> "Welcome!"
        1 * notificationActionFactory.getAction(Channel.EMAIL) >> action
        1 * action.send(_) >> false
        thrown(NotificationException.NotificationDispatchException)
    }

    def "sendNotification proceeds when the user has an ENABLED preference for the type and channel"() {
        given:
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.EMAIL,
                notificationType: "welcome",
                templateVariables: [name: "Ada"]
        )
        def action = Mock(NotificationAction)
        def preference = new UserPreference(userId: 1L, notificationType: "welcome", channel: Channel.EMAIL, preference: PreferenceType.ENABLED)

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "welcome", Channel.EMAIL) >> Optional.of(preference)
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(aTemplate())
        1 * templateService.resolveTemplate("Hello {{name}}", [name: "Ada"]) >> "Hello Ada"
        1 * templateService.resolveTemplate("Welcome!", [name: "Ada"]) >> "Welcome!"
        1 * notificationActionFactory.getAction(Channel.EMAIL) >> action
        1 * action.send(_) >> true
        noExceptionThrown()
    }

    def "sendNotification sends the notification when no preference exists for the user, type, and channel"() {
        given:
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.EMAIL,
                notificationType: "welcome",
                templateVariables: [name: "Ada"]
        )
        def action = Mock(NotificationAction)

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "welcome", Channel.EMAIL) >> Optional.empty()
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(aTemplate())
        1 * templateService.resolveTemplate("Hello {{name}}", [name: "Ada"]) >> "Hello Ada"
        1 * templateService.resolveTemplate("Welcome!", [name: "Ada"]) >> "Welcome!"
        1 * notificationActionFactory.getAction(Channel.EMAIL) >> action
        1 * action.send(_) >> true
        noExceptionThrown()
    }

    def "sendNotification resolves the subject stored on the template"() {
        given:
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.EMAIL,
                notificationType: "welcome",
                templateVariables: [name: "Ada"]
        )
        def action = Mock(NotificationAction)

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "welcome", Channel.EMAIL) >> Optional.empty()
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(aTemplate())
        1 * templateService.resolveTemplate("Hello {{name}}", [name: "Ada"]) >> "Hello Ada"
        1 * templateService.resolveTemplate("Welcome!", [name: "Ada"]) >> "Welcome!"
        1 * notificationActionFactory.getAction(Channel.EMAIL) >> action
        1 * action.send({ NotificationContext ctx -> ctx.subject == "Welcome!" }) >> true
        noExceptionThrown()
    }

    def "sendNotification leaves the subject unresolved when the template has no subject"() {
        given:
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.EMAIL,
                notificationType: "welcome",
                templateVariables: [name: "Ada"]
        )
        def action = Mock(NotificationAction)
        def template = new Template(channel: Channel.EMAIL, notificationType: "welcome", body: "Hello {{name}}")

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "welcome", Channel.EMAIL) >> Optional.empty()
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(template)
        1 * templateService.resolveTemplate("Hello {{name}}", [name: "Ada"]) >> "Hello Ada"
        0 * templateService.resolveTemplate(null, _)
        1 * notificationActionFactory.getAction(Channel.EMAIL) >> action
        1 * action.send({ NotificationContext ctx -> ctx.subject == null }) >> true
        noExceptionThrown()
    }

    def "sendNotification includes the user's device tokens in the notification context"() {
        given:
        def user = aUser()
        user.deviceTokens = [
                new DeviceToken(token: "device-token-1"),
                new DeviceToken(token: "device-token-2")
        ]
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.PUSH,
                notificationType: "welcome",
                templateVariables: [name: "Ada"]
        )
        def action = Mock(NotificationAction)
        def template = new Template(channel: Channel.PUSH, notificationType: "welcome", body: "Hello {{name}}")

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(user)
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "welcome", Channel.PUSH) >> Optional.empty()
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.PUSH) >> Optional.of(template)
        1 * templateService.resolveTemplate("Hello {{name}}", [name: "Ada"]) >> "Hello Ada"
        1 * notificationActionFactory.getAction(Channel.PUSH) >> action
        1 * action.send({ NotificationContext ctx -> ctx.deviceTokens == ["device-token-1", "device-token-2"] }) >> true
        noExceptionThrown()
    }

    def "sendNotification throws NotificationNotAllowed when the user has disabled the type and channel"() {
        given:
        def request = new SendNotificationRequest(userExternalId: "ext-1", channel: Channel.EMAIL, notificationType: "welcome")
        def preference = new UserPreference(userId: 1L, notificationType: "welcome", channel: Channel.EMAIL, preference: PreferenceType.DISABLED)

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "welcome", Channel.EMAIL) >> Optional.of(preference)
        0 * templateRepository.findByNotificationTypeAndChannel(*_)
        0 * notificationActionFactory.getAction(_)
        thrown(NotificationException.NotificationNotAllowed)
    }

    def "sendNotification records a SUCCESS action with the resolved message details"() {
        given:
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.EMAIL,
                notificationType: "welcome",
                templateVariables: [name: "Ada"]
        )
        def action = Mock(NotificationAction)
        NotificationActionRecord saved = null

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "welcome", Channel.EMAIL) >> Optional.empty()
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(aTemplate())
        1 * templateService.resolveTemplate("Hello {{name}}", [name: "Ada"]) >> "Hello Ada"
        1 * templateService.resolveTemplate("Welcome!", [name: "Ada"]) >> "Welcome!"
        1 * notificationActionFactory.getAction(Channel.EMAIL) >> action
        1 * action.send(_) >> true
        1 * actionRepository.save(_) >> { NotificationActionRecord record -> saved = record }

        and:
        saved.status == ActionStatus.SUCCESS
        saved.job == null
        saved.userExternalId == "ext-1"
        saved.messageDetails == new MessageDetails("Hello Ada", "Welcome!")
        saved.failureReason == null
        saved.deviceTokens == null
    }

    def "sendNotification records device tokens only for PUSH"() {
        given:
        def user = aUser()
        user.deviceTokens = [new DeviceToken(token: "device-token-1")]
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.PUSH,
                notificationType: "welcome",
                templateVariables: [name: "Ada"]
        )
        def action = Mock(NotificationAction)
        def template = new Template(channel: Channel.PUSH, notificationType: "welcome", body: "Hello {{name}}")
        NotificationActionRecord saved = null

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(user)
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "welcome", Channel.PUSH) >> Optional.empty()
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.PUSH) >> Optional.of(template)
        1 * templateService.resolveTemplate("Hello {{name}}", [name: "Ada"]) >> "Hello Ada"
        1 * notificationActionFactory.getAction(Channel.PUSH) >> action
        1 * action.send(_) >> true
        1 * actionRepository.save(_) >> { NotificationActionRecord record -> saved = record }

        and:
        saved.status == ActionStatus.SUCCESS
        saved.deviceTokens == ["device-token-1"]
    }

    def "sendNotification records a SKIPPED action and rethrows when the user has disabled the channel"() {
        given:
        def request = new SendNotificationRequest(userExternalId: "ext-1", channel: Channel.EMAIL, notificationType: "welcome")
        def preference = new UserPreference(userId: 1L, notificationType: "welcome", channel: Channel.EMAIL, preference: PreferenceType.DISABLED)
        NotificationActionRecord saved = null

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "welcome", Channel.EMAIL) >> Optional.of(preference)
        1 * actionRepository.save(_) >> { NotificationActionRecord record -> saved = record }
        thrown(NotificationException.NotificationNotAllowed)

        and:
        saved.status == ActionStatus.SKIPPED
        saved.failureReason.contains("has disabled all notifications")
        saved.messageDetails == null
    }

    def "sendNotification records a FAILED action and rethrows when the channel fails to send"() {
        given:
        def request = new SendNotificationRequest(
                userExternalId: "ext-1",
                channel: Channel.EMAIL,
                notificationType: "welcome",
                templateVariables: [name: "Ada"]
        )
        def action = Mock(NotificationAction)
        NotificationActionRecord saved = null

        when:
        notificationService.sendNotification(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(aUser())
        1 * preferenceRepository.findByUserIdAndNotificationTypeAndChannel(1L, "welcome", Channel.EMAIL) >> Optional.empty()
        1 * templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(aTemplate())
        1 * templateService.resolveTemplate("Hello {{name}}", [name: "Ada"]) >> "Hello Ada"
        1 * templateService.resolveTemplate("Welcome!", [name: "Ada"]) >> "Welcome!"
        1 * notificationActionFactory.getAction(Channel.EMAIL) >> action
        1 * action.send(_) >> false
        1 * actionRepository.save(_) >> { NotificationActionRecord record -> saved = record }
        thrown(NotificationException.NotificationDispatchException)

        and:
        saved.status == ActionStatus.FAILED
        saved.failureReason.contains("Failed to send notification")
        saved.messageDetails == null
    }

    def "getBulkNotificationJob returns the job's status, counts and timestamps"() {
        given:
        def jobId = UUID.randomUUID()
        def completedAt = Instant.parse("2026-09-18T10:15:30Z")
        def job = new BulkNotificationJob(
                id: jobId,
                status: JobStatus.PARTIALLY_COMPLETED,
                actionCount: 10,
                successCount: 7,
                failureCount: 2,
                skippedCount: 1,
                createdAt: completedAt.minusSeconds(60),
                updatedAt: completedAt,
                completedAt: completedAt
        )

        when:
        def response = notificationService.getBulkNotificationJob(jobId)

        then:
        1 * jobRepository.findById(jobId) >> Optional.of(job)

        and:
        response.jobId == jobId
        response.status == JobStatus.PARTIALLY_COMPLETED
        response.actionCount == 10
        response.successCount == 7
        response.failureCount == 2
        response.skippedCount == 1
        response.createdAt == completedAt.minusSeconds(60)
        response.updatedAt == completedAt
        response.completedAt == completedAt
    }

    def "getBulkNotificationJob reports a job that is still running with no completedAt"() {
        given:
        def jobId = UUID.randomUUID()
        def job = new BulkNotificationJob(id: jobId, status: JobStatus.IN_PROGRESS, actionCount: 5)

        when:
        def response = notificationService.getBulkNotificationJob(jobId)

        then:
        1 * jobRepository.findById(jobId) >> Optional.of(job)

        and:
        response.status == JobStatus.IN_PROGRESS
        response.actionCount == 5
        response.successCount == 0
        response.failureCount == 0
        response.skippedCount == 0
        response.completedAt == null
    }

    def "getBulkNotificationJob throws BulkNotificationJobNotFound when no job has that id"() {
        given:
        def jobId = UUID.randomUUID()

        when:
        notificationService.getBulkNotificationJob(jobId)

        then:
        1 * jobRepository.findById(jobId) >> Optional.empty()
        def e = thrown(NotificationException.BulkNotificationJobNotFound)
        e.message == "No bulk notification job with id ${jobId} exists."
    }
}
