-- ============================================================
-- V72: Bonus/Overtime/Arrears for manual payroll adjustments
--
-- Architecture refactor Phase 6: PayrollCalculationService's Total
-- Earnings breakdown needs Bonus/Overtime/Arrears as their own explicit
-- figures, kept separate from Other Manual Deduction and Allowance
-- (same narrow, explicit-field design established in Phase 5 - no
-- generic "everything" table).
-- ============================================================

ALTER TABLE employee_payroll_adjustments
    ADD COLUMN bonus    DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER allowance,
    ADD COLUMN overtime DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER bonus,
    ADD COLUMN arrears  DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER overtime;
