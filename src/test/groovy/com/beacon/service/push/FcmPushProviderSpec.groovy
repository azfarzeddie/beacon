package com.beacon.service.push

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import spock.lang.Specification
import spock.lang.Subject

class FcmPushProviderSpec extends Specification {

    FirebaseMessaging firebaseMessaging = Mock()

    @Subject
    FcmPushProvider fcmPushProvider = new FcmPushProvider({ firebaseMessaging })

    def "getProviderName returns firebase"() {
        expect:
        fcmPushProvider.getProviderName() == "firebase"
    }

    def "send passes a message to the Firebase client and returns true on success"() {
        when:
        boolean result = fcmPushProvider.send("device-token-123", "Your order shipped!")

        then:
        1 * firebaseMessaging.send(_ as Message) >> "projects/beacon/messages/1"
        result
    }

    def "send returns false when Firebase rejects the message"() {
        given:
        // FirebaseMessagingException has no public constructor - a mock stands in as a throwable.
        def exception = Mock(FirebaseMessagingException)

        when:
        boolean result = fcmPushProvider.send("device-token-123", "Hi")

        then:
        1 * firebaseMessaging.send(_ as Message) >> { throw exception }
        !result
    }

    def "the real client is only built lazily on first send"() {
        given:
        boolean built = false
        def provider = new FcmPushProvider({ built = true; firebaseMessaging })

        expect:
        !built

        when:
        provider.send("device-token-123", "Hi")

        then:
        built
        1 * firebaseMessaging.send(_ as Message) >> "projects/beacon/messages/1"
    }
}
