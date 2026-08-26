-- ============================================================
-- V7: Force password change after admin-issued temporary passwords
-- ============================================================

ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE AFTER password_changed_at;
