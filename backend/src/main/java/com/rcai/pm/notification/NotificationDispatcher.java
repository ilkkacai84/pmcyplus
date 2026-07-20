package com.rcai.pm.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationDispatcher {
    private final NotificationRepository notifications;
    private final Map<NotificationChannel, NotificationSender> senders = new EnumMap<>(NotificationChannel.class);

    public NotificationDispatcher(NotificationRepository notifications, List<NotificationSender> senders) {
        this.notifications = notifications;
        senders.forEach(sender -> this.senders.put(sender.channel(), sender));
    }

    @Scheduled(fixedDelayString = "${app.notifications.dispatch-interval-ms:30000}")
    @Transactional
    public void dispatchPending() {
        dispatch(notifications.findTop100ByStatusOrderByCreatedAtAsc(NotificationStatus.PENDING));
    }

    public void dispatch(List<Notification> pending) {
        for (Notification notification : pending) {
            NotificationSender sender = senders.get(notification.getChannel());
            if (sender == null || !sender.configured()) continue;
            try {
                sender.send(notification);
                notification.markSent();
            } catch (Exception exception) {
                notification.markAttemptFailed(exception);
            }
        }
    }
}
