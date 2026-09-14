package com.beacon.service.push;

public interface PushProvider {
    String getProviderName();
    boolean send(String to, String message);
}
