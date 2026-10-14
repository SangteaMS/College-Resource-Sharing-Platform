ALTER TABLE resources ADD COLUMN removed_by BIGINT NULL;
ALTER TABLE resources ADD COLUMN removed_at TIMESTAMP NULL;
ALTER TABLE resources ADD COLUMN removal_reason VARCHAR(500) NULL;

ALTER TABLE resources
    ADD CONSTRAINT fk_resource_remover FOREIGN KEY (removed_by) REFERENCES app_users(id);
