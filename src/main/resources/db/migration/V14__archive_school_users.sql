ALTER TABLE app_users
    ADD COLUMN archived_at TIMESTAMPTZ,
    ADD COLUMN archived_by BIGINT;

ALTER TABLE app_users
    ADD CONSTRAINT fk_app_users_archived_by
        FOREIGN KEY (archived_by)
        REFERENCES app_users (id)
        ON DELETE SET NULL;

CREATE INDEX idx_app_users_school_archived
    ON app_users (school_id, archived_at);
