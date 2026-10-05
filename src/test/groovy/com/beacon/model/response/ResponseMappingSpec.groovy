package com.beacon.model.response

import com.beacon.model.entity.BulkNotificationJob
import com.beacon.model.entity.DeviceToken
import com.beacon.model.entity.Template
import com.beacon.model.entity.User
import com.beacon.model.entity.UserPreference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import spock.lang.Specification

import java.time.Instant

import static com.beacon.model.Types.Channel
import static com.beacon.model.Types.JobStatus
import static com.beacon.model.Types.Platform
import static com.beacon.model.Types.PreferenceType

class ResponseMappingSpec extends Specification {

    def created = Instant.parse("2026-01-01T00:00:00Z")
    def updated = Instant.parse("2026-01-02T00:00:00Z")

    def "GetTemplateResponse.from copies every template field"() {
        given:
        def template = new Template(id: UUID.randomUUID(), channel: Channel.EMAIL, notificationType: "welcome",
                body: "Hello", subject: "Hi", createdAt: created, updatedAt: updated)

        expect:
        GetTemplateResponse.from(template) ==
                new GetTemplateResponse(template.id, Channel.EMAIL, "welcome", "Hello", "Hi", created, updated)
    }

    def "CreateTemplateResponse.from copies id, channel and notification type"() {
        given:
        def template = new Template(id: UUID.randomUUID(), channel: Channel.SMS, notificationType: "otp", body: "x")

        expect:
        CreateTemplateResponse.from(template) == new CreateTemplateResponse(template.id, Channel.SMS, "otp")
    }

    def "CreateUserResponse.from copies the user fields"() {
        given:
        def user = new User(id: 7L, externalId: "ext-7", name: "Ada", email: "ada@example.com", phone: "555")

        expect:
        CreateUserResponse.from(user) == new CreateUserResponse(7L, "ext-7", "Ada", "ada@example.com", "555")
    }

    def "GetUserResponse.from maps the user and its device tokens"() {
        given:
        def user = new User(id: 7L, externalId: "ext-7", name: "Ada", email: "ada@example.com", phone: "555",
                createdAt: created, updatedAt: updated)
        user.addDeviceToken(new DeviceToken(token: "t1", platform: Platform.IOS))
        user.addDeviceToken(new DeviceToken(token: "t2", platform: Platform.WEB))

        expect:
        GetUserResponse.from(user) == new GetUserResponse(7L, "Ada", "ext-7", "ada@example.com", "555",
                [new DeviceTokenResponse("t1", Platform.IOS), new DeviceTokenResponse("t2", Platform.WEB)],
                created, updated)
    }

    def "GetPreferenceResponse.from copies the preference and its active flag"() {
        given:
        def preference = new UserPreference(id: UUID.randomUUID(), userId: 1L, notificationType: "promo",
                channel: Channel.PUSH, preference: PreferenceType.DISABLED, active: false,
                createdAt: created, updatedAt: updated)

        expect:
        GetPreferenceResponse.from(preference) == new GetPreferenceResponse(preference.id, "promo", Channel.PUSH,
                PreferenceType.DISABLED, false, created, updated)
    }

    def "CreateUserPreferenceResponse.from uses the supplied external user id"() {
        given:
        def preference = new UserPreference(id: UUID.randomUUID(), userId: 1L, notificationType: "promo",
                channel: Channel.PUSH, preference: PreferenceType.ENABLED, createdAt: created, updatedAt: updated)

        expect:
        CreateUserPreferenceResponse.from(preference, "ext-1") ==
                new CreateUserPreferenceResponse(preference.id, 1L, "ext-1", "promo", Channel.PUSH, created, updated)
    }

    def "GetBulkNotificationJobResponse.from copies the job counters and timestamps"() {
        given:
        def completed = Instant.parse("2026-01-03T00:00:00Z")
        def job = new BulkNotificationJob(id: UUID.randomUUID(), status: JobStatus.COMPLETED, actionCount: 5,
                successCount: 3, failureCount: 1, skippedCount: 1, createdAt: created, updatedAt: updated,
                completedAt: completed)

        expect:
        GetBulkNotificationJobResponse.from(job) == new GetBulkNotificationJobResponse(job.id,
                JobStatus.COMPLETED, 5, 3, 1, 1, created, updated, completed)
    }

    def "GetPreferenceResponse serializes the active flag as isActive"() {
        given:
        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build()
        def response = new GetPreferenceResponse(UUID.randomUUID(), "promo", Channel.PUSH,
                PreferenceType.ENABLED, true, created, updated)

        when:
        def json = mapper.readTree(mapper.writeValueAsString(response))

        then:
        json.get("isActive").asBoolean()
        !json.has("active")
    }
}
