CREATE INDEX idx_guardian_devices_guardian_active
    ON guardian_devices (guardian_id)
    WHERE active = TRUE;