-- ============================================================
-- V70: Advance recovery traceability + manual settlement support
--
-- Architecture refactor Phase 5:
--   payroll_run_id - answers "which payroll recovered this amount?" for
--     PAYROLL-sourced rows (null for manual settlements).
--   source - PAYROLL (created by PayrollCalculationService during a
--     Payroll Run calculation) or MANUAL_SETTLEMENT (employee paid some or
--     all of the outstanding amount outside payroll - see
--     EmployeeAdvanceService.settlePartial()). Existing rows all predate
--     this column and are unambiguously PAYROLL-sourced, so the DEFAULT
--     backfills them correctly with no data migration needed.
-- ============================================================

ALTER TABLE advance_recovery_transactions
    ADD COLUMN payroll_run_id BIGINT AFTER advance_id,
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'PAYROLL' AFTER recovered_amount,
    ADD COLUMN created_by BIGINT AFTER created_at,
    ADD CONSTRAINT fk_advance_recovery_payroll_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs (id) ON DELETE SET NULL;

CREATE INDEX idx_advance_recovery_payroll_run ON advance_recovery_transactions (payroll_run_id);
