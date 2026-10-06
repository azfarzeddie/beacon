package com.beacon.model.request;

import com.beacon.model.Types;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateUserPreferenceRequest(
        @NotNull @NotEmpty String userExternalId,
        @NotNull @NotEmpty String notificationType,
        @NotNull Types.Channel channel,
        @NotNull Types.PreferenceType preference) {
}
