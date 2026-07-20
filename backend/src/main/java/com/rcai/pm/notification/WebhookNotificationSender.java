package com.rcai.pm.notification;

import org.springframework.web.client.RestClient;

import java.util.Map;

abstract class WebhookNotificationSender implements NotificationSender {
    private final String webhookUrl;
    private final RestClient client = RestClient.create();

    protected WebhookNotificationSender(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @Override public boolean configured() { return webhookUrl != null && !webhookUrl.isBlank(); }

    @Override
    public void send(Notification notification) {
        client.post().uri(webhookUrl).body(payload(notification)).retrieve().toBodilessEntity();
    }

    protected String text(Notification item) {
        return item.getRecipient().getDisplayName() + "：" + item.getTitle() + "\n"
            + (item.getContent() == null ? "" : item.getContent());
    }

    protected abstract Map<String, ?> payload(Notification notification);
}
