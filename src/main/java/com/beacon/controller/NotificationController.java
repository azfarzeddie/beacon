package com.beacon.controller;

import com.beacon.model.request.BulkNotificationRequest;
import com.beacon.model.request.SendNotificationRequest;
import com.beacon.model.response.BulkNotificationResponse;
import com.beacon.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    ResponseEntity<Void> sendNotification(@Valid @RequestBody SendNotificationRequest request) {
        notificationService.sendNotification(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/bulk")
    ResponseEntity<BulkNotificationResponse> sendBulkNotifications(@Valid @RequestBody BulkNotificationRequest request) {
        BulkNotificationResponse response = notificationService.sendBulkNotifications(request);
        return ResponseEntity.accepted().body(response);
    }
}
