ALTER TABLE users
    ADD COLUMN IF NOT EXISTS mobile VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS force_password_change BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'users'
          AND constraint_name = 'users_email_key'
    ) THEN
        ALTER TABLE users DROP CONSTRAINT users_email_key;
    END IF;
END $$;

UPDATE users
SET mobile = CASE role
    WHEN 'NORMAL_USER' THEN '+10000000001'
    WHEN 'STAFF' THEN '+10000000002'
    WHEN 'SUPERVISOR' THEN '+10000000003'
    ELSE '+10000000000'
END
WHERE mobile IS NULL;

UPDATE users
SET force_password_change = TRUE
WHERE role IN ('NORMAL_USER', 'STAFF');

ALTER TABLE users
    ALTER COLUMN mobile SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uk_users_username UNIQUE (username),
    ADD CONSTRAINT uk_users_role_email UNIQUE (role, email),
    ADD CONSTRAINT uk_users_role_mobile UNIQUE (role, mobile);
