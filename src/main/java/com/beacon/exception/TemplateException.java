package com.beacon.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

public class TemplateException {
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TemplateAlreadyExists extends RuntimeException {

        public TemplateAlreadyExists(String message) {
            super(message);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TemplateNotFound extends RuntimeException {

        public TemplateNotFound(String message) {
            super(message);
        }
    }
}
