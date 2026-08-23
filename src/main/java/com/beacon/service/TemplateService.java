package com.beacon.service;

import com.beacon.exception.TemplateException;
import com.beacon.model.entity.Template;
import com.beacon.model.request.CreateTemplateRequest;
import com.beacon.model.response.CreateTemplateResponse;
import com.beacon.model.response.GetTemplateResponse;
import com.beacon.repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

import static com.beacon.exception.TemplateException.*;

@Service
public class TemplateService {
    private final TemplateRepository templateRepository;

    @Autowired
    public TemplateService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public CreateTemplateResponse createTemplate(CreateTemplateRequest request) {
        // check if a template for this notification type and channel combination already exists
        if (templateRepository.findByNotificationTypeAndChannel(
                request.getNotificationType(), request.getChannel()).isPresent()) {
            throw new TemplateAlreadyExists("A template for " + request.getNotificationType()
                    + " and " + request.getChannel() + " already exists.");
        }

        Template template = new Template();
        template.setChannel(request.getChannel());
        template.setNotificationType(request.getNotificationType());
        template.setBody(request.getTemplateBody());
        if (request.getSubject() != null) {
            template.setSubject(request.getSubject());
        }
        template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());

        templateRepository.save(template);

        return new CreateTemplateResponse(template.getId(), template.getChannel(), template.getNotificationType());
    }

    public GetTemplateResponse getTemplate(String notificationType, String channel) {
        Optional<Template> found = templateRepository.findByNotificationTypeAndChannel(notificationType, channel);
        if (found.isEmpty()) {
            throw new TemplateNotFound("No template found for " + notificationType + " and " + channel);
        }

        Template template = found.get();
        GetTemplateResponse response = new GetTemplateResponse(
                template.getId(),
                template.getChannel(),
                template.getNotificationType(),
                template.getBody(),
                template.getSubject(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
        return response;
    }
}
