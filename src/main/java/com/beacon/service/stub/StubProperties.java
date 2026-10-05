package com.beacon.service.stub;

import com.beacon.model.Types.Channel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tuning knobs for the stub notification providers (active when
 * spring.sms.provider / spring.push.provider / spring.mail.provider is "stub").
 * <p>
 * Latency per call is log-normally distributed: {@code medianMs * exp(sigma * N(0,1))}, capped at
 * {@code maxMs}. That gives a realistic right-skewed tail (most calls near the median, a few
 * much slower) rather than a flat constant. Defaults approximate typical API-accept latencies.
 */
@Data
@Component
@ConfigurationProperties("beacon.stub")
public class StubProperties {

    /** Max number of accepted/failed messages kept in memory for inspection. */
    private int retain = 10_000;

    private Profile email = new Profile(150, 0.5, 5_000, 0.0);  // SMTP relay accept
    private Profile sms = new Profile(250, 0.4, 5_000, 0.0);    // Twilio / SNS publish
    private Profile push = new Profile(90, 0.5, 3_000, 0.0);    // FCM / SNS / OneSignal send

    public Profile forChannel(Channel channel) {
        return switch (channel) {
            case EMAIL -> email;
            case SMS -> sms;
            case PUSH -> push;
        };
    }

    @Data
    public static class Profile {
        /** Median accept latency in ms. 0 disables the delay. */
        private long medianMs;
        /** Log-normal shape: 0 = constant latency, ~0.5 = realistic tail, 1+ = very heavy tail. */
        private double sigma;
        /** Hard cap on a single call's latency in ms. */
        private long maxMs;
        /** Probability (0..1) that the provider rejects the message. */
        private double failureRate;

        public Profile() {
        }

        public Profile(long medianMs, double sigma, long maxMs, double failureRate) {
            this.medianMs = medianMs;
            this.sigma = sigma;
            this.maxMs = maxMs;
            this.failureRate = failureRate;
        }
    }
}
