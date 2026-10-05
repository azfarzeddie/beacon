package com.beacon.model.response;

import java.util.List;

public record GetPreferencesResponse(Long userId, List<GetPreferenceResponse> preferences) {
}
