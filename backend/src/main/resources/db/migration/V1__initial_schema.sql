CREATE TABLE departments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    parent_id BIGINT NULL,
    manager_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_department_parent FOREIGN KEY (parent_id) REFERENCES departments(id)
);

CREATE TABLE user_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    department_id BIGINT NULL,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NULL,
    user_type VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    language VARCHAR(10) NOT NULL DEFAULT 'zh-CN',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES departments(id)
);

ALTER TABLE departments
    ADD CONSTRAINT fk_department_manager FOREIGN KEY (manager_id) REFERENCES user_accounts(id);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_role_user FOREIGN KEY (user_id) REFERENCES user_accounts(id)
);

CREATE TABLE projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT NULL,
    project_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    manager_id BIGINT NOT NULL,
    customer_id BIGINT NULL,
    planned_start_at DATETIME(6) NULL,
    planned_end_at DATETIME(6) NULL,
    actual_start_at DATETIME(6) NULL,
    actual_end_at DATETIME(6) NULL,
    budget DECIMAL(18,2) NOT NULL DEFAULT 0,
    labor_cost DECIMAL(18,2) NOT NULL DEFAULT 0,
    other_cost DECIMAL(18,2) NOT NULL DEFAULT 0,
    merged_into_id BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_project_manager FOREIGN KEY (manager_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_project_customer FOREIGN KEY (customer_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_project_merged_into FOREIGN KEY (merged_into_id) REFERENCES projects(id)
);

CREATE TABLE project_members (
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    project_role VARCHAR(30) NOT NULL,
    PRIMARY KEY (project_id, user_id),
    CONSTRAINT fk_member_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES user_accounts(id)
);

CREATE TABLE milestones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    owner_id BIGINT NULL,
    planned_at DATETIME(6) NULL,
    actual_at DATETIME(6) NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_milestone_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_milestone_owner FOREIGN KEY (owner_id) REFERENCES user_accounts(id)
);

CREATE TABLE task_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    milestone_id BIGINT NULL,
    parent_task_id BIGINT NULL,
    title VARCHAR(240) NOT NULL,
    description TEXT NULL,
    owner_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    planned_start_at DATETIME(6) NULL,
    planned_end_at DATETIME(6) NULL,
    actual_start_at DATETIME(6) NULL,
    actual_end_at DATETIME(6) NULL,
    estimated_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    actual_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    merged_into_id BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_task_milestone FOREIGN KEY (milestone_id) REFERENCES milestones(id),
    CONSTRAINT fk_task_parent FOREIGN KEY (parent_task_id) REFERENCES task_items(id),
    CONSTRAINT fk_task_owner FOREIGN KEY (owner_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_task_merged_into FOREIGN KEY (merged_into_id) REFERENCES task_items(id)
);

CREATE TABLE task_participants (
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    participant_type VARCHAR(30) NOT NULL,
    planned_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (task_id, user_id),
    CONSTRAINT fk_participant_task FOREIGN KEY (task_id) REFERENCES task_items(id),
    CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES user_accounts(id)
);

CREATE TABLE requirements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    requirement_no VARCHAR(40) NOT NULL UNIQUE,
    source VARCHAR(30) NOT NULL,
    title VARCHAR(240) NOT NULL,
    description TEXT NULL,
    submitter_id BIGINT NOT NULL,
    customer_id BIGINT NULL,
    assignee_id BIGINT NULL,
    project_id BIGINT NULL,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    merged_into_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_requirement_submitter FOREIGN KEY (submitter_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_requirement_customer FOREIGN KEY (customer_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_requirement_assignee FOREIGN KEY (assignee_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_requirement_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_requirement_merged_into FOREIGN KEY (merged_into_id) REFERENCES requirements(id)
);

CREATE TABLE worklogs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    hours DECIMAL(10,2) NOT NULL,
    note VARCHAR(500) NULL,
    worked_on DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_worklog_task FOREIGN KEY (task_id) REFERENCES task_items(id),
    CONSTRAINT fk_worklog_user FOREIGN KEY (user_id) REFERENCES user_accounts(id)
);

CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_id BIGINT NULL,
    action VARCHAR(80) NOT NULL,
    object_type VARCHAR(40) NOT NULL,
    object_id BIGINT NULL,
    detail_json JSON NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_object (object_type, object_id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES user_accounts(id)
);

CREATE INDEX idx_project_manager ON projects(manager_id);
CREATE INDEX idx_project_customer ON projects(customer_id);
CREATE INDEX idx_task_project_status ON task_items(project_id, status);
CREATE INDEX idx_task_owner ON task_items(owner_id);
CREATE INDEX idx_requirement_status ON requirements(status);
CREATE INDEX idx_requirement_submitter ON requirements(submitter_id);
