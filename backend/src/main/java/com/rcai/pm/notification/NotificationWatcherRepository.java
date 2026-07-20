package com.rcai.pm.notification;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationWatcherRepository extends JpaRepository<NotificationWatcher, Long> {
    boolean existsByObjectTypeAndObjectIdAndUserId(String objectType, Long objectId, Long userId);
    @EntityGraph(attributePaths = "user")
    List<NotificationWatcher> findByObjectTypeAndObjectId(String objectType, Long objectId);
}
