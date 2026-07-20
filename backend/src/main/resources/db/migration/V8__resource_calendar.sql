CREATE TABLE work_calendar_days (
 calendar_date DATE PRIMARY KEY, workday BOOLEAN NOT NULL, standard_hours DECIMAL(5,2) NOT NULL DEFAULT 8,
 note VARCHAR(240) NULL
);
CREATE TABLE resource_capacities (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, capacity_date DATE NOT NULL,
 available_hours DECIMAL(5,2) NOT NULL, note VARCHAR(240) NULL,
 CONSTRAINT uk_resource_capacity UNIQUE(user_id,capacity_date),
 CONSTRAINT fk_capacity_user FOREIGN KEY(user_id) REFERENCES user_accounts(id)
);
