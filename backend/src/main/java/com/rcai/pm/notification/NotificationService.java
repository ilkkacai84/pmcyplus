package com.rcai.pm.notification;

import com.rcai.pm.common.ApiException;
import com.rcai.pm.user.UserAccount;
import com.rcai.pm.user.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserAccountRepository users;
    private final NotificationWatcherRepository watchers;

    public NotificationService(NotificationRepository notifications, UserAccountRepository users,
                               NotificationWatcherRepository watchers) {
        this.notifications = notifications; this.users = users; this.watchers = watchers;
    }

    public NotificationInbox inbox(Authentication authentication) {
        UserAccount user = current(authentication);
        List<NotificationView> items = notifications
            .findTop100ByRecipientIdAndChannelOrderByCreatedAtDesc(user.getId(), NotificationChannel.SITE)
            .stream().map(NotificationView::from).toList();
        long unread = notifications.countByRecipientIdAndChannelAndStatus(
            user.getId(), NotificationChannel.SITE, NotificationStatus.SENT
        );
        return new NotificationInbox(unread, items);
    }

    @Transactional
    public void notify(Collection<UserAccount> recipients, String eventType, String title, String content,
                       String objectType, Long objectId, int escalationLevel) {
        Map<Long, UserAccount> allRecipients = new LinkedHashMap<>();
        recipients.stream().filter(java.util.Objects::nonNull)
            .forEach(recipient -> allRecipients.put(recipient.getId(), recipient));
        if (objectType != null && objectId != null) {
            watchers.findByObjectTypeAndObjectId(objectType, objectId).stream()
                .map(NotificationWatcher::getUser).filter(UserAccount::isEnabled)
                .forEach(recipient -> allRecipients.put(recipient.getId(), recipient));
        }
        allRecipients.values().forEach(recipient -> {
            for (NotificationChannel channel : NotificationChannel.values()) {
                notifications.save(new Notification(recipient, channel, eventType, title, content,
                    objectType, objectId, escalationLevel));
            }
        });
    }

    @Transactional
    public void watch(String objectType, Long objectId, Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return;
        for (UserAccount user : users.findAllById(userIds)) {
            if (user.isEnabled() && !watchers.existsByObjectTypeAndObjectIdAndUserId(objectType, objectId, user.getId())) {
                watchers.save(new NotificationWatcher(objectType, objectId, user));
            }
        }
    }

    @Transactional
    public void markRead(Long id, Authentication authentication) {
        UserAccount user = current(authentication);
        Notification notification = notifications.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "通知不存在"));
        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权处理该通知");
        }
        notification.markRead();
    }

    private UserAccount current(Authentication authentication) {
        return users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
    }

    public record NotificationInbox(long unread, List<NotificationView> items) {}
    public record NotificationView(Long id, NotificationStatus status, String eventType, String title, String content,
                                   String objectType, Long objectId, int escalationLevel, Instant createdAt) {
        static NotificationView from(Notification item) {
            return new NotificationView(item.getId(), item.getStatus(), item.getEventType(), item.getTitle(),
                item.getContent(), item.getObjectType(), item.getObjectId(), item.getEscalationLevel(), item.getCreatedAt());
        }
    }
}
