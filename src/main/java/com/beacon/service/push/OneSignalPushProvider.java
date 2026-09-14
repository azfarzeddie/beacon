package com.beacon.service.push;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class OneSignalPushProvider implements PushProvider {
    private static final String PROVIDER_NAME = "onesignal";

    private final RestClient restClient;
    private final String appId;

    public OneSignalPushProvider(@Value("${spring.push.onesignal.base-url}") String baseUrl,
                                  @Value("${spring.push.onesignal.app-id}") String appId,
                                  @Value("${spring.push.onesignal.api-key}") String apiKey) {
        this.appId = appId;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Key " + apiKey)
                .build();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean send(String to, String message) {
        Map<String, Object> payload = Map.of(
                "app_id", appId,
                "include_subscription_ids", List.of(to),
                "contents", Map.of("en", message)
        );

        try {
            return restClient.post()
                    .uri("/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode()
                    .is2xxSuccessful();
        } catch (RestClientException e) {
            return false;
        }
    }
}
