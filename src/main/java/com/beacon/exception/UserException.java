package com.beacon.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

public class UserException {
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class UserAlreadyExistsException extends RuntimeException {

        public UserAlreadyExistsException(String message) {
            super(message);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class UserNotFoundException extends RuntimeException {

        public UserNotFoundException(String message) {
            super(message);
        }
    }
}
