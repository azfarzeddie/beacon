package com.beacon.repository;

import com.beacon.model.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import static com.beacon.model.Types.Channel;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    Optional<Template> findByNotificationTypeAndChannel(String notificationType, Channel channel);
}
