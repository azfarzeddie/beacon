package com.beacon.model.request;

import com.beacon.model.Types;
import jakarta.validation.constraints.NotNull;

public record UpdateUserPreferenceRequest(@NotNull Types.PreferenceType preference) {
}
