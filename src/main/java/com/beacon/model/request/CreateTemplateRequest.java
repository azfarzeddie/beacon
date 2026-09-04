package com.beacon.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import static com.beacon.model.Types.Channel;

@Data
public class CreateTemplateRequest {
    @NotNull
    @NotEmpty
    private String templateBody;
    @NotNull
    @NotEmpty
    private String notificationType;
    @NotNull
    private Channel channel;
    private String subject;
}
