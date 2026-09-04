package com.beacon.exception;

public class TemplateException {

    public static class TemplateAlreadyExists extends RuntimeException {
        public TemplateAlreadyExists(String message) {
            super(message);
        }
    }

    public static class TemplateNotFound extends RuntimeException {
        public TemplateNotFound(String message) {
            super(message);
        }
    }

    public static class TemplateNotResolved extends RuntimeException {
        public TemplateNotResolved(String message) {
            super(message);
        }
    }
}
