-- ============================================================
-- V73: Payroll Run approval/finalization/locking (architecture refactor Phase 7)
--
-- cancellation_reason - now mandatory when cancelling (spec section 10).
-- reopened_at/reopened_by/reopen_reason - the controlled APPROVED -> CALCULATED
--   reopen path (spec section 11) - PAID payroll is never reopenable through
--   this workflow.
-- version - optimistic locking (spec section 30) so two admins cannot both
--   approve/recalculate/reopen the same run from a stale read; the second
--   save fails with a 409 instead of silently overwriting the first admin's
--   change. No other entity in the project uses @Version yet - introduced
--   here specifically because PayrollRun is where a lost-update race
--   genuinely matters (see GlobalExceptionHandler for the resulting 409).
-- ============================================================

ALTER TABLE payroll_runs
    ADD COLUMN cancellation_reason VARCHAR(500) AFTER cancelled_by,
    ADD COLUMN reopened_at DATETIME AFTER cancellation_reason,
    ADD COLUMN reopened_by BIGINT AFTER reopened_at,
    ADD COLUMN reopen_reason VARCHAR(500) AFTER reopened_by,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER reopen_reason;
