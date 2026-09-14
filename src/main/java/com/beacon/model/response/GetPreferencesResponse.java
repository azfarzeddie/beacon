package com.beacon.model.response;

import com.beacon.model.Types.Channel;
import com.beacon.model.Types.PreferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GetPreferencesResponse {
    Long userId;
    List<GetPreferenceResponse> preferences;
}
