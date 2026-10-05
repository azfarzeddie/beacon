package com.beacon.model.response;

import com.beacon.model.entity.Template;

import java.time.Instant;
import java.util.UUID;

import static com.beacon.model.Types.Channel;

public record GetTemplateResponse(
        UUID id,
        Channel channel,
        String notificationType,
        String body,
        String subject,
        Instant createdAt,
        Instant updatedAt) {

    public static GetTemplateResponse from(Template template) {
        return new GetTemplateResponse(template.getId(), template.getChannel(), template.getNotificationType(),
                template.getBody(), template.getSubject(), template.getCreatedAt(), template.getUpdatedAt());
    }
}
