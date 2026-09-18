package com.beacon.repository;

import com.beacon.model.entity.BulkNotificationJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BulkNotificationJobRepository extends JpaRepository<BulkNotificationJob, UUID> {
}
