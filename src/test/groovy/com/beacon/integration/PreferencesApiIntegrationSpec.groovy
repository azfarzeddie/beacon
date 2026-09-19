package com.beacon.integration

import com.beacon.repository.PreferenceRepository
import com.beacon.repository.UserRepository
import tools.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

import static com.beacon.model.Types.Channel
import static org.hamcrest.Matchers.containsString
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
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

    private String idOf(String createResponseBody) {
        return objectMapper.readTree(createResponseBody).get("id").asText()
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

    def "PUT /api/v1/preferences/{userExternalId}/{preferenceId} updates the preference value"() {
        given:
        def externalId = createUser("pref-user-800")
        def preferenceId = idOf(createPreference(externalId, "welcome", "EMAIL", "ENABLED"))

        when:
        def result = mockMvc.perform(put("/api/v1/preferences/{userExternalId}/{preferenceId}", externalId, preferenceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString([preference: "DISABLED"])))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.id').value(preferenceId))
                .andExpect(jsonPath('$.preference').value("DISABLED"))
                .andExpect(jsonPath('$.isActive').value(true))

        and: "the change is visible on a subsequent read"
        mockMvc.perform(get("/api/v1/preferences/{userExternalId}/{preferenceId}", externalId, preferenceId))
                .andExpect(jsonPath('$.preference').value("DISABLED"))
    }

    def "PUT /api/v1/preferences/{userExternalId}/{preferenceId} returns 400 when the preference value is missing"() {
        given:
        def externalId = createUser("pref-user-810")
        def preferenceId = idOf(createPreference(externalId, "welcome", "EMAIL", "ENABLED"))

        expect:
        mockMvc.perform(put("/api/v1/preferences/{userExternalId}/{preferenceId}", externalId, preferenceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.errorCode').value("VALIDATION_ERROR"))
    }

    def "PUT /api/v1/preferences/{userExternalId}/{preferenceId} returns 404 for an unknown preference"() {
        given:
        def externalId = createUser("pref-user-820")

        expect:
        mockMvc.perform(put("/api/v1/preferences/{userExternalId}/{preferenceId}", externalId, UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString([preference: "DISABLED"])))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.errorCode').value("PREFERENCE_NOT_FOUND"))
    }

    def "DELETE /api/v1/preferences/{userExternalId}/{preferenceId} soft deletes and hides the preference"() {
        given:
        def externalId = createUser("pref-user-900")
        def preferenceId = idOf(createPreference(externalId, "welcome", "EMAIL", "DISABLED"))

        when:
        def result = mockMvc.perform(delete("/api/v1/preferences/{userExternalId}/{preferenceId}", externalId, preferenceId))

        then:
        result.andExpect(status().isNoContent())

        and: "it disappears from both read endpoints"
        mockMvc.perform(get("/api/v1/preferences/{userExternalId}/{preferenceId}", externalId, preferenceId))
                .andExpect(status().isNotFound())
        mockMvc.perform(get("/api/v1/preferences/{userExternalId}", externalId))
                .andExpect(jsonPath('$.preferences').isEmpty())

        and: "the row survives with active=false rather than being removed"
        def userId = userRepository.findByExternalId(externalId).get().id
        def row = preferenceRepository.findByUserIdAndNotificationTypeAndChannel(userId, "welcome", Channel.EMAIL)
        row.isPresent()
        !row.get().active
    }

    def "DELETE then POST revives the soft-deleted preference rather than conflicting"() {
        given: "a preference that has been deleted, leaving an inactive row in the unique slot"
        def externalId = createUser("pref-user-910")
        def originalId = idOf(createPreference(externalId, "welcome", "EMAIL", "ENABLED"))
        mockMvc.perform(delete("/api/v1/preferences/{userExternalId}/{preferenceId}", externalId, originalId))
                .andExpect(status().isNoContent())

        when: "the same notificationType and channel is created again"
        def result = mockMvc.perform(post("/api/v1/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString([
                        userExternalId  : externalId,
                        notificationType: "welcome",
                        channel         : "EMAIL",
                        preference      : "DISABLED"
                ])))

        then: "it succeeds by reviving the existing row, keeping the same id"
        result.andExpect(status().isCreated())
                .andExpect(jsonPath('$.id').value(originalId))

        and: "it is readable again with the new value"
        mockMvc.perform(get("/api/v1/preferences/{userExternalId}/{preferenceId}", externalId, originalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.preference').value("DISABLED"))
                .andExpect(jsonPath('$.isActive').value(true))
    }

    def "DELETE /api/v1/preferences/{userExternalId}/{preferenceId} is 404 on a second call"() {
        given:
        def externalId = createUser("pref-user-920")
        def preferenceId = idOf(createPreference(externalId, "welcome", "EMAIL", "ENABLED"))
        mockMvc.perform(delete("/api/v1/preferences/{userExternalId}/{preferenceId}", externalId, preferenceId))
                .andExpect(status().isNoContent())

        expect:
        mockMvc.perform(delete("/api/v1/preferences/{userExternalId}/{preferenceId}", externalId, preferenceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.errorCode').value("PREFERENCE_NOT_FOUND"))
    }
}
