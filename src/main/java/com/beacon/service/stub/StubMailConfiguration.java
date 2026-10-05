package com.beacon.service.stub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/** Replaces the SMTP mail sender with the stub when spring.mail.provider=stub. */
@Configuration
@ConditionalOnProperty(name = "spring.mail.provider", havingValue = "stub")
public class StubMailConfiguration {

    @Bean
    JavaMailSender stubMailSender(StubGateway gateway) {
        return new StubMailSender(gateway);
    }
}
