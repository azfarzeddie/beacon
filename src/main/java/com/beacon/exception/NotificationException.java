package com.beacon.exception;

public class NotificationException {

    public static class NotificationDispatchException extends RuntimeException {
        public NotificationDispatchException(String message) {
            super(message);
        }
    }

    public static class NotificationNotAllowed extends RuntimeException {
        public NotificationNotAllowed(String message) {
            super(message);
        }
    }

    public static class BulkNotificationJobNotFound extends RuntimeException {
        public BulkNotificationJobNotFound(String message) {
            super(message);
        }
    }
}
