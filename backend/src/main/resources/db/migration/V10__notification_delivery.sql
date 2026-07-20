ALTER TABLE notifications
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_error VARCHAR(1000) NULL,
    ADD COLUMN last_attempt_at TIMESTAMP(6) NULL,
    ADD COLUMN sent_at TIMESTAMP(6) NULL;

CREATE INDEX idx_notification_dispatch ON notifications(status, channel, created_at);
