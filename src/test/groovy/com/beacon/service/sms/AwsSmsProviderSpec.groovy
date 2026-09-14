package com.beacon.service.sms

import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.SnsException
import spock.lang.Specification
import spock.lang.Subject

class AwsSmsProviderSpec extends Specification {

    SnsClient snsClient = Mock()

    @Subject
    AwsSmsProvider awsSmsProvider = new AwsSmsProvider(snsClient)

    def "getProviderName returns aws"() {
        expect:
        awsSmsProvider.getProviderName() == "aws"
    }

    def "send publishes the message to the given phone number and returns true"() {
        when:
        boolean result = awsSmsProvider.send("+15550100", "Your code is 123456")

        then:
        1 * snsClient.publish({ PublishRequest request ->
            request.phoneNumber() == "+15550100" && request.message() == "Your code is 123456"
        }) >> null
        result
    }

    def "send returns false when SNS rejects the message"() {
        when:
        boolean result = awsSmsProvider.send("+15550100", "Your code is 123456")

        then:
        1 * snsClient.publish(_ as PublishRequest) >> { throw SnsException.builder().message("Invalid phone number").build() }
        !result
    }
}
