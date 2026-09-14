package com.beacon.model.response;

import com.beacon.model.Types;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class CreateUserPreferenceResponse {
    UUID id;
    Long userId;
    String notificationType;
    Types.Channel channel;
    Instant createdAt;
    Instant updatedAt;
}
