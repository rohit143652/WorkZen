-- ============================================================
-- V122: Outgoing email (sender) settings PER CLIENT COMPANY
--
-- V121 stored ONE platform-wide mail account. Each client company now gets its own: when an email
-- is sent for a company (employee invitation, password reset, ...) it goes out from THAT company's
-- sender. Resolution order at send time:
--   1. the company's own row (enabled)         -> client_company_id = that company
--   2. the platform default row (enabled)      -> client_company_id IS NULL
--   3. the MAIL_* environment variables
--
-- The row V121 created (if any) has client_company_id = NULL after this migration, so it simply
-- becomes the platform default - nothing already saved is lost or changed.
--
-- One row per company is enforced by the UNIQUE constraint. "At most one platform default" (the
-- NULL row) can't be expressed as a plain UNIQUE in MySQL (NULLs never collide), so the service
-- upserts that single row; only a Super Admin can write here, so a race is not a realistic concern.
-- ============================================================
ALTER TABLE mail_settings DROP INDEX uq_mail_settings_singleton;
ALTER TABLE mail_settings DROP COLUMN singleton_key;

ALTER TABLE mail_settings ADD COLUMN client_company_id BIGINT NULL AFTER id;
ALTER TABLE mail_settings
    ADD CONSTRAINT fk_mail_settings_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE;
ALTER TABLE mail_settings
    ADD CONSTRAINT uq_mail_settings_company UNIQUE (client_company_id);
