package com.beacon.model;

public class Types {
    public enum Platform {
        IOS, ANDROID, WEB
    }

    public enum Channel {
        SMS, EMAIL, PUSH
    }

    public enum PreferenceType {
        ENABLED, DISABLED
    }

    public enum JobStatus {
        PENDING, IN_PROGRESS, COMPLETED, PARTIALLY_COMPLETED, FAILED
    }

    public enum ActionStatus {
        SUCCESS, FAILED, SKIPPED
    }
}
