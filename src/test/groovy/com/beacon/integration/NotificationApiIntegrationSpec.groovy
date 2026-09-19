package com.beacon.integration

import com.beacon.model.NotificationContext
import com.beacon.model.entity.BulkNotificationJob
import com.beacon.model.entity.Template
import com.beacon.model.entity.User
import com.beacon.model.entity.UserPreference
import com.beacon.repository.BulkNotificationJobRepository
import com.beacon.repository.PreferenceRepository
import com.beacon.repository.TemplateRepository
import com.beacon.repository.UserRepository
import com.beacon.service.EmailNotificationAction
import tools.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.mockito.Mockito
import org.mockito.invocation.Invocation

import java.time.Instant

import static com.beacon.model.Types.Channel
import static com.beacon.model.Types.JobStatus
import static com.beacon.model.Types.PreferenceType
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.doReturn
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * The EMAIL/PUSH/SMS channel implementations are stubs that currently always
 * return false (they haven't been wired up to a real provider yet). We spy on
 * EmailNotificationAction to exercise the success path end-to-end, and rely
 * on the real (still-unimplemented) SMS action to exercise the dispatch
 * failure path exactly as a real client would see it today.
 */
class NotificationApiIntegrationSpec extends AbstractIntegrationSpec {

    @Autowired
    UserRepository userRepository

    @Autowired
    TemplateRepository templateRepository

    @Autowired
    PreferenceRepository preferenceRepository

    @Autowired
    BulkNotificationJobRepository jobRepository

    @Autowired
    ObjectMapper objectMapper

    @MockitoSpyBean
    EmailNotificationAction emailNotificationAction

    def "POST /api/v1/notifications resolves the template and dispatches it, returning 202"() {
        given:
        userRepository.save(new User(externalId: "ext-notify-1", name: "Ada", email: "ada@example.com", phone: "555-0100"))
        templateRepository.save(new Template(
                channel: Channel.EMAIL,
                notificationType: "welcome",
                body: "Hello {{name}}, welcome to Beacon!",
                subject: "Welcome"
        ))
        doReturn(true).when(emailNotificationAction).send(any())

        def payload = [
                userExternalId   : "ext-notify-1",
                channel          : "EMAIL",
                notificationType : "welcome",
                templateVariables: [name: "Ada"]
        ]

        expect:
        mockMvc.perform(post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted())

        and:
        List<Invocation> invocations = Mockito.mockingDetails(emailNotificationAction).invocations.findAll { it.method.name == "send" }
        invocations.size() == 1
        (invocations[0].arguments[0] as NotificationContext).name == "Ada"
        (invocations[0].arguments[0] as NotificationContext).message == "Hello Ada, welcome to Beacon!"
    }

    def "POST /api/v1/notifications returns 404 when the user does not exist"() {
        given:
        def payload = [userExternalId: "missing-user", channel: "EMAIL", notificationType: "welcome"]

        expect:
        mockMvc.perform(post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.errorCode').value("USER_NOT_FOUND"))
    }

    def "POST /api/v1/notifications returns 404 when no template matches the notification type and channel"() {
        given:
        userRepository.save(new User(externalId: "ext-notify-2", name: "Ada", email: "ada2@example.com"))
        def payload = [userExternalId: "ext-notify-2", channel: "EMAIL", notificationType: "unknown-type"]

        expect:
        mockMvc.perform(post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.errorCode').value("TEMPLATE_NOT_FOUND"))
    }

    def "POST /api/v1/notifications returns 503 when the channel cannot dispatch"() {
        given:
        userRepository.save(new User(externalId: "ext-notify-3", name: "Ada", phone: "555-0100", email: "ada3@example.com"))
        templateRepository.save(new Template(
                channel: Channel.SMS,
                notificationType: "otp",
                body: "Your code is {{code}}"
        ))
        def payload = [
                userExternalId   : "ext-notify-3",
                channel          : "SMS",
                notificationType : "otp",
                templateVariables: [code: "123456"]
        ]

        expect:
        mockMvc.perform(post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath('$.errorCode').value("NOTIFICATION_DISPATCH_FAILED"))
    }

    def "POST /api/v1/notifications returns 403 when the user has disabled the notification type and channel"() {
        given:
        def user = userRepository.save(new User(externalId: "ext-notify-4", name: "Ada", email: "ada4@example.com"))
        templateRepository.save(new Template(
                channel: Channel.EMAIL,
                notificationType: "welcome",
                body: "Hello {{name}}, welcome to Beacon!"
        ))
        preferenceRepository.save(new UserPreference(
                userId: user.id,
                notificationType: "welcome",
                channel: Channel.EMAIL,
                preference: PreferenceType.DISABLED
        ))
        def payload = [
                userExternalId   : "ext-notify-4",
                channel          : "EMAIL",
                notificationType : "welcome",
                templateVariables: [name: "Ada"]
        ]

        expect:
        mockMvc.perform(post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath('$.errorCode').value("NOTIFICATION_NOT_ALLOWED"))
    }

    def "POST /api/v1/notifications ignores a soft-deleted DISABLED preference"() {
        given: "a DISABLED preference that has since been soft deleted"
        def user = userRepository.save(new User(externalId: "ext-notify-9", name: "Ada", email: "ada9@example.com"))
        templateRepository.save(new Template(
                channel: Channel.EMAIL,
                notificationType: "welcome",
                body: "Hello {{name}}, welcome to Beacon!"
        ))
        preferenceRepository.save(new UserPreference(
                userId: user.id,
                notificationType: "welcome",
                channel: Channel.EMAIL,
                preference: PreferenceType.DISABLED,
                active: false
        ))
        doReturn(true).when(emailNotificationAction).send(any(NotificationContext))
        def payload = [
                userExternalId   : "ext-notify-9",
                channel          : "EMAIL",
                notificationType : "welcome",
                templateVariables: [name: "Ada"]
        ]

        expect: "the inactive row no longer blocks the send, so it is accepted rather than 403"
        mockMvc.perform(post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isAccepted())
    }

    def "POST /api/v1/notifications returns 400 for an invalid payload"() {
        given:
        def payload = [userExternalId: "", notificationType: ""]

        expect:
        mockMvc.perform(post("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.errorCode').value("VALIDATION_ERROR"))
    }

    def "GET /api/v1/notifications/bulk/{jobId} returns the job's status and counts"() {
        given:
        // flushed so Hibernate populates the @CreationTimestamp / @UpdateTimestamp columns
        def job = jobRepository.saveAndFlush(new BulkNotificationJob(
                actionCount: 10,
                successCount: 7,
                failureCount: 2,
                skippedCount: 1,
                status: JobStatus.PARTIALLY_COMPLETED,
                completedAt: Instant.parse("2026-09-18T10:15:30Z")
        ))

        expect:
        mockMvc.perform(get("/api/v1/notifications/bulk/{jobId}", job.id))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.jobId').value(job.id.toString()))
                .andExpect(jsonPath('$.status').value("PARTIALLY_COMPLETED"))
                .andExpect(jsonPath('$.actionCount').value(10))
                .andExpect(jsonPath('$.successCount').value(7))
                .andExpect(jsonPath('$.failureCount').value(2))
                .andExpect(jsonPath('$.skippedCount').value(1))
                .andExpect(jsonPath('$.createdAt').exists())
                .andExpect(jsonPath('$.updatedAt').exists())
                .andExpect(jsonPath('$.completedAt').exists())
    }

    def "GET /api/v1/notifications/bulk/{jobId} reports a pending job with zeroed counts and no completedAt"() {
        given:
        def job = jobRepository.saveAndFlush(new BulkNotificationJob(actionCount: 3, status: JobStatus.PENDING))

        expect:
        mockMvc.perform(get("/api/v1/notifications/bulk/{jobId}", job.id))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.status').value("PENDING"))
                .andExpect(jsonPath('$.actionCount').value(3))
                .andExpect(jsonPath('$.successCount').value(0))
                .andExpect(jsonPath('$.failureCount').value(0))
                .andExpect(jsonPath('$.skippedCount').value(0))
                .andExpect(jsonPath('$.completedAt').doesNotExist())
    }

    def "GET /api/v1/notifications/bulk/{jobId} returns 404 when no job has that id"() {
        given:
        def unknownJobId = UUID.randomUUID()

        expect:
        mockMvc.perform(get("/api/v1/notifications/bulk/{jobId}", unknownJobId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.errorCode').value("BULK_NOTIFICATION_JOB_NOT_FOUND"))
                .andExpect(jsonPath('$.errorMessage').value("No bulk notification job with id ${unknownJobId} exists.".toString()))
                .andExpect(jsonPath('$.path').value("/api/v1/notifications/bulk/${unknownJobId}".toString()))
    }
}
