ALTER TABLE credentials
    ADD COLUMN deactivated_at TIMESTAMPTZ;

ALTER TABLE credentials
    ADD CONSTRAINT chk_credentials_expiration
        CHECK (
            expires_at IS NULL
            OR expires_at > issued_at
        );

CREATE INDEX idx_credentials_student
    ON credentials (student_id);

CREATE UNIQUE INDEX uk_credentials_active_student
    ON credentials (student_id)
    WHERE active = TRUE;

CREATE UNIQUE INDEX uk_student_guardians_primary_contact
    ON student_guardians (student_id)
    WHERE primary_contact = TRUE;