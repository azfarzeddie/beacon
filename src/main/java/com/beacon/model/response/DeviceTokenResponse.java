package com.beacon.model.response;

import com.beacon.model.entity.DeviceToken;

import static com.beacon.model.Types.Platform;

public record DeviceTokenResponse(String token, Platform platform) {
    public static DeviceTokenResponse from(DeviceToken deviceToken) {
        return new DeviceTokenResponse(deviceToken.getToken(), deviceToken.getPlatform());
    }
}
