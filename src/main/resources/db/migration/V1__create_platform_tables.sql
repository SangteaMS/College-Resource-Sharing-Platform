CREATE TABLE programmes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE semesters (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    programme_id BIGINT NOT NULL,
    semester_number INT NOT NULL,
    UNIQUE (programme_id, semester_number),
    UNIQUE (id, programme_id),
    CONSTRAINT fk_semester_programme FOREIGN KEY (programme_id) REFERENCES programmes(id)
);

CREATE TABLE subjects (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    programme_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (programme_id, semester_id, name),
    UNIQUE (id, programme_id, semester_id),
    CONSTRAINT fk_subject_programme FOREIGN KEY (programme_id) REFERENCES programmes(id),
    CONSTRAINT fk_subject_semester FOREIGN KEY (semester_id) REFERENCES semesters(id)
);

CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(20) NOT NULL,
    preferred_programme_id BIGINT NULL,
    current_semester_id BIGINT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_programme FOREIGN KEY (preferred_programme_id) REFERENCES programmes(id),
    CONSTRAINT fk_user_semester FOREIGN KEY (current_semester_id) REFERENCES semesters(id)
);

CREATE TABLE resources (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(500),
    programme_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    file_format VARCHAR(12) NOT NULL,
    display_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(80) NOT NULL UNIQUE,
    file_size_bytes BIGINT NOT NULL,
    exam_year INT NULL,
    exam_type VARCHAR(20) NULL,
    exam_session VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL,
    rejection_reason VARCHAR(500) NULL,
    uploaded_by BIGINT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    download_count BIGINT NOT NULL DEFAULT 0,
    demo_resource BOOLEAN NOT NULL DEFAULT FALSE,
    removed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_resource_programme_semester FOREIGN KEY (semester_id, programme_id) REFERENCES semesters(id, programme_id),
    CONSTRAINT fk_resource_subject_selection FOREIGN KEY (subject_id, programme_id, semester_id) REFERENCES subjects(id, programme_id, semester_id),
    CONSTRAINT fk_resource_uploader FOREIGN KEY (uploaded_by) REFERENCES app_users(id),
    CONSTRAINT fk_resource_reviewer FOREIGN KEY (reviewed_by) REFERENCES app_users(id)
);

CREATE TABLE usage_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(20) NOT NULL,
    resource_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    event_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usage_resource FOREIGN KEY (resource_id) REFERENCES resources(id),
    CONSTRAINT fk_usage_actor FOREIGN KEY (actor_id) REFERENCES app_users(id)
);

CREATE INDEX idx_semesters_programme ON semesters(programme_id);
CREATE INDEX idx_subjects_programme_semester ON subjects(programme_id, semester_id);
CREATE INDEX idx_resources_public_browse ON resources(status, programme_id, semester_id, subject_id, resource_type);
CREATE INDEX idx_resources_uploaded_at ON resources(uploaded_at);
CREATE INDEX idx_usage_resource_date ON usage_events(resource_id, event_at);
