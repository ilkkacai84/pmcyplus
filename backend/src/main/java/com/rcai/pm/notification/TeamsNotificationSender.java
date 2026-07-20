package com.rcai.pm.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TeamsNotificationSender extends WebhookNotificationSender {
    public TeamsNotificationSender(@Value("${app.notifications.teams.webhook-url:}") String webhookUrl) {
        super(webhookUrl);
    }

    @Override public NotificationChannel channel() { return NotificationChannel.TEAMS; }
    @Override protected Map<String, ?> payload(Notification item) { return Map.of("text", text(item)); }
}
