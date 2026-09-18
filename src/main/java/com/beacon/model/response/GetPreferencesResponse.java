package com.beacon.model.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetPreferencesResponse {
    Long userId;
    List<GetPreferenceResponse> preferences;
}
