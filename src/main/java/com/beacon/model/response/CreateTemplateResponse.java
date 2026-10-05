package com.beacon.model.response;

import com.beacon.model.entity.Template;

import java.util.UUID;

import static com.beacon.model.Types.Channel;

public record CreateTemplateResponse(UUID id, Channel channel, String notificationType) {
    public static CreateTemplateResponse from(Template template) {
        return new CreateTemplateResponse(template.getId(), template.getChannel(), template.getNotificationType());
    }
}
