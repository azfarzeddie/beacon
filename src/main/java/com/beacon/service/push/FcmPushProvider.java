package com.beacon.service.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.Supplier;

@Component
public class FcmPushProvider implements PushProvider {
    private static final String PROVIDER_NAME = "firebase";

    private final Supplier<FirebaseMessaging> messagingSupplier;
    private volatile FirebaseMessaging messaging;

    @Autowired
    public FcmPushProvider(@Value("${spring.push.firebase.credentials-path}") String credentialsPath) {
        this(() -> buildMessaging(credentialsPath));
    }

    // test seam: bypasses real Firebase initialization entirely.
    FcmPushProvider(Supplier<FirebaseMessaging> messagingSupplier) {
        this.messagingSupplier = messagingSupplier;
    }

    private static FirebaseMessaging buildMessaging(String credentialsPath) {
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new FileInputStream(credentialsPath)))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            return FirebaseMessaging.getInstance(app);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Firebase credentials from: " + credentialsPath, e);
        }
    }

    // built lazily and memoized so a blank/invalid credentials-path (an inactive,
    // unconfigured provider) never fails at Spring startup - only if this provider
    // is actually the configured/active one and gets used.
    private FirebaseMessaging messaging() {
        FirebaseMessaging result = messaging;
        if (result == null) {
            synchronized (this) {
                result = messaging;
                if (result == null) {
                    messaging = result = messagingSupplier.get();
                }
            }
        }
        return result;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean send(String to, String message) {
        Message fcmMessage = Message.builder()
                .setToken(to)
                .setNotification(Notification.builder().setBody(message).build())
                .build();
        try {
            messaging().send(fcmMessage);
            return true;
        } catch (FirebaseMessagingException | IllegalStateException e) {
            return false;
        }
    }
}
