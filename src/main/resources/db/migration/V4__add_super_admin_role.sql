ALTER TABLE app_users
    DROP CONSTRAINT chk_app_users_role;

ALTER TABLE app_users
    ALTER COLUMN school_id DROP NOT NULL;

ALTER TABLE app_users
    ADD CONSTRAINT chk_app_users_role
        CHECK (
            role IN (
                'SUPER_ADMIN',
                'ADMIN',
                'OPERATOR'
            )
        );

ALTER TABLE app_users
    ADD CONSTRAINT chk_app_users_school_scope
        CHECK (
            (
                role = 'SUPER_ADMIN'
                AND school_id IS NULL
            )
            OR
            (
                role IN ('ADMIN', 'OPERATOR')
                AND school_id IS NOT NULL
            )
        );