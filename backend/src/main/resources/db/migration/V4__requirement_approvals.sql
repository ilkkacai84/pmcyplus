ALTER TABLE requirements
    ADD COLUMN project_type VARCHAR(30) NOT NULL DEFAULT 'INTERNAL' AFTER project_id;

CREATE TABLE workflow_approval_steps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    name VARCHAR(120) NOT NULL,
    approver_role VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_workflow_approval_step UNIQUE (template_id, step_order),
    CONSTRAINT fk_approval_step_template FOREIGN KEY (template_id) REFERENCES workflow_templates(id)
);

CREATE TABLE approval_instances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    requirement_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    current_step INT NOT NULL,
    submitted_by BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_approval_requirement FOREIGN KEY (requirement_id) REFERENCES requirements(id),
    CONSTRAINT fk_approval_template FOREIGN KEY (template_id) REFERENCES workflow_templates(id),
    CONSTRAINT fk_approval_submitter FOREIGN KEY (submitted_by) REFERENCES user_accounts(id)
);

CREATE TABLE approval_actions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    instance_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    actor_id BIGINT NOT NULL,
    decision VARCHAR(30) NOT NULL,
    opinion VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_approval_action_instance FOREIGN KEY (instance_id) REFERENCES approval_instances(id),
    CONSTRAINT fk_approval_action_actor FOREIGN KEY (actor_id) REFERENCES user_accounts(id)
);

CREATE INDEX idx_approval_requirement ON approval_instances(requirement_id, created_at);
