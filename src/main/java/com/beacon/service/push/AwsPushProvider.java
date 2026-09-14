package com.beacon.service.push;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

@Component
public class AwsPushProvider implements PushProvider {
    private static final String PROVIDER_NAME = "aws";

    private final SnsClient snsClient;
    private final String platformApplicationArn;

    public AwsPushProvider(SnsClient snsClient,
                            @Value("${spring.push.aws.platform-application-arn}") String platformApplicationArn) {
        this.snsClient = snsClient;
        this.platformApplicationArn = platformApplicationArn;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean send(String to, String message) {
        try {
            String endpointArn = snsClient.createPlatformEndpoint(CreatePlatformEndpointRequest.builder()
                    .platformApplicationArn(platformApplicationArn)
                    .token(to)
                    .build()).endpointArn();

            // published as a plain string, which SNS forwards to the platform as best-effort;
            // a fully correct per-platform payload (APNs/FCM each expect their own JSON
            // structure via MessageStructure="json") is a known simplification here.
            snsClient.publish(PublishRequest.builder()
                    .targetArn(endpointArn)
                    .message(message)
                    .build());
            return true;
        } catch (SnsException e) {
            return false;
        }
    }
}
