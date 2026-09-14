package com.beacon.service

import com.beacon.exception.PreferenceException
import com.beacon.exception.UserException
import com.beacon.model.entity.User
import com.beacon.model.entity.UserPreference
import com.beacon.model.request.CreateUserPreferenceRequest
import com.beacon.model.response.CreateUserPreferenceResponse
import com.beacon.model.response.GetPreferenceResponse
import com.beacon.model.response.GetPreferencesResponse
import com.beacon.repository.PreferenceRepository
import com.beacon.repository.UserRepository
import spock.lang.Specification
import spock.lang.Subject

import static com.beacon.model.Types.Channel
import static com.beacon.model.Types.PreferenceType

class PreferencesServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    PreferenceRepository preferenceRepository = Mock()

    @Subject
    PreferencesService preferencesService = new PreferencesService(userRepository, preferenceRepository)

    private static User userWithId(Long id, String externalId = "ext-1") {
        def user = new User()
        user.id = id
        user.externalId = externalId
        return user
    }

    def "addUserPreference persists a new preference and returns the created response"() {
        given:
        def request = new CreateUserPreferenceRequest(
                userExternalId: "ext-1",
                notificationType: "welcome",
                channel: Channel.EMAIL,
                preference: PreferenceType.ENABLED
        )
        def user = userWithId(1L)

        when:
        CreateUserPreferenceResponse response = preferencesService.addUserPreference(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(user)
        1 * preferenceRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.empty()
        1 * preferenceRepository.save({ UserPreference p ->
            p.userId == 1L &&
                    p.notificationType == "welcome" &&
                    p.channel == Channel.EMAIL &&
                    p.preference == PreferenceType.ENABLED
        }) >> { UserPreference p -> p.id = UUID.fromString("00000000-0000-0000-0000-000000000001"); p }

        response.id == UUID.fromString("00000000-0000-0000-0000-000000000001")
        response.userId == 1L
        response.notificationType == "welcome"
        response.channel == Channel.EMAIL
    }

    def "addUserPreference throws UserNotFoundException when the user does not exist"() {
        given:
        def request = new CreateUserPreferenceRequest(
                userExternalId: "unknown",
                notificationType: "welcome",
                channel: Channel.EMAIL,
                preference: PreferenceType.ENABLED
        )

        when:
        preferencesService.addUserPreference(request)

        then:
        1 * userRepository.findByExternalId("unknown") >> Optional.empty()
        0 * preferenceRepository.save(_)
        thrown(UserException.UserNotFoundException)
    }

    def "addUserPreference throws PreferenceAlreadyExists when a preference for the type and channel already exists"() {
        given:
        def request = new CreateUserPreferenceRequest(
                userExternalId: "ext-1",
                notificationType: "welcome",
                channel: Channel.EMAIL,
                preference: PreferenceType.ENABLED
        )
        def user = userWithId(1L)

        when:
        preferencesService.addUserPreference(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(user)
        1 * preferenceRepository.findByNotificationTypeAndChannel("welcome", Channel.EMAIL) >> Optional.of(new UserPreference())
        0 * preferenceRepository.save(_)
        thrown(PreferenceException.PreferenceAlreadyExists)
    }

    def "getAllUserPreferences returns every preference for the user"() {
        given:
        def user = userWithId(1L)
        def preferences = [
                new UserPreference(id: UUID.randomUUID(), notificationType: "welcome", channel: Channel.EMAIL, preference: PreferenceType.ENABLED, active: true),
                new UserPreference(id: UUID.randomUUID(), notificationType: "otp", channel: Channel.SMS, preference: PreferenceType.DISABLED, active: false)
        ]

        when:
        GetPreferencesResponse response = preferencesService.getAllUserPreferences("ext-1")

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(user)
        1 * preferenceRepository.findByUserId(1L) >> Optional.of(preferences)

        response.userId == 1L
        response.preferences.size() == 2
        response.preferences*.notificationType == ["welcome", "otp"]
        response.preferences*.preference == [PreferenceType.ENABLED, PreferenceType.DISABLED]
    }

    def "getAllUserPreferences throws UserNotFoundException when the user does not exist"() {
        when:
        preferencesService.getAllUserPreferences("unknown")

        then:
        1 * userRepository.findByExternalId("unknown") >> Optional.empty()
        0 * preferenceRepository.findByUserId(_)
        thrown(UserException.UserNotFoundException)
    }

    def "getAllUserPreferences throws PreferenceNotFound when the repository returns an empty Optional"() {
        given:
        def user = userWithId(1L)

        when:
        preferencesService.getAllUserPreferences("ext-1")

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(user)
        1 * preferenceRepository.findByUserId(1L) >> Optional.empty()
        thrown(PreferenceException.PreferenceNotFound)
    }

    def "getUserPreference returns the matching preference"() {
        given:
        def user = userWithId(1L)
        def preferenceId = UUID.randomUUID()
        def preference = new UserPreference(
                id: preferenceId,
                notificationType: "welcome",
                channel: Channel.EMAIL,
                preference: PreferenceType.ENABLED,
                active: true
        )

        when:
        GetPreferenceResponse response = preferencesService.getUserPreference("ext-1", preferenceId)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(user)
        1 * preferenceRepository.findByIdAndUserId(preferenceId, 1L) >> Optional.of(preference)

        response.id == preferenceId
        response.notificationType == "welcome"
        response.channel == Channel.EMAIL
        response.preference == PreferenceType.ENABLED
        response.isActive
    }

    def "getUserPreference throws PreferenceNotFound when no preference matches"() {
        given:
        def user = userWithId(1L)
        def preferenceId = UUID.randomUUID()

        when:
        preferencesService.getUserPreference("ext-1", preferenceId)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(user)
        1 * preferenceRepository.findByIdAndUserId(preferenceId, 1L) >> Optional.empty()
        thrown(PreferenceException.PreferenceNotFound)
    }

    def "getUserPreference throws UserNotFoundException when the user does not exist"() {
        given:
        def preferenceId = UUID.randomUUID()

        when:
        preferencesService.getUserPreference("unknown", preferenceId)

        then:
        1 * userRepository.findByExternalId("unknown") >> Optional.empty()
        0 * preferenceRepository.findByIdAndUserId(_, _)
        thrown(UserException.UserNotFoundException)
    }
}
