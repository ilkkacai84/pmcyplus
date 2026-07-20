CREATE TABLE merge_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    object_type VARCHAR(30) NOT NULL,
    source_ids_json TEXT NOT NULL,
    target_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    mapping_data TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_merge_target (object_type, target_id),
    CONSTRAINT fk_merge_operator FOREIGN KEY (operator_id) REFERENCES user_accounts(id)
);
