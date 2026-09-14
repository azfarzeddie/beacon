package com.beacon.service;

import com.beacon.model.NotificationContext;
import com.beacon.service.sms.SmsProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.beacon.model.Types.Channel;

@Component
public class SMSNotificationAction implements NotificationAction {
    private final Map<String, SmsProvider> providers;

    @Value("${spring.sms.provider}")
    String smsProvider;

    public SMSNotificationAction(List<SmsProvider> smsProviders) {
        this.providers = smsProviders.stream().collect(Collectors.toMap(
                SmsProvider::getProviderName,
                Function.identity()
        ));
    }

    @PostConstruct
    void validateConfiguredProvider() {
        if (!providers.containsKey(smsProvider)) {
            throw new IllegalStateException("Unsupported SMS provider configured: '" + smsProvider
                    + "'. Supported providers: " + providers.keySet());
        }
    }

    @Override
    public Channel getChannel() {
        return Channel.SMS;
    }

    @Override
    public boolean send(NotificationContext notificationContext) {
        SmsProvider provider = providers.get(smsProvider);
        return provider.send(notificationContext.getPhone(), notificationContext.getMessage());
    }
}
