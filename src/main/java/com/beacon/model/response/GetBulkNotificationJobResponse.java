package com.beacon.model.response;

import com.beacon.model.entity.BulkNotificationJob;

import java.time.Instant;
import java.util.UUID;

import static com.beacon.model.Types.JobStatus;

public record GetBulkNotificationJobResponse(
        UUID jobId,
        JobStatus status,
        int actionCount,
        int successCount,
        int failureCount,
        int skippedCount,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt) {

    public static GetBulkNotificationJobResponse from(BulkNotificationJob job) {
        return new GetBulkNotificationJobResponse(job.getId(), job.getStatus(), job.getActionCount(),
                job.getSuccessCount(), job.getFailureCount(), job.getSkippedCount(), job.getCreatedAt(),
                job.getUpdatedAt(), job.getCompletedAt());
    }
}
