package com.beacon.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import static com.beacon.model.Types.Platform;

@Data
@AllArgsConstructor
public class DeviceTokenResponse {
    private String token;
    private Platform platform;
}
