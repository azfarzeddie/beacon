package com.beacon.integration

import com.beacon.model.NotificationContext
import com.beacon.model.entity.Template
import com.beacon.model.entity.User
import com.beacon.repository.TemplateRepository
import com.beacon.repository.UserRepository
import com.beacon.service.EmailNotificationAction
import tools.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.mockito.Mockito
import org.mockito.invocation.Invocation

import static com.beacon.model.Types.Channel
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when
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
        when(emailNotificationAction.send(any())).thenReturn(true)

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
}
