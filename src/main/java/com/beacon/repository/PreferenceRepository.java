package com.beacon.repository;

import com.beacon.model.Types.Channel;
import com.beacon.model.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PreferenceRepository extends JpaRepository<UserPreference, UUID> {
    Optional<UserPreference> findByNotificationTypeAndChannel(String notificationType, Channel channel);

    Optional<UserPreference> findByIdAndUserIdAndActiveTrue(UUID id, Long userId);

    List<UserPreference> findByUserIdAndActiveTrue(Long userId);

    Optional<UserPreference> findByUserIdAndNotificationTypeAndChannel(Long userId, String notificationType, Channel channel);

    Optional<UserPreference> findByUserIdAndNotificationTypeAndChannelAndActiveTrue(Long userId, String notificationType, Channel channel);
}
