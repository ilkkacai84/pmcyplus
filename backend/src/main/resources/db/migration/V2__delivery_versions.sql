CREATE TABLE delivery_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    submitted_by BIGINT NOT NULL,
    submission_note VARCHAR(1000) NULL,
    status VARCHAR(30) NOT NULL,
    reviewed_by BIGINT NULL,
    review_opinion VARCHAR(1000) NULL,
    submitted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    reviewed_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_delivery_task_version UNIQUE (task_id, version_no),
    CONSTRAINT fk_delivery_task FOREIGN KEY (task_id) REFERENCES task_items(id),
    CONSTRAINT fk_delivery_submitter FOREIGN KEY (submitted_by) REFERENCES user_accounts(id),
    CONSTRAINT fk_delivery_reviewer FOREIGN KEY (reviewed_by) REFERENCES user_accounts(id)
);

CREATE INDEX idx_delivery_task_status ON delivery_versions(task_id, status);
