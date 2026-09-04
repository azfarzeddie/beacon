package com.beacon.service.factory;

import com.beacon.service.NotificationAction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.beacon.model.Types.Channel;

@Component
public class NotificationActionFactory {
    private final Map<Channel, NotificationAction> actions;

    public NotificationActionFactory(List<NotificationAction> notificationActions) {
        this.actions = notificationActions.stream().collect(Collectors.toMap(
                NotificationAction::getChannel,
                Function.identity()
        ));
    }

    public NotificationAction getAction(Channel channel) {
        NotificationAction action = actions.get(channel);

        if (action == null) {
            throw new IllegalArgumentException("Unexpected channel: " + channel);
        }

        return action;
    }
}
