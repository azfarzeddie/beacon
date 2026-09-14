package com.beacon.model.request;

import com.beacon.model.Types;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserPreferenceRequest {
    @NotNull
    @NotEmpty
    String userExternalId;
    @NotNull
    @NotEmpty
    String notificationType;
    @NotNull
    Types.Channel channel;
    @NotNull
    Types.PreferenceType preference;
}
