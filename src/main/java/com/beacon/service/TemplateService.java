package com.beacon.service;

import com.beacon.model.entity.Template;
import com.beacon.model.request.CreateTemplateRequest;
import com.beacon.model.response.CreateTemplateResponse;
import com.beacon.model.response.GetTemplateResponse;
import com.beacon.repository.TemplateRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
                request.notificationType(), request.channel()).isPresent()) {
            throw new TemplateAlreadyExists("A template for " + request.notificationType()
                    + " and " + request.channel() + " already exists. Please call the PUT endpoint to update it.");
        }

        Template template = new Template();
        template.setChannel(request.channel());
        template.setNotificationType(request.notificationType());
        template.setBody(request.templateBody());
        if (request.subject() != null) {
            template.setSubject(request.subject());
        }

        try {
            templateRepository.saveAndFlush(template);
        } catch (DataIntegrityViolationException e) {
            throw new TemplateAlreadyExists("A template for " + request.notificationType() + " and "
                    + request.channel() + " already exists. Please call the PUT endpoint to update it.");
        }

        return CreateTemplateResponse.from(template);
    }

    public GetTemplateResponse getTemplate(String notificationType, Channel channel) {
        Optional<Template> found = templateRepository.findByNotificationTypeAndChannel(notificationType, channel);
        if (found.isEmpty()) {
            throw new TemplateNotFound("No template found for " + notificationType + " and " + channel);
        }

        return GetTemplateResponse.from(found.get());
    }

    public GetTemplateResponse getTemplateById(UUID id) {
        Optional<Template> found = templateRepository.findById(id);
        if (found.isEmpty()) {
            throw new TemplateNotFound("No template found for ID: " + id);
        }

        return GetTemplateResponse.from(found.get());
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([^{}]+)}}");

    /**
     * Renders the template in a single pass: each placeholder is replaced by its variable's value, and the
     * substituted values are never re-scanned. A variable value that itself contains {{...}} is therefore
     * emitted literally, and the result does not depend on the map's iteration order.
     * Placeholders with no (or a null) value are collected and reported together by name.
     */
    public String resolveTemplate(String template, Map<String, String> templateVariables) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder message = new StringBuilder();
        Set<String> unresolved = new LinkedHashSet<>();

        while (matcher.find()) {
            String value = templateVariables.get(matcher.group(1));
            if (value == null) {
                unresolved.add(matcher.group(1));
                matcher.appendReplacement(message, Matcher.quoteReplacement(matcher.group()));
            } else {
                matcher.appendReplacement(message, Matcher.quoteReplacement(value));
            }
        }
        matcher.appendTail(message);

        if (!unresolved.isEmpty()) {
            throw new IllegalArgumentException("Unresolved template variables: " + String.join(", ", unresolved));
        }

        return message.toString();
    }
}
