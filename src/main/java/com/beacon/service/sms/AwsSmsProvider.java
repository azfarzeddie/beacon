package com.beacon.service.sms;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

@Component
public class AwsSmsProvider implements SmsProvider {
    private static final String PROVIDER_NAME = "aws";

    private final SnsClient snsClient;

    public AwsSmsProvider(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean send(String to, String message) {
        try {
            snsClient.publish(PublishRequest.builder()
                    .phoneNumber(to)
                    .message(message)
                    .build());
            return true;
        } catch (SnsException e) {
            return false;
        }
    }
}
