package com.beacon.service;

import com.beacon.exception.NotificationException.NotificationNotAllowed;
import com.beacon.model.MessageDetails;
import com.beacon.model.NotificationContext;
import com.beacon.model.Types.*;
import com.beacon.model.entity.*;
import com.beacon.model.request.BulkNotificationRequest;
import com.beacon.model.request.SendNotificationRequest;
import com.beacon.model.response.BulkNotificationResponse;
import com.beacon.repository.*;
import com.beacon.repository.NotificationActionRecordRepository.StatusCount;
import com.beacon.service.factory.NotificationActionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    private final PreferenceRepository preferenceRepository;
    private final BulkNotificationJobRepository jobRepository;
    private final NotificationActionRecordRepository actionRepository;

    public NotificationService(UserRepository userRepository, NotificationActionFactory notificationActionFactory,
                               TemplateRepository templateRepository, TemplateService templateService,
                               PreferenceRepository preferenceRepository, BulkNotificationJobRepository jobRepository,
                               NotificationActionRecordRepository actionRepository) {
        this.userRepository = userRepository;
        this.notificationActionFactory = notificationActionFactory;
        this.templateRepository = templateRepository;
        this.templateService = templateService;
        this.preferenceRepository = preferenceRepository;
        this.jobRepository = jobRepository;
        this.actionRepository = actionRepository;
    }

    public void sendNotification(SendNotificationRequest request) {
        NotificationActionRecord record = new NotificationActionRecord();
        record.setChannel(request.getChannel());
        record.setNotificationType(request.getNotificationType());
        record.setUserExternalId(request.getUserExternalId());

        // if any error happens, let the exception handlers return the appropriate HTTP response by rethrowing the error
        try {
            NotificationContext context = sendSingleNotification(request);
            applySuccess(record, context, request.getChannel());
            actionRepository.save(record);
        } catch (NotificationNotAllowed notAllowed) {
            record.setStatus(ActionStatus.SKIPPED);
            record.setFailureReason(notAllowed.getLocalizedMessage());
            actionRepository.save(record);
            throw notAllowed;
        } catch (UserNotFoundException | TemplateNotFound | TemplateNotResolved | NotificationDispatchException e) {
            record.setStatus(ActionStatus.FAILED);
            record.setFailureReason(e.getLocalizedMessage());
            actionRepository.save(record);
            throw e;
        } catch (RuntimeException e) {
            record.setStatus(ActionStatus.FAILED);
            record.setFailureReason("Internal Server Error: " + e.getLocalizedMessage());
            actionRepository.save(record);
            throw e;
        }
    }

    private NotificationContext sendSingleNotification(SendNotificationRequest request) throws TemplateNotFound {
        // send a notification of notification type to the user with externalId at the given channel
        // check if a user with that externalId exists
        Optional<User> found = userRepository.findByExternalId(request.getUserExternalId());
        if (found.isEmpty()) {
            throw new UserNotFoundException("No user with externalId " + request.getUserExternalId() + " exists.");
        }

        User user = found.get();
        // check user preferences for DnD and applicable channels
        Optional<UserPreference> preference = preferenceRepository.findByUserIdAndNotificationTypeAndChannel(
                user.getId(), request.getNotificationType(), request.getChannel());
        if (preference.isPresent() && preference.get().getPreference() == PreferenceType.DISABLED) {
            throw new NotificationNotAllowed("User with externalId: " + request.getUserExternalId()
                    + " has disabled all notifications for type " + request.getNotificationType() + " on channel "
                    + request.getChannel().toString());
        }

        // fetch the template for this combination of notificationType and channel
        Optional<Template> templateFound = templateRepository.findByNotificationTypeAndChannel(request.getNotificationType(), request.getChannel());
        if (templateFound.isEmpty()) {
            throw new TemplateNotFound("No template found for "
                    + request.getNotificationType() + " and " + request.getChannel());
        }

        Template template = templateFound.get();
        String templateBody = template.getBody();
        String subjectTemplate = template.getSubject();
        String message, subject = null;
        try {
            message = templateService.resolveTemplate(templateBody, request.getTemplateVariables());
            if (subjectTemplate != null) {
                subject = templateService.resolveTemplate(subjectTemplate, request.getTemplateVariables());
            }
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
                .subject(subject)
                .deviceTokens(user.getDeviceTokens().stream().map(DeviceToken::getToken).toList())
                .build();
        boolean result = action.send(context);
        if (!result) {
            throw new NotificationDispatchException("Failed to send notification for "
                    + request.getNotificationType() + " at " + request.getChannel() + ". Please try again later.");
        }

        return context;
    }

    public BulkNotificationResponse sendBulkNotifications(BulkNotificationRequest request) {
        BulkNotificationJob job = createBulkJob(request);

        // immediately return the jobId and continue the execution in a coordinator virtual thread
        Thread.ofVirtual().name("bulk-job-" + job.getId())
                .start(() -> runJob(job, request.getNotifications()));

        return new BulkNotificationResponse(job.getId());
    }

    private void runJob(BulkNotificationJob job, List<SendNotificationRequest> notifications) {
        try {
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                job.setStatus(JobStatus.IN_PROGRESS);
                jobRepository.save(job);
                notifications.forEach(e -> executor.execute(() -> processNotificationAction(job, e)));
            }  // try with resources waits for the actions to finish in order to call the close() function on the executor
            finalizeJob(job.getId());
        } catch (RuntimeException e) {
            log.error("Bulk job {} failed", job.getId(), e);
            try {
                markJobFailed(job.getId(), e);
            } catch (RuntimeException failed) {
                // nothing above this thread can report the failure, so it has to be logged here
                log.error("Could not mark bulk job {} as FAILED", job.getId(), failed);
            }
        }
    }

    private void markJobFailed(UUID jobId, RuntimeException e) {
        BulkNotificationJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(Instant.now());
        Map<String, Object> summary = new HashMap<>();
        summary.put("error", e.getLocalizedMessage() + " caused by " + e.getCause());
        job.setJobSummary(summary);
        jobRepository.save(job);
    }

    private void finalizeJob(UUID jobId) {
        Map<ActionStatus, Integer> counts = actionRepository.countByStatusForJob(jobId).stream()
                .collect(Collectors.toMap(StatusCount::getStatus, StatusCount::getCount));
        BulkNotificationJob job = jobRepository.findById(jobId).orElseThrow();
        job.setSuccessCount(counts.getOrDefault(ActionStatus.SUCCESS, 0));
        job.setFailureCount(counts.getOrDefault(ActionStatus.FAILED, 0));
        job.setSkippedCount(counts.getOrDefault(ActionStatus.SKIPPED, 0));

        // an action whose record could not be written is not counted anywhere, so the job must not report COMPLETED
        int recorded = job.getSuccessCount() + job.getFailureCount() + job.getSkippedCount();
        int unrecorded = job.getActionCount() - recorded;
        if (unrecorded > 0) {
            log.error("Bulk job {} recorded only {} of {} actions", jobId, recorded, job.getActionCount());
            Map<String, Object> summary = job.getJobSummary() != null ? job.getJobSummary() : new HashMap<>();
            summary.put("unrecordedActions", unrecorded);
            job.setJobSummary(summary);
        }

        job.setStatus(job.getFailureCount() == 0 && unrecorded == 0 ? JobStatus.COMPLETED
                : job.getSuccessCount() == 0 ? JobStatus.FAILED : JobStatus.PARTIALLY_COMPLETED);
        job.setCompletedAt(Instant.now());
        jobRepository.save(job);
    }

    private void processNotificationAction(BulkNotificationJob job, SendNotificationRequest notificationAction) {
        NotificationActionRecord record = new NotificationActionRecord();
        record.setJob(job);
        record.setChannel(notificationAction.getChannel());
        record.setNotificationType(notificationAction.getNotificationType());
        record.setUserExternalId(notificationAction.getUserExternalId());

        try {
            try {
                NotificationContext context = sendSingleNotification(notificationAction);
                applySuccess(record, context, notificationAction.getChannel());
            } catch (NotificationNotAllowed notAllowed) {
                record.setStatus(ActionStatus.SKIPPED);
                record.setFailureReason(notAllowed.getLocalizedMessage());
                log.info("Job {} skipped notification for user {}: {}", job.getId(),
                        notificationAction.getUserExternalId(), notAllowed.getMessage());
            } catch (UserNotFoundException | TemplateNotFound | TemplateNotResolved | NotificationDispatchException e) {
                record.setStatus(ActionStatus.FAILED);
                record.setFailureReason(e.getLocalizedMessage());
                log.warn("Job {} failed to send notification for user {}: {}", job.getId(),
                        notificationAction.getUserExternalId(), e.getMessage());
            } catch (RuntimeException e) {
                record.setStatus(ActionStatus.FAILED);
                record.setFailureReason("Internal Server Error: " + e.getLocalizedMessage());
                log.error("Job {} hit an unexpected error sending notification for user {}", job.getId(),
                        notificationAction.getUserExternalId(), e);
            }

            actionRepository.save(record);
        } catch (RuntimeException e) {
            // the executor discards whatever a task throws, so a failed save would otherwise disappear silently
            log.error("Job {} could not record the notification action for user {}", job.getId(),
                    notificationAction.getUserExternalId(), e);
        }
    }

    private void applySuccess(NotificationActionRecord record, NotificationContext context, Channel channel) {
        record.setStatus(ActionStatus.SUCCESS);
        record.setMessageDetails(new MessageDetails(context.getMessage(), context.getSubject()));
        if (channel == Channel.PUSH) {
            record.setDeviceTokens(context.getDeviceTokens());
        }
    }

    private BulkNotificationJob createBulkJob(BulkNotificationRequest request) {
        BulkNotificationJob job = new BulkNotificationJob();
        job.setActionCount(request.getNotifications().size());
        job.setStatus(JobStatus.PENDING);

        return jobRepository.save(job);
    }
}
