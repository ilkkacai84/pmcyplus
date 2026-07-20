CREATE TABLE documents (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, project_id BIGINT NOT NULL, title VARCHAR(240) NOT NULL,
 customer_visible BOOLEAN NOT NULL DEFAULT FALSE, created_by BIGINT NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_document_project FOREIGN KEY(project_id) REFERENCES projects(id),
 CONSTRAINT fk_document_creator FOREIGN KEY(created_by) REFERENCES user_accounts(id)
);
CREATE TABLE document_versions (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, document_id BIGINT NOT NULL, version_no INT NOT NULL,
 storage_key VARCHAR(500) NOT NULL, file_name VARCHAR(255) NOT NULL, content_type VARCHAR(120) NULL,
 file_size BIGINT NOT NULL, note VARCHAR(1000) NULL, uploaded_by BIGINT NOT NULL,
 uploaded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT uk_document_version UNIQUE(document_id,version_no),
 CONSTRAINT fk_version_document FOREIGN KEY(document_id) REFERENCES documents(id),
 CONSTRAINT fk_version_uploader FOREIGN KEY(uploaded_by) REFERENCES user_accounts(id)
);
