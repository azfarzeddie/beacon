package com.beacon.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

import static com.beacon.model.Types.Channel;

@Data
public class SendNotificationRequest {
    @NotNull
    @NotEmpty
    private String userExternalId;
    @NotNull
    @NotEmpty
    private Channel channel;
    @NotNull
    @NotEmpty
    private String notificationType;
    private Map<String, String> templateVariables;
    private String subject;
}
