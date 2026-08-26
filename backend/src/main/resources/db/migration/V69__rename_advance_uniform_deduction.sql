-- ============================================================
-- V69: Rename employee_payroll_adjustments.advance_uniform_deduction
--
-- Architecture refactor Phase 5: this column name incorrectly implied it
-- could represent an employee advance. It never did - EmployeeAdvance and
-- advance_recovery_transactions are the ONLY source of truth for actual
-- advances and their recovery (see V62/V63). Renamed so the word "advance"
-- cannot appear in a generic manual-deduction column anywhere in the
-- schema. Existing data is preserved - this is a rename, not a drop/recreate.
-- ============================================================

ALTER TABLE employee_payroll_adjustments
    CHANGE COLUMN advance_uniform_deduction other_manual_deduction DECIMAL(10,2) NOT NULL DEFAULT 0;
