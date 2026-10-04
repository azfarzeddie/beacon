package com.beacon.service;

import com.beacon.model.entity.Template;
import com.beacon.model.request.CreateTemplateRequest;
import com.beacon.model.response.CreateTemplateResponse;
import com.beacon.model.response.GetTemplateResponse;
import com.beacon.repository.TemplateRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.beacon.exception.TemplateException.TemplateAlreadyExists;
import static com.beacon.exception.TemplateException.TemplateNotFound;
import static com.beacon.model.Types.Channel;

@Service
public class TemplateService {
    private final TemplateRepository templateRepository;

    public TemplateService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional
    public CreateTemplateResponse createTemplate(CreateTemplateRequest request) {
        // check if a template for this notification type and channel combination already exists
        if (templateRepository.findByNotificationTypeAndChannel(
                request.getNotificationType(), request.getChannel()).isPresent()) {
            throw new TemplateAlreadyExists("A template for " + request.getNotificationType()
                    + " and " + request.getChannel() + " already exists. Please call the PUT endpoint to update it.");
        }

        Template template = new Template();
        template.setChannel(request.getChannel());
        template.setNotificationType(request.getNotificationType());
        template.setBody(request.getTemplateBody());
        if (request.getSubject() != null) {
            template.setSubject(request.getSubject());
        }

        try {
            templateRepository.saveAndFlush(template);
        } catch (DataIntegrityViolationException e) {
            throw new TemplateAlreadyExists("A template for " + request.getNotificationType() + " and "
                    + request.getChannel() + " already exists. Please call the PUT endpoint to update it.");
        }

        return new CreateTemplateResponse(template.getId(), template.getChannel(), template.getNotificationType());
    }

    public GetTemplateResponse getTemplate(String notificationType, Channel channel) {
        Optional<Template> found = templateRepository.findByNotificationTypeAndChannel(notificationType, channel);
        if (found.isEmpty()) {
            throw new TemplateNotFound("No template found for " + notificationType + " and " + channel);
        }

        return prepareResponse(found.get());
    }

    private static GetTemplateResponse prepareResponse(Template found) {
        return new GetTemplateResponse(
                found.getId(),
                found.getChannel(),
                found.getNotificationType(),
                found.getBody(),
                found.getSubject(),
                found.getCreatedAt(),
                found.getUpdatedAt()
        );
    }

    public GetTemplateResponse getTemplateById(UUID id) {
        Optional<Template> found = templateRepository.findById(id);
        if (found.isEmpty()) {
            throw new TemplateNotFound("No template found for ID: " + id);
        }

        return prepareResponse(found.get());
    }

    public String resolveTemplate(String template, Map<String, String> templateVariables) {
        String message = template;

        for (Map.Entry<String, String> entry : templateVariables.entrySet()) {
            String variableName = entry.getKey();
            String variableValue = entry.getValue();

            message = message.replace("{{" + variableName + "}}", variableValue);
        }

        Pattern pattern = Pattern.compile("\\{\\{([^{}]+)}}");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            throw new IllegalArgumentException(
                    "Unresolved template variable: " + matcher.group(1)
            );
        }

        return message;
    }
}
