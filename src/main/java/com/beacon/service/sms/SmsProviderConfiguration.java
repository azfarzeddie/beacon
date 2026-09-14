package com.beacon.service.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class SmsProviderConfiguration {

    @Bean(destroyMethod = "close")
    SnsClient snsClient(@Value("${spring.sms.aws.access-key}") String accessKey,
                         @Value("${spring.sms.aws.secret-key}") String secretKey,
                         @Value("${spring.sms.aws.region}") String region) {
        return SnsClient.builder()
                .region(Region.of(region))
                // deferred to a lambda so a blank access key/secret (an inactive, unconfigured
                // provider) never fails eagerly at bean creation - AwsBasicCredentials.create()
                // validates its arguments immediately, so it must only run when this provider
                // is actually invoked, not at startup.
                .credentialsProvider(() -> AwsBasicCredentials.create(accessKey, secretKey))
                .build();
    }
}
