package com.beacon.exception;

public class PreferenceException {
    public static class PreferenceAlreadyExists extends RuntimeException {
        public PreferenceAlreadyExists(String message) {
            super(message);
        }
    }

    public static class PreferenceNotFound extends RuntimeException {
        public PreferenceNotFound(String message) {
            super(message);
        }
    }

}
