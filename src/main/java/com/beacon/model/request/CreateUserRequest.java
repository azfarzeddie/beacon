package com.beacon.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

import static com.beacon.model.Types.Platform;

public record CreateUserRequest(
        @NotNull String externalId,
        @NotNull @NotEmpty String name,
        @NotNull @Email @NotEmpty String email,
        String phone,
        @Valid List<DeviceToken> deviceTokens) {

    public CreateUserRequest {
        deviceTokens = deviceTokens == null ? List.of() : deviceTokens;
    }

    public record DeviceToken(
            @NotBlank String token,
            @NotNull Platform platform) {
    }

}
