package com.beacon.service;

import com.beacon.model.NotificationContext;
import com.beacon.model.entity.Template;
import com.beacon.model.entity.User;
import com.beacon.model.request.SendNotificationRequest;
import com.beacon.repository.TemplateRepository;
import com.beacon.repository.UserRepository;
import com.beacon.service.factory.NotificationActionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.beacon.exception.NotificationException.NotificationDispatchException;
import static com.beacon.exception.TemplateException.TemplateNotFound;
import static com.beacon.exception.TemplateException.TemplateNotResolved;
import static com.beacon.exception.UserException.UserNotFoundException;

@Slf4j
@Service
public class NotificationService {
    private final UserRepository userRepository;
    private final NotificationActionFactory notificationActionFactory;
    private final TemplateRepository templateRepository;
    private final TemplateService templateService;

    @Autowired
    public NotificationService(UserRepository userRepository, NotificationActionFactory notificationActionFactory, TemplateRepository templateRepository, TemplateService templateService) {
        this.userRepository = userRepository;
        this.notificationActionFactory = notificationActionFactory;
        this.templateRepository = templateRepository;
        this.templateService = templateService;
    }

    public void sendSingleNotification(SendNotificationRequest request) throws TemplateNotFound {
        // send a notification of notification type to the user with externalId at the given channel
        // check if a user with that externalId exists
        Optional<User> found = userRepository.findByExternalId(request.getUserExternalId());
        if (found.isEmpty()) {
            throw new UserNotFoundException("No user with externalId " + request.getUserExternalId() + " exists.");
        }

        User user = found.get();
        // check user preferences for DnD and applicable channels
        // TODO: Add APIs for user preferences
        // for now, assuming that the user has allowed sending all notifications

        // fetch the template for this combination of notificationType and channel
        Optional<Template> templateFound = templateRepository.findByNotificationTypeAndChannel(request.getNotificationType(), request.getChannel());
        if (templateFound.isEmpty()) {
            throw new TemplateNotFound("No template found for "
                    + request.getNotificationType() + " and " + request.getChannel());
        }

        String template = String.valueOf(templateFound.get());
        String message;
        try {
            message = templateService.resolveTemplate(template, request.getTemplateVariables());
        } catch (Exception e) {
            throw new TemplateNotResolved("Failed to resolve template: " + template
                    + " with variables: " + request.getTemplateVariables().toString());
        }

        // get the notification action for this channel
        NotificationAction action = notificationActionFactory.getAction(request.getChannel());
        NotificationContext context = NotificationContext.builder()
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .message(message)
                .subject(request.getSubject())
                .build();
        boolean result = action.send(context);
        if (!result) {
            throw new NotificationDispatchException("Failed to send notification for "
                    + request.getNotificationType() + " at " + request.getChannel() + ". Please try again later.");
        }
    }
}
