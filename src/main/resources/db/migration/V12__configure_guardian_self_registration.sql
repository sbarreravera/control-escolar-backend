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

CREATE OR REPLACE FUNCTION enforce_student_guardian_maximum()
RETURNS TRIGGER AS $$
DECLARE
    maximum_allowed INTEGER;
    current_count INTEGER;
BEGIN
    PERFORM 1
    FROM students
    WHERE id = NEW.student_id
    FOR UPDATE;

    SELECT school.guardian_max_per_student
    INTO maximum_allowed
    FROM students student
    JOIN schools school ON school.id = student.school_id
    WHERE student.id = NEW.student_id;

    SELECT COUNT(*)
    INTO current_count
    FROM student_guardians
    WHERE student_id = NEW.student_id;

    IF current_count >= maximum_allowed THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            MESSAGE = 'student_guardian_maximum_exceeded';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_student_guardian_maximum
BEFORE INSERT ON student_guardians
FOR EACH ROW
EXECUTE FUNCTION enforce_student_guardian_maximum();
