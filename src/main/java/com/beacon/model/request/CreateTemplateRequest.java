package com.beacon.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTemplateRequest {
    @NotNull
    @NotEmpty
    private String templateBody;
    @NotNull
    @NotEmpty
    private String notificationType;
    @NotNull
    @NotEmpty
    private String channel;
    private String subject;
}
