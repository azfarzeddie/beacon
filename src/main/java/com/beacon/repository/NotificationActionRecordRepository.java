package com.beacon.repository;

import com.beacon.model.entity.NotificationActionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

import static com.beacon.model.Types.ActionStatus;

public interface NotificationActionRecordRepository extends JpaRepository<NotificationActionRecord, UUID> {
    List<NotificationActionRecord> findByJobId(UUID jobId);

    List<NotificationActionRecord> findByUserExternalId(String userExternalId);

    // computed once all actions of a job are written, instead of incrementing counters from each thread
    @Query("""
            SELECT a.status AS status, COUNT(a) AS count
            FROM NotificationActionRecord a
            WHERE a.job.id = :jobId
            GROUP BY a.status
            """)
    List<StatusCount> countByStatusForJob(UUID jobId);

    interface StatusCount {
        ActionStatus getStatus();

        int getCount();
    }
}
