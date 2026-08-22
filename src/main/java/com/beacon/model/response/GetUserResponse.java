package com.beacon.model.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.beacon.model.request.CreateUserRequest.DeviceToken;

@Data
@Builder
public class GetUserResponse {
    Long id;
    String name;
    String externalId;
    String email;
    String phone;
    List<DeviceToken> deviceTokens;
    Instant createdAt;
    Instant updatedAt;
}
