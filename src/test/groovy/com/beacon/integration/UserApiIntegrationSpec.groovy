package com.beacon.integration

import com.beacon.repository.UserRepository
import tools.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

import static com.beacon.model.Types.Platform
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.hasItems
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserApiIntegrationSpec extends AbstractIntegrationSpec {

    @Autowired
    UserRepository userRepository

    @Autowired
    ObjectMapper objectMapper

    def "POST /api/v1/users creates a user together with its device tokens"() {
        given:
        def payload = [
                externalId  : "ext-100",
                name        : "Grace Hopper",
                email       : "grace@example.com",
                phone       : "555-0101",
                deviceTokens: [[token: "tok-1", platform: Platform.ANDROID.name()]]
        ]

        expect:
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/users/")))
                .andExpect(jsonPath('$.externalId').value("ext-100"))
                .andExpect(jsonPath('$.name').value("Grace Hopper"))
                .andExpect(jsonPath('$.email').value("grace@example.com"))
                .andExpect(jsonPath('$.phone').value("555-0101"))

        and:
        userRepository.findByExternalId("ext-100").isPresent()
    }

    def "POST /api/v1/users rejects a duplicate externalId with 409"() {
        given:
        def payload = [externalId: "ext-200", name: "Ada", email: "ada@example.com"]
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())

        expect:
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.errorCode').value("USER_ALREADY_EXISTS"))
    }

    def "POST /api/v1/users rejects an invalid payload with 400 and field-level errors"() {
        given:
        def payload = [externalId: "ext-300", name: "", email: "not-an-email"]

        expect:
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.errorCode').value("VALIDATION_ERROR"))
                .andExpect(jsonPath('$.validationErrors[*].field').value(hasItems("name", "email")))
    }

    def "GET /api/v1/users/{id} returns the user with its device tokens"() {
        given:
        def createResponse = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString([
                        externalId  : "ext-400",
                        name        : "Katherine Johnson",
                        email       : "katherine@example.com",
                        deviceTokens: [[token: "tok-9", platform: Platform.IOS.name()]]
                ])))
                .andReturn().response.contentAsString
        def userId = objectMapper.readTree(createResponse).get("id").asLong()

        expect:
        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.externalId').value("ext-400"))
                .andExpect(jsonPath('$.name').value("Katherine Johnson"))
                .andExpect(jsonPath('$.deviceTokens[0].token').value("tok-9"))
                .andExpect(jsonPath('$.deviceTokens[0].platform').value("IOS"))
    }

    def "GET /api/v1/users/{id} returns 404 when the user does not exist"() {
        expect:
        mockMvc.perform(get("/api/v1/users/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.errorCode').value("USER_NOT_FOUND"))
    }
}
