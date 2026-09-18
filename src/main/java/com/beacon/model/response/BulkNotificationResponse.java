package com.beacon.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class BulkNotificationResponse {
    private UUID jobId;
}
