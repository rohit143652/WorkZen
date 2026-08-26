-- ============================================================
-- V75: Refactor PayrollSettings into an effective-dated model (architecture refactor Phase 8)
--
-- Was: one mutable row per tenant, PK = client_company_id - editing it
-- always changed "the" settings, with no way to know what rate applied to
-- a past month. Now: any number of rows per tenant, each with its own
-- effective_from/effective_to window - PayrollSettingsResolver picks the
-- one whose window covers a given payroll month. This is the same
-- entity/table refactored in place (spec section 3 explicitly allows
-- this instead of a separate History table), not a second system.
--
-- Existing rows get effective_from = 2000-01-01 (safely before any real
-- payroll data in this system) and status = ACTIVE, so they continue to
-- apply to every month calculated so far - no historical payroll's
-- applicable configuration changes as a result of this migration.
-- ============================================================

ALTER TABLE payroll_settings
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ADD COLUMN effective_from DATE NOT NULL DEFAULT '2000-01-01',
    ADD COLUMN effective_to DATE NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT,
    ADD INDEX idx_payroll_settings_company_effective (client_company_id, effective_from);

UPDATE payroll_settings SET created_at = updated_at, created_by = updated_by WHERE created_by IS NULL;
