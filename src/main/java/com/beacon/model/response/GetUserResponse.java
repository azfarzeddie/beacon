package com.beacon.model.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class GetUserResponse {
    Long id;
    String name;
    String externalId;
    String email;
    String phone;
    List<DeviceTokenResponse> deviceTokens;
    Instant createdAt;
    Instant updatedAt;
}
