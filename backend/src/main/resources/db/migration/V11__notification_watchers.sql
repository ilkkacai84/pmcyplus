CREATE TABLE notification_watchers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    object_type VARCHAR(40) NOT NULL,
    object_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_watcher UNIQUE (object_type, object_id, user_id),
    CONSTRAINT fk_notification_watcher_user FOREIGN KEY (user_id) REFERENCES user_accounts(id)
);

CREATE INDEX idx_notification_watcher_object ON notification_watchers(object_type, object_id);
