CREATE TABLE workflow_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    project_type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_workflow_project_type UNIQUE (project_type)
);

CREATE TABLE workflow_transitions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    object_type VARCHAR(30) NOT NULL,
    from_status VARCHAR(40) NOT NULL,
    to_status VARCHAR(40) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    requires_reason BOOLEAN NOT NULL DEFAULT FALSE,
    notification_event VARCHAR(80) NULL,
    CONSTRAINT uk_workflow_transition UNIQUE (template_id, object_type, from_status, to_status),
    CONSTRAINT fk_transition_template FOREIGN KEY (template_id) REFERENCES workflow_templates(id)
);

CREATE TABLE workflow_transition_roles (
    transition_id BIGINT NOT NULL,
    role_code VARCHAR(40) NOT NULL,
    PRIMARY KEY (transition_id, role_code),
    CONSTRAINT fk_transition_role FOREIGN KEY (transition_id) REFERENCES workflow_transitions(id)
);
