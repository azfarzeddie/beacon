package com.beacon.model.request;

import com.beacon.model.Types;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserPreferenceRequest {
    @NotNull
    Types.PreferenceType preference;
}
