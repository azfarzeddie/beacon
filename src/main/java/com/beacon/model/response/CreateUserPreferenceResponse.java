package com.beacon.model.response;

import com.beacon.model.Types;
import com.beacon.model.entity.UserPreference;

import java.time.Instant;
import java.util.UUID;

public record CreateUserPreferenceResponse(
        UUID id,
        Long userId,
        String userExternalId,
        String notificationType,
        Types.Channel channel,
        Instant createdAt,
        Instant updatedAt) {

    public static CreateUserPreferenceResponse from(UserPreference preference, String userExternalId) {
        return new CreateUserPreferenceResponse(preference.getId(), preference.getUserId(), userExternalId,
                preference.getNotificationType(), preference.getChannel(), preference.getCreatedAt(),
                preference.getUpdatedAt());
    }
}
