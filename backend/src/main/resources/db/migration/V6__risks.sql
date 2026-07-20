CREATE TABLE risks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    milestone_id BIGINT NULL,
    task_id BIGINT NULL,
    title VARCHAR(240) NOT NULL,
    description TEXT NULL,
    risk_level VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_risk_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_risk_milestone FOREIGN KEY (milestone_id) REFERENCES milestones(id),
    CONSTRAINT fk_risk_task FOREIGN KEY (task_id) REFERENCES task_items(id),
    CONSTRAINT fk_risk_owner FOREIGN KEY (owner_id) REFERENCES user_accounts(id)
);

CREATE TABLE risk_updates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    risk_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    note VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_risk_update_risk FOREIGN KEY (risk_id) REFERENCES risks(id),
    CONSTRAINT fk_risk_update_author FOREIGN KEY (author_id) REFERENCES user_accounts(id)
);
