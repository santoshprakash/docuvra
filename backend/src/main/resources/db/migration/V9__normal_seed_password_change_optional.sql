UPDATE users
SET force_password_change = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '10000000-0000-0000-0000-000000000001';
