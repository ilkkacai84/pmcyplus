package com.rcai.pm.notification;

public interface NotificationSender {
    NotificationChannel channel();
    boolean configured();
    void send(Notification notification);
}
