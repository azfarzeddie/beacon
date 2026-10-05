package com.beacon.model.response;

import com.beacon.model.Types;
import com.beacon.model.entity.UserPreference;

import java.time.Instant;
import java.util.UUID;

public record GetPreferenceResponse(
        UUID id,
        String notificationType,
        Types.Channel channel,
        Types.PreferenceType preference,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt) {

    public static GetPreferenceResponse from(UserPreference preference) {
        return new GetPreferenceResponse(preference.getId(), preference.getNotificationType(),
                preference.getChannel(), preference.getPreference(), preference.isActive(),
                preference.getCreatedAt(), preference.getUpdatedAt());
    }
}
