package com.beacon.service.factory

import com.beacon.service.NotificationAction
import spock.lang.Specification

import static com.beacon.model.Types.Channel

class NotificationActionFactorySpec extends Specification {

    def "returns the action registered for a given channel"() {
        given:
        def emailAction = Stub(NotificationAction) { getChannel() >> Channel.EMAIL }
        def smsAction = Stub(NotificationAction) { getChannel() >> Channel.SMS }
        def factory = new NotificationActionFactory([emailAction, smsAction])

        expect:
        factory.getAction(Channel.EMAIL) == emailAction
        factory.getAction(Channel.SMS) == smsAction
    }

    def "throws IllegalArgumentException when no action is registered for the channel"() {
        given:
        def emailAction = Stub(NotificationAction) { getChannel() >> Channel.EMAIL }
        def factory = new NotificationActionFactory([emailAction])

        when:
        factory.getAction(Channel.PUSH)

        then:
        thrown(IllegalArgumentException)
    }
}
