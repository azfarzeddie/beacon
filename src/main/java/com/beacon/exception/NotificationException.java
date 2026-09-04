package com.beacon.exception;

public class NotificationException {

    public static class NotificationDispatchException extends RuntimeException {
        public NotificationDispatchException(String message) {
            super(message);
        }
    }
}
