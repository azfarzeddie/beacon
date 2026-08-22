package com.beacon.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String externalId;
    @Column(nullable = false)
    private String name;
    @Column(unique = true)
    private String email;
    private String phone;
    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    List<DeviceToken> deviceTokens = new ArrayList<>();

    public void addDeviceToken(DeviceToken deviceToken) {
        deviceTokens.add(deviceToken);
        deviceToken.setCreatedAt(Instant.now());
        deviceToken.setUpdatedAt(Instant.now());
        deviceToken.setUser(this);
    }

    public void removeDeviceToken(DeviceToken deviceToken) {
        deviceTokens.remove(deviceToken);
        deviceToken.setUser(null);
    }
}
