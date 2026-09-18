package com.beacon.model.entity;

import com.beacon.model.MessageDetails;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.beacon.model.Types.ActionStatus;
import static com.beacon.model.Types.Channel;

@Entity
@Getter
@Setter
@Table(name = "notification_actions", indexes = {
        @Index(name = "idx_notification_actions_job_id", columnList = "job_id"),
        @Index(name = "idx_notification_actions_user_external_id", columnList = "userExternalId")
})
public class NotificationActionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    // null for notifications sent through the single-send API
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private BulkNotificationJob job;
    @Column(nullable = false)
    private String userExternalId;
    @Column(nullable = false)
    private String notificationType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;
    @Column(columnDefinition = "TEXT")
    private String failureReason;
    // resolved message and subject; null if the notification failed before template resolution
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private MessageDetails messageDetails;
    // only populated for PUSH
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> deviceTokens;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
