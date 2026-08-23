package com.beacon.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CreateTemplateResponse {
    private UUID id;
    private String channel;
    private String notificationType;
}
