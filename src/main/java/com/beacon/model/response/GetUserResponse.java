package com.beacon.model.response;

import com.beacon.model.entity.User;

import java.time.Instant;
import java.util.List;

public record GetUserResponse(
        Long id,
        String name,
        String externalId,
        String email,
        String phone,
        List<DeviceTokenResponse> deviceTokens,
        Instant createdAt,
        Instant updatedAt) {

    public static GetUserResponse from(User user) {
        return new GetUserResponse(user.getId(), user.getName(), user.getExternalId(), user.getEmail(),
                user.getPhone(), user.getDeviceTokens().stream().map(DeviceTokenResponse::from).toList(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
