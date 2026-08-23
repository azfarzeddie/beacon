package com.beacon.service;

import com.beacon.model.NotificationContext;
import org.springframework.stereotype.Component;

import static com.beacon.model.Types.Channel;

@Component
public class EmailNotificationAction implements NotificationAction {
    @Override
    public Channel getChannel() {
        return Channel.EMAIL;
    }

    @Override
    public boolean send(NotificationContext notificationContext) {
        return false;
    }
}
