CREATE INDEX idx_student_guardians_guardian_student
    ON student_guardians (guardian_id, student_id);

CREATE INDEX idx_access_events_student_occurred
    ON access_events (student_id, occurred_at DESC, id DESC);
