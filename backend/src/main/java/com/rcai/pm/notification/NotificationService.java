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
import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserAccountRepository users;

    public NotificationService(NotificationRepository notifications, UserAccountRepository users) {
        this.notifications = notifications; this.users = users;
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
        recipients.stream().filter(java.util.Objects::nonNull).distinct().forEach(recipient -> {
            for (NotificationChannel channel : NotificationChannel.values()) {
                notifications.save(new Notification(recipient, channel, eventType, title, content,
                    objectType, objectId, escalationLevel));
            }
        });
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
