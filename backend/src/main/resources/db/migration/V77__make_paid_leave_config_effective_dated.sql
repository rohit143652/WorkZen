-- ============================================================
-- V77: Refactor PaidLeaveConfiguration into an effective-dated model (architecture refactor Phase 9)
--
-- Was: one mutable row per tenant, PK = client_company_id - editing it
-- always changed "the" policy, with no way to know what policy applied to
-- a past month (the exact bug the original architecture audit flagged:
-- resolveCarryForward() always read "today's" config). Now: any number of
-- rows per tenant, each with its own effective_from/effective_to window -
-- LeavePolicyResolver picks the one whose window covers a given month.
-- Refactored in place (same pattern as V75 for PayrollSettings), not a
-- separate LeavePolicyHistory table.
--
-- Existing rows get effective_from = 2000-01-01 (safely before any real
-- leave data in this system) and status = ACTIVE, so they continue to
-- apply to every month already resolved - no historical leave balance's
-- applicable policy changes as a result of this migration.
-- ============================================================

ALTER TABLE paid_leave_configurations
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ADD COLUMN effective_from DATE NOT NULL DEFAULT '2000-01-01',
    ADD COLUMN effective_to DATE NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD INDEX idx_paid_leave_config_company_effective (client_company_id, effective_from);

UPDATE paid_leave_configurations SET created_at = updated_at WHERE updated_at IS NOT NULL;
