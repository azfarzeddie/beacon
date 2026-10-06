package com.beacon.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

import static com.beacon.model.Types.Channel;

public record SendNotificationRequest(
        @NotNull @NotEmpty String userExternalId,
        @NotNull Channel channel,
        @NotNull @NotEmpty String notificationType,
        Map<String, String> templateVariables) {
}
