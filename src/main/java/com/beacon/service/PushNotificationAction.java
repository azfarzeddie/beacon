package com.beacon.service;

import com.beacon.model.NotificationContext;
import org.springframework.stereotype.Component;

import static com.beacon.model.Types.Channel;

@Component
public class PushNotificationAction implements NotificationAction {
    @Override
    public Channel getChannel() {
        return Channel.PUSH;
    }

    @Override
    public boolean send(NotificationContext notificationContext) {
        return false;
    }
}
