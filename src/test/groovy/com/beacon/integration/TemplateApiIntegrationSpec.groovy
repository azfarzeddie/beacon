package com.beacon.integration

import com.beacon.repository.TemplateRepository
import tools.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

import static com.beacon.model.Types.Channel
import static org.hamcrest.Matchers.containsString
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TemplateApiIntegrationSpec extends AbstractIntegrationSpec {

    @Autowired
    TemplateRepository templateRepository

    @Autowired
    ObjectMapper objectMapper

    def "POST /api/v1/templates creates a template"() {
        given:
        def payload = [
                templateBody    : "Hello {{name}}, welcome!",
                notificationType: "welcome",
                channel         : "EMAIL",
                subject         : "Welcome!"
        ]

        expect:
        mockMvc.perform(post("/api/v1/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/templates/")))
                .andExpect(jsonPath('$.channel').value("EMAIL"))
                .andExpect(jsonPath('$.notificationType').value("welcome"))

        and:
        templateRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL).isPresent()
    }

    def "POST /api/v1/templates rejects a duplicate notificationType/channel combination with 409"() {
        given:
        def payload = [templateBody: "Your code is {{code}}", notificationType: "otp", channel: "SMS"]
        mockMvc.perform(post("/api/v1/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())

        expect:
        mockMvc.perform(post("/api/v1/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.errorCode').value("TEMPLATE_ALREADY_EXISTS"))
    }

    def "POST /api/v1/templates rejects an invalid payload with 400"() {
        given:
        def payload = [templateBody: "", notificationType: "", channel: "EMAIL"]

        expect:
        mockMvc.perform(post("/api/v1/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.errorCode').value("VALIDATION_ERROR"))
    }

    def "GET /api/v1/templates/{notificationType}/{channel} returns the matching template"() {
        given:
        mockMvc.perform(post("/api/v1/templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString([
                        templateBody    : "Hi {{name}}",
                        notificationType: "reminder",
                        channel         : "PUSH",
                        subject         : "Reminder"
                ])))
                .andExpect(status().isCreated())

        expect:
        mockMvc.perform(get("/api/v1/templates/{notificationType}/{channel}", "reminder", "PUSH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.notificationType').value("reminder"))
                .andExpect(jsonPath('$.channel').value("PUSH"))
                .andExpect(jsonPath('$.body').value("Hi {{name}}"))
                .andExpect(jsonPath('$.subject').value("Reminder"))
                .andExpect(jsonPath('$.createdAt').exists())
                .andExpect(jsonPath('$.updatedAt').exists())
    }

    def "GET /api/v1/templates/{notificationType}/{channel} returns 404 when no template matches"() {
        expect:
        mockMvc.perform(get("/api/v1/templates/{notificationType}/{channel}", "unknown", "EMAIL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.errorCode').value("TEMPLATE_NOT_FOUND"))
    }
}
