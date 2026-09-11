ALTER TABLE guardians
    ADD COLUMN external_reference VARCHAR(50);

CREATE UNIQUE INDEX uk_guardians_school_external_reference_ci
    ON guardians (school_id, LOWER(external_reference))
    WHERE external_reference IS NOT NULL;
