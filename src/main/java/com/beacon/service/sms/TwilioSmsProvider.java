package com.beacon.service.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TwilioSmsProvider implements SmsProvider {
    private static final String PROVIDER_NAME = "twilio";

    private final RestClient restClient;
    private final String accountSid;
    private final String fromNumber;

    public TwilioSmsProvider(@Value("${spring.sms.twilio.base-url}") String baseUrl,
                              @Value("${spring.sms.twilio.account-sid}") String accountSid,
                              @Value("${spring.sms.twilio.auth-token}") String authToken,
                              @Value("${spring.sms.twilio.from-number}") String fromNumber) {
        this.accountSid = accountSid;
        this.fromNumber = fromNumber;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor(new BasicAuthenticationInterceptor(accountSid, authToken))
                .build();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean send(String to, String message) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", to);
        body.add("From", fromNumber);
        body.add("Body", message);

        try {
            return restClient.post()
                    .uri("/Accounts/{accountSid}/Messages.json", accountSid)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode()
                    .is2xxSuccessful();
        } catch (RestClientException e) {
            return false;
        }
    }
}
