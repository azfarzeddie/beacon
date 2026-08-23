package com.beacon.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.generator.Generator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "templates")
@Getter
@Setter
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String channel;
    @Column(nullable = false)
    private String notificationType;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
    private String subject;
    private Instant createdAt;
    private Instant updatedAt;
}
