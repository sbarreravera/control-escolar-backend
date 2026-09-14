ALTER TABLE schools
    ADD COLUMN guardian_self_registration_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN guardian_registration_token VARCHAR(64),
    ADD COLUMN guardian_min_per_student INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN guardian_max_per_student INTEGER NOT NULL DEFAULT 99;

UPDATE schools
SET guardian_registration_token =
    REPLACE(gen_random_uuid()::text, '-', '') ||
    REPLACE(gen_random_uuid()::text, '-', '')
WHERE guardian_registration_token IS NULL;

ALTER TABLE schools
    ALTER COLUMN guardian_registration_token SET NOT NULL;

ALTER TABLE schools
    ADD CONSTRAINT uq_schools_guardian_registration_token
        UNIQUE (guardian_registration_token),
    ADD CONSTRAINT ck_schools_guardian_min_per_student
        CHECK (guardian_min_per_student >= 0),
    ADD CONSTRAINT ck_schools_guardian_max_per_student
        CHECK (guardian_max_per_student >= 1),
    ADD CONSTRAINT ck_schools_guardian_range
        CHECK (guardian_max_per_student >= guardian_min_per_student);
