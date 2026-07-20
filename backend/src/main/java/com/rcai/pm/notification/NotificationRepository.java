package com.rcai.pm.notification;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @EntityGraph(attributePaths = "recipient")
    List<Notification> findTop100ByRecipientIdAndChannelOrderByCreatedAtDesc(Long recipientId, NotificationChannel channel);
    long countByRecipientIdAndChannelAndStatus(Long recipientId, NotificationChannel channel, NotificationStatus status);
    @EntityGraph(attributePaths = "recipient")
    List<Notification> findTop100ByStatusOrderByCreatedAtAsc(NotificationStatus status);
}
