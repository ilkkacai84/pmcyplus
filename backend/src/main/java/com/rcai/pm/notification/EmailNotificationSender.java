package com.rcai.pm.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender implements NotificationSender {
    private final JavaMailSenderImpl mailSender;
    private final boolean enabled;
    private final String from;

    public EmailNotificationSender(@Value("${app.notifications.email.enabled:false}") boolean enabled,
                                   @Value("${app.notifications.email.from:}") String from,
                                   @Value("${spring.mail.host:localhost}") String host,
                                   @Value("${spring.mail.port:25}") int port,
                                   @Value("${spring.mail.username:}") String username,
                                   @Value("${spring.mail.password:}") String password,
                                   @Value("${spring.mail.properties.mail.smtp.auth:false}") boolean auth,
                                   @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean startTls) {
        this.mailSender = new JavaMailSenderImpl();
        this.mailSender.setHost(host);
        this.mailSender.setPort(port);
        this.mailSender.setUsername(username);
        this.mailSender.setPassword(password);
        this.mailSender.getJavaMailProperties().put("mail.smtp.auth", auth);
        this.mailSender.getJavaMailProperties().put("mail.smtp.starttls.enable", startTls);
        this.enabled = enabled;
        this.from = from;
    }

    @Override public NotificationChannel channel() { return NotificationChannel.EMAIL; }
    @Override public boolean configured() { return enabled && !from.isBlank(); }

    @Override
    public void send(Notification notification) {
        String address = notification.getRecipient().getEmail();
        if (address == null || address.isBlank()) throw new IllegalStateException("收件人未配置邮箱");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(address);
        message.setSubject(notification.getTitle());
        message.setText(notification.getContent() == null ? notification.getTitle() : notification.getContent());
        mailSender.send(message);
    }
}
