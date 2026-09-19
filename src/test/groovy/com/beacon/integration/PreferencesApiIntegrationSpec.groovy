package com.beacon.integration

import com.beacon.repository.PreferenceRepository
import com.beacon.repository.UserRepository
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

class PreferencesApiIntegrationSpec extends AbstractIntegrationSpec {

    @Autowired
    UserRepository userRepository

    @Autowired
    PreferenceRepository preferenceRepository

    @Autowired
    ObjectMapper objectMapper

    private String createUser(String externalId) {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString([
                        externalId: externalId,
                        name      : "Test User",
                        email     : externalId + "@example.com"
                ])))
                .andExpect(status().isCreated())
        return externalId
    }

    private String createPreference(String userExternalId, String notificationType, String channel, String preference) {
        return mockMvc.perform(post("/api/v1/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString([
                        userExternalId  : userExternalId,
                        notificationType: notificationType,
                        channel         : channel,
                        preference      : preference
                ])))
                .andExpect(status().isCreated())
                .andReturn().response.contentAsString
    }

    def "POST /api/v1/preferences creates a preference for an existing user"() {
        given:
        def externalId = createUser("pref-user-100")

        expect:
        mockMvc.perform(post("/api/v1/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString([
                        userExternalId  : externalId,
                        notificationType: "welcome",
                        channel         : "EMAIL",
                        preference      : "ENABLED"
                ])))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/preferences/")))
                .andExpect(jsonPath('$.notificationType').value("welcome"))
                .andExpect(jsonPath('$.channel').value("EMAIL"))

        and:
        def userId = userRepository.findByExternalId(externalId).get().id
        preferenceRepository.findByUserIdAndNotificationTypeAndChannel(userId, "welcome", Channel.EMAIL).isPresent()
    }

    def "POST /api/v1/preferences lets a second user create a notificationType/channel another user already has"() {
        given:
        def firstUser = createUser("pref-user-600")
        def secondUser = createUser("pref-user-601")
        createPreference(firstUser, "welcome", "EMAIL", "ENABLED")

        when: "the second user asks for the same notificationType and channel"
        def result = mockMvc.perform(post("/api/v1/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString([
                        userExternalId  : secondUser,
                        notificationType: "welcome",
                        channel         : "EMAIL",
                        preference      : "DISABLED"
                ])))

        then: "it is created rather than rejected as a duplicate"
        result.andExpect(status().isCreated())
                .andExpect(jsonPath('$.notificationType').value("welcome"))
                .andExpect(jsonPath('$.channel').value("EMAIL"))

        and: "both users end up with their own row"
        def firstUserId = userRepository.findByExternalId(firstUser).get().id
        def secondUserId = userRepository.findByExternalId(secondUser).get().id
        preferenceRepository.findByUserIdAndNotificationTypeAndChannel(firstUserId, "welcome", Channel.EMAIL).isPresent()
        preferenceRepository.findByUserIdAndNotificationTypeAndChannel(secondUserId, "welcome", Channel.EMAIL).isPresent()
    }

    def "POST /api/v1/preferences returns 404 when the user does not exist"() {
        expect:
        mockMvc.perform(post("/api/v1/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString([
                        userExternalId  : "unknown-user",
                        notificationType: "welcome",
                        channel         : "EMAIL",
                        preference      : "ENABLED"
                ])))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.errorCode').value("USER_NOT_FOUND"))
    }

    def "POST /api/v1/preferences rejects a duplicate notificationType/channel combination for the same user with 409"() {
        given:
        def externalId = createUser("pref-user-200")
        createPreference(externalId, "otp", "SMS", "ENABLED")

        expect:
        mockMvc.perform(post("/api/v1/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString([
                        userExternalId  : externalId,
                        notificationType: "otp",
                        channel         : "SMS",
                        preference      : "DISABLED"
                ])))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.errorCode').value("PREFERENCE_ALREADY_EXISTS"))
    }

    def "GET /api/v1/preferences/{userExternalId} returns every preference for the user"() {
        given:
        def externalId = createUser("pref-user-300")
        createPreference(externalId, "welcome", "EMAIL", "ENABLED")
        createPreference(externalId, "otp", "SMS", "DISABLED")

        expect:
        mockMvc.perform(get("/api/v1/preferences/{userExternalId}", externalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.preferences.length()').value(2))
                .andExpect(jsonPath('$.preferences[*].notificationType').value(org.hamcrest.Matchers.containsInAnyOrder("welcome", "otp")))
    }

    def "GET /api/v1/preferences/{userExternalId} returns 200 and an empty list for a user with no preferences"() {
        given:
        def externalId = createUser("pref-user-700")

        expect: "an existing user with no rows is not a 404 - Spring Data returns an empty list, never an empty Optional"
        mockMvc.perform(get("/api/v1/preferences/{userExternalId}", externalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.preferences').isEmpty())
    }

    def "GET /api/v1/preferences/{userExternalId} returns 404 when the user does not exist"() {
        expect:
        mockMvc.perform(get("/api/v1/preferences/{userExternalId}", "unknown-user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.errorCode').value("USER_NOT_FOUND"))
    }

    def "GET /api/v1/preferences/{userExternalId}/{preferenceId} returns the matching preference"() {
        given:
        def externalId = createUser("pref-user-400")
        def createResponse = createPreference(externalId, "reminder", "PUSH", "ENABLED")
        def preferenceId = objectMapper.readTree(createResponse).get("id").asText()

        expect:
        mockMvc.perform(get("/api/v1/preferences/{userExternalId}/{preferenceId}", externalId, preferenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.id').value(preferenceId))
                .andExpect(jsonPath('$.notificationType').value("reminder"))
                .andExpect(jsonPath('$.channel').value("PUSH"))
                .andExpect(jsonPath('$.preference').value("ENABLED"))
                .andExpect(jsonPath('$.isActive').value(true))
    }

    def "GET /api/v1/preferences/{userExternalId}/{preferenceId} returns 404 when no preference matches"() {
        given:
        def externalId = createUser("pref-user-500")

        expect:
        mockMvc.perform(get("/api/v1/preferences/{userExternalId}/{preferenceId}", externalId, UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.errorCode').value("PREFERENCE_NOT_FOUND"))
    }
}
