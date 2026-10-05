package com.beacon.integration

import com.beacon.model.entity.DeviceToken
import com.beacon.model.entity.Template
import com.beacon.model.entity.User
import com.beacon.repository.TemplateRepository
import com.beacon.repository.UserRepository
import com.beacon.service.stub.StubGateway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper

import static com.beacon.model.Types.Channel
import static com.beacon.model.Types.Platform
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Full notification flow for every channel against the stub providers (no real provider involved). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = [
        "SMS_PROVIDER=stub", "PUSH_PROVIDER=stub", "MAIL_PROVIDER=stub",
        "STUB_EMAIL_MEDIAN_MS=0", "STUB_SMS_MEDIAN_MS=0", "STUB_PUSH_MEDIAN_MS=0"
])
class StubProvidersIntegrationSpec extends AbstractIntegrationSpec {

    @Autowired UserRepository userRepository
    @Autowired TemplateRepository templateRepository
    @Autowired StubGateway gateway
    @Autowired ObjectMapper objectMapper

    def setup() {
        gateway.reset()
    }

    def "#channel notification is delivered to the stub and visible via /stub/notifications"() {
        given:
        def user = new User(externalId: "ext-stub-1", name: "Ada", email: "ada@example.com", phone: "+15550100")
        user.deviceTokens = [new DeviceToken(token: "tok-1", platform: Platform.ANDROID, user: user)]
        userRepository.save(user)
        templateRepository.save(new Template(channel: channel, notificationType: "welcome",
                body: "Hello {{name}}", subject: "Welcome"))

        expect:
        mockMvc.perform(post("/api/v1/notifications").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString([userExternalId: "ext-stub-1", channel: channel.name(),
                                                          notificationType: "welcome", templateVariables: [name: "Ada"]])))
                .andExpect(status().isAccepted())

        and:
        mockMvc.perform(get("/stub/notifications").param("channel", channel.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$[0].to').value(to))
                .andExpect(jsonPath('$[0].accepted').value(true))
        mockMvc.perform(get("/stub/stats")).andExpect(jsonPath("\$.${channel.name()}.accepted").value(1))

        where:
        channel       | to
        Channel.EMAIL | "ada@example.com"
        Channel.SMS   | "+15550100"
        Channel.PUSH  | "tok-1"
    }
}
