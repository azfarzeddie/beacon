package com.beacon.service;

import com.beacon.model.entity.Template;
import com.beacon.model.request.CreateTemplateRequest;
import com.beacon.model.response.CreateTemplateResponse;
import com.beacon.model.response.GetTemplateResponse;
import com.beacon.repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.beacon.exception.TemplateException.TemplateAlreadyExists;
import static com.beacon.exception.TemplateException.TemplateNotFound;
import static com.beacon.model.Types.Channel;

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

    public GetTemplateResponse getTemplate(String notificationType, Channel channel) {
        Optional<Template> found = templateRepository.findByNotificationTypeAndChannel(notificationType, channel);
        if (found.isEmpty()) {
            throw new TemplateNotFound("No template found for " + notificationType + " and " + channel);
        }

        Template template = found.get();
        return new GetTemplateResponse(
                template.getId(),
                template.getChannel(),
                template.getNotificationType(),
                template.getBody(),
                template.getSubject(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
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
