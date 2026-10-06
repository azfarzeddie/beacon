package com.beacon.service

import com.beacon.exception.UserException
import com.beacon.model.entity.DeviceToken
import com.beacon.model.entity.User
import com.beacon.model.request.CreateUserRequest
import com.beacon.model.response.CreateUserResponse
import com.beacon.model.response.GetUserResponse
import com.beacon.repository.DeviceTokenRepository
import com.beacon.repository.UserRepository
import spock.lang.Specification
import spock.lang.Subject

import static com.beacon.model.Types.Platform

class UserServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    DeviceTokenRepository deviceTokenRepository = Mock()

    @Subject
    UserService userService = new UserService(userRepository, deviceTokenRepository)

    def "createUser persists a new user with its device tokens and returns the created response"() {
        given:
        def request = new CreateUserRequest(
                "ext-1",
                "Ada Lovelace",
                "ada@example.com",
                "555-0100",
                [
                        new CreateUserRequest.DeviceToken("token-1", Platform.ANDROID),
                        new CreateUserRequest.DeviceToken("token-2", Platform.IOS)
                ]
        )

        when:
        CreateUserResponse response = userService.createUser(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.empty()
        2 * deviceTokenRepository.findByToken(_) >> Optional.empty()
        1 * userRepository.save({ User u ->
            u.externalId == "ext-1" &&
                    u.name == "Ada Lovelace" &&
                    u.email == "ada@example.com" &&
                    u.phone == "555-0100" &&
                    u.deviceTokens.size() == 2 &&
                    u.deviceTokens*.token.sort() == ["token-1", "token-2"] &&
                    u.deviceTokens.every { it.user == u }
        }) >> { User u -> u.id = 42L; u }

        response.id() == 42L
        response.externalId() == "ext-1"
        response.name() == "Ada Lovelace"
        response.email() == "ada@example.com"
        response.phone() == "555-0100"
    }

    def "createUser reassigns an already registered device token to the new user instead of inserting a duplicate"() {
        given:
        def previousOwner = new User(id: 7L, externalId: "ext-old")
        def existing = new DeviceToken(id: 5L, token: "shared", platform: Platform.ANDROID, user: previousOwner)
        def request = new CreateUserRequest("ext-2", "Grace", "grace@example.com", null,
                [new CreateUserRequest.DeviceToken("shared", Platform.IOS)])
        User saved = null

        when:
        userService.createUser(request)

        then:
        1 * userRepository.findByExternalId("ext-2") >> Optional.empty()
        1 * deviceTokenRepository.findByToken("shared") >> Optional.of(existing)
        1 * userRepository.save(_ as User) >> { User u -> saved = u; u }

        and: "the very same row now belongs to the new user, with the platform it re-registered with"
        saved.deviceTokens.size() == 1
        saved.deviceTokens[0].is(existing)
        existing.user.is(saved)
        existing.platform == Platform.IOS
    }

    def "createUser registers a token repeated within the request only once"() {
        given:
        def request = new CreateUserRequest("ext-3", "Ada", "ada@example.com", null, [
                new CreateUserRequest.DeviceToken("dup", Platform.ANDROID),
                new CreateUserRequest.DeviceToken("dup", Platform.IOS),
                new CreateUserRequest.DeviceToken("other", Platform.WEB)
        ])
        User saved = null

        when:
        userService.createUser(request)

        then:
        1 * userRepository.findByExternalId("ext-3") >> Optional.empty()
        2 * deviceTokenRepository.findByToken(_) >> Optional.empty()
        1 * userRepository.save(_ as User) >> { User u -> saved = u; u }

        and: "the first occurrence wins"
        saved.deviceTokens*.token == ["dup", "other"]
        saved.deviceTokens[0].platform == Platform.ANDROID
    }

    def "createUser throws UserAlreadyExistsException when the externalId is already taken"() {
        given:
        def request = new CreateUserRequest("ext-1", "Ada", "ada@example.com", null, null)

        when:
        userService.createUser(request)

        then:
        1 * userRepository.findByExternalId("ext-1") >> Optional.of(new User())
        0 * userRepository.save(_)
        thrown(UserException.UserAlreadyExistsException)
    }

    def "getUser returns the user together with its device tokens"() {
        given:
        def user = new User(id: 1L, externalId: "ext-1", name: "Ada", email: "ada@example.com", phone: "555-0100")
        user.addDeviceToken(new DeviceToken(token: "token-1", platform: Platform.WEB))

        when:
        GetUserResponse response = userService.getUser(1L)

        then:
        1 * userRepository.findById(1L) >> Optional.of(user)
        response.id() == 1L
        response.externalId() == "ext-1"
        response.name() == "Ada"
        response.deviceTokens().size() == 1
        response.deviceTokens()[0].token() == "token-1"
        response.deviceTokens()[0].platform() == Platform.WEB
    }

    def "getUser returns an empty device token list when the user has none"() {
        given:
        def user = new User(id: 2L, externalId: "ext-2", name: "Bea", email: "bea@example.com")

        when:
        GetUserResponse response = userService.getUser(2L)

        then:
        1 * userRepository.findById(2L) >> Optional.of(user)
        response.deviceTokens().isEmpty()
    }

    def "getUser throws UserNotFoundException when no user exists for the given id"() {
        when:
        userService.getUser(99L)

        then:
        1 * userRepository.findById(99L) >> Optional.empty()
        thrown(UserException.UserNotFoundException)
    }
}
