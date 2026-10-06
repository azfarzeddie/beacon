package com.beacon.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import static com.beacon.model.Types.Channel;

public record CreateTemplateRequest(
        @NotNull @NotEmpty String templateBody,
        @NotNull @NotEmpty String notificationType,
        @NotNull Channel channel,
        String subject) {
}
