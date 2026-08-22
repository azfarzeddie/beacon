package com.beacon.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.beacon.model.Types.Platform;

@Data
public class CreateUserRequest {

    @NotNull
    String externalId;
    @NotNull
    @NotEmpty
    String name;
    @NotNull
    @Email
    @NotEmpty
    String email;
    String phone;
    List<DeviceToken> deviceTokens = new ArrayList<>();

    @Data
    public static class DeviceToken {
        String token;
        Platform platform;
    }

}
