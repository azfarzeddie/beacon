package com.beacon.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.beacon.model.Types.JobStatus;

@Data
@AllArgsConstructor
public class GetBulkNotificationJobResponse {
    private UUID jobId;
    private JobStatus status;
    private int actionCount;
    private int successCount;
    private int failureCount;
    private int skippedCount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
}
