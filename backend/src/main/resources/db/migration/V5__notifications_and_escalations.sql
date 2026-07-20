CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    title VARCHAR(240) NOT NULL,
    content VARCHAR(1000) NULL,
    object_type VARCHAR(40) NULL,
    object_id BIGINT NULL,
    escalation_level INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES user_accounts(id)
);

CREATE TABLE task_escalations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    escalation_level INT NOT NULL DEFAULT 0,
    last_notified_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_task_escalation UNIQUE (task_id),
    CONSTRAINT fk_escalation_task FOREIGN KEY (task_id) REFERENCES task_items(id)
);

CREATE INDEX idx_notification_recipient ON notifications(recipient_id, channel, created_at);
