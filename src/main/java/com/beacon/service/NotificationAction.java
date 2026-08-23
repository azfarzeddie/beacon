package com.beacon.service;

import com.beacon.model.NotificationContext;

import static com.beacon.model.Types.*;

public interface NotificationAction {
    Channel getChannel();
    boolean send(NotificationContext notificationContext);
}
