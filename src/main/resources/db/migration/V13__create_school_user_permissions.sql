CREATE TABLE app_user_permissions (
    user_id BIGINT NOT NULL,
    module_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, module_key),

    CONSTRAINT fk_app_user_permissions_user
        FOREIGN KEY (user_id)
        REFERENCES app_users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_app_user_permissions_module
    ON app_user_permissions (module_key);

CREATE INDEX idx_access_events_recorded_by
    ON access_events (recorded_by);

-- Conserva el acceso que tenían los operadores existentes antes de
-- introducir permisos configurables por módulo.
INSERT INTO app_user_permissions (user_id, module_key)
SELECT id, permission_key
FROM app_users
CROSS JOIN (
    VALUES
        ('DASHBOARD'),
        ('ACCESS_SCANNER'),
        ('STUDENTS'),
        ('CREDENTIALS'),
        ('GUARDIANS')
) AS existing_permissions(permission_key)
WHERE role = 'OPERATOR'
ON CONFLICT DO NOTHING;
