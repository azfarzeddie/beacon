package com.beacon.service.sms;

public interface SmsProvider {
    String getProviderName();
    boolean send(String to, String message);
}
