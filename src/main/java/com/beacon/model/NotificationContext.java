package com.beacon.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NotificationContext {
    private String name;
    private String phone;
    private String email;
    private String template;
    private String message;
    private String subject;
    private String notificationType;
    private List<String> deviceTokens;
}
