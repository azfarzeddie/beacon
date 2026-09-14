package com.beacon.service;

import com.beacon.model.NotificationContext;
import com.beacon.service.push.PushProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.beacon.model.Types.Channel;

@Component
public class PushNotificationAction implements NotificationAction {
    private final Map<String, PushProvider> providers;

    @Value("${spring.push.provider}")
    String pushProvider;

    public PushNotificationAction(List<PushProvider> pushProviders) {
        this.providers = pushProviders.stream().collect(Collectors.toMap(
                PushProvider::getProviderName,
                Function.identity()
        ));
    }

    @PostConstruct
    void validateConfiguredProvider() {
        if (!providers.containsKey(pushProvider)) {
            throw new IllegalStateException("Unsupported push notification provider configured: '" + pushProvider
                    + "'. Supported providers: " + providers.keySet());
        }
    }

    @Override
    public Channel getChannel() {
        return Channel.PUSH;
    }

    @Override
    public boolean send(NotificationContext notificationContext) {
        List<String> deviceTokens = notificationContext.getDeviceTokens();
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            return false;
        }

        PushProvider provider = providers.get(pushProvider);
        boolean anySucceeded = false;
        for (String deviceToken : deviceTokens) {
            if (provider.send(deviceToken, notificationContext.getMessage())) {
                anySucceeded = true;
            }
        }
        return anySucceeded;
    }
}
