package com.beacon.model.entity;

import com.beacon.model.Types.Channel;
import com.beacon.model.Types.PreferenceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_notification_preferences",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_pref_user_type_channel",
                columnNames = {"userId", "notificationType", "channel"}))
public class UserPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private String notificationType;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Channel channel;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PreferenceType preference;
    @Column(nullable = false)
    private boolean active = true;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
