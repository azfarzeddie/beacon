package com.beacon.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationContext {
    private String name;
    private String phone;
    private String email;
    private String template;
    private String message;
    private String subject;
}
