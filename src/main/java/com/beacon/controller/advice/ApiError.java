package com.beacon.controller.advice;

import com.beacon.exception.NotificationException.BulkNotificationJobNotFound;
import com.beacon.exception.NotificationException.ChannelNotAvailableForUser;
import com.beacon.exception.NotificationException.NotificationDispatchException;
import com.beacon.exception.NotificationException.NotificationNotAllowed;
import com.beacon.exception.PreferenceException.PreferenceAlreadyExists;
import com.beacon.exception.PreferenceException.PreferenceNotFound;
import com.beacon.exception.TemplateException.TemplateAlreadyExists;
import com.beacon.exception.TemplateException.TemplateNotFound;
import com.beacon.exception.TemplateException.TemplateNotResolved;
import com.beacon.exception.UserException.UserAlreadyExistsException;
import com.beacon.exception.UserException.UserNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.Optional;

/**
 * Maps each domain exception to the error code (the constant name) and HTTP status it is reported with.
 */
@Getter
@RequiredArgsConstructor
public enum ApiError {
    USER_ALREADY_EXISTS(UserAlreadyExistsException.class, HttpStatus.CONFLICT),
    USER_NOT_FOUND(UserNotFoundException.class, HttpStatus.NOT_FOUND),
    PREFERENCE_ALREADY_EXISTS(PreferenceAlreadyExists.class, HttpStatus.CONFLICT),
    PREFERENCE_NOT_FOUND(PreferenceNotFound.class, HttpStatus.NOT_FOUND),
    TEMPLATE_ALREADY_EXISTS(TemplateAlreadyExists.class, HttpStatus.CONFLICT),
    TEMPLATE_NOT_FOUND(TemplateNotFound.class, HttpStatus.NOT_FOUND),
    TEMPLATE_NOT_RESOLVED(TemplateNotResolved.class, HttpStatus.UNPROCESSABLE_ENTITY),
    NOTIFICATION_DISPATCH_FAILED(NotificationDispatchException.class, HttpStatus.SERVICE_UNAVAILABLE),
    NOTIFICATION_NOT_ALLOWED(NotificationNotAllowed.class, HttpStatus.FORBIDDEN),
    CHANNEL_NOT_AVAILABLE_FOR_USER(ChannelNotAvailableForUser.class, HttpStatus.UNPROCESSABLE_CONTENT),
    BULK_NOTIFICATION_JOB_NOT_FOUND(BulkNotificationJobNotFound.class, HttpStatus.NOT_FOUND);

    private final Class<? extends Exception> exceptionType;
    private final HttpStatus status;

    public static Optional<ApiError> of(Exception e) {
        for (ApiError error : values()) {
            if (error.exceptionType.isInstance(e)) {
                return Optional.of(error);
            }
        }
        return Optional.empty();
    }
}
