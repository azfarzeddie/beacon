package com.beacon.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

import static com.beacon.model.Types.Channel;

@Data
@AllArgsConstructor
public class CreateTemplateResponse {
    private UUID id;
    private Channel channel;
    private String notificationType;
}
