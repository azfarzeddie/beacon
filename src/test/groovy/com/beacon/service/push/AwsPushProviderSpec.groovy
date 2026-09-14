package com.beacon.service.push

import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.SnsException
import spock.lang.Specification
import spock.lang.Subject

class AwsPushProviderSpec extends Specification {

    SnsClient snsClient = Mock()

    @Subject
    AwsPushProvider awsPushProvider = new AwsPushProvider(snsClient, "arn:aws:sns:us-east-1:123456789012:app/GCM/beacon")

    def "getProviderName returns aws"() {
        expect:
        awsPushProvider.getProviderName() == "aws"
    }

    def "send creates a platform endpoint for the device token and publishes to it, returning true"() {
        when:
        boolean result = awsPushProvider.send("device-token-123", "Your order shipped!")

        then:
        1 * snsClient.createPlatformEndpoint({ CreatePlatformEndpointRequest request ->
            request.platformApplicationArn() == "arn:aws:sns:us-east-1:123456789012:app/GCM/beacon" &&
                    request.token() == "device-token-123"
        }) >> CreatePlatformEndpointResponse.builder().endpointArn("arn:aws:sns:us-east-1:123456789012:endpoint/GCM/beacon/abc").build()

        1 * snsClient.publish({ PublishRequest request ->
            request.targetArn() == "arn:aws:sns:us-east-1:123456789012:endpoint/GCM/beacon/abc" &&
                    request.message() == "Your order shipped!"
        }) >> null

        result
    }

    def "send returns false when creating the platform endpoint fails"() {
        when:
        boolean result = awsPushProvider.send("device-token-123", "Hi")

        then:
        1 * snsClient.createPlatformEndpoint(_ as CreatePlatformEndpointRequest) >> {
            throw SnsException.builder().message("Invalid token").build()
        }
        0 * snsClient.publish(_)
        !result
    }

    def "send returns false when publishing fails"() {
        when:
        boolean result = awsPushProvider.send("device-token-123", "Hi")

        then:
        1 * snsClient.createPlatformEndpoint(_ as CreatePlatformEndpointRequest) >>
                CreatePlatformEndpointResponse.builder().endpointArn("arn:endpoint").build()
        1 * snsClient.publish(_ as PublishRequest) >> { throw SnsException.builder().message("Endpoint disabled").build() }
        !result
    }
}
