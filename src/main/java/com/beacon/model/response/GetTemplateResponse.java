package com.beacon.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

import static com.beacon.model.Types.Channel;

@Data
@AllArgsConstructor
public class GetTemplateResponse {
    private UUID id;
    private Channel channel;
    private String notificationType;
    private String body;
    private String subject;
    private Instant createdAt;
    private Instant updatedAt;
}
