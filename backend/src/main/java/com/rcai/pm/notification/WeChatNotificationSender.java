package com.rcai.pm.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WeChatNotificationSender extends WebhookNotificationSender {
    public WeChatNotificationSender(@Value("${app.notifications.wechat.webhook-url:}") String webhookUrl) {
        super(webhookUrl);
    }

    @Override public NotificationChannel channel() { return NotificationChannel.WECHAT; }
    @Override protected Map<String, ?> payload(Notification item) {
        return Map.of("msgtype", "text", "text", Map.of("content", text(item)));
    }
}
