package com.beacon.model.response;

import com.beacon.model.Types;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class GetPreferenceResponse {
    UUID id;
    String notificationType;
    Types.Channel channel;
    Types.PreferenceType preference;
    Boolean isActive;
    Instant createdAt;
    Instant updatedAt;
}
