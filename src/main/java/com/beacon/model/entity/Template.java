package com.beacon.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

import static com.beacon.model.Types.Channel;

@Entity
@Table(name = "templates")
@Getter
@Setter
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Channel channel;
    @Column(nullable = false)
    private String notificationType;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
    private String subject;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
