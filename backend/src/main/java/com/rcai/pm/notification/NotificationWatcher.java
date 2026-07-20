package com.rcai.pm.notification;

import com.rcai.pm.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "notification_watchers", uniqueConstraints =
    @UniqueConstraint(name = "uk_notification_watcher", columnNames = {"object_type", "object_id", "user_id"}))
public class NotificationWatcher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "object_type", nullable = false, length = 40)
    private String objectType;
    @Column(name = "object_id", nullable = false)
    private Long objectId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    protected NotificationWatcher() {}
    public NotificationWatcher(String objectType, Long objectId, UserAccount user) {
        this.objectType = objectType;
        this.objectId = objectId;
        this.user = user;
    }
    public UserAccount getUser() { return user; }
}
