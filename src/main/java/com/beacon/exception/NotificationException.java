package com.beacon.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

public class NotificationException {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class NotificationDispatchException extends RuntimeException {
        public NotificationDispatchException(String message) {
            super(message);
        }
    }
}
