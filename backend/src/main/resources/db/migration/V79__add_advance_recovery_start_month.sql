-- ============================================================
-- V79: Advance Recovery Start Month + Remarks
--
-- Genuine gap found during the Phase 1-9 completion audit: there was no
-- way to say "this advance shouldn't start being recovered until a future
-- payroll month" - EmployeeAdvanceService.computeMonthlyRecovery() would
-- try to recover from an advance the very first month it existed. Adds
-- recovery_start_year/recovery_start_month (backfilled from advance_date
-- for existing rows, so their behavior is unchanged - they already
-- started recovering the month they were granted) and a separate
-- free-text remarks column (distinct from the existing short "reason").
-- ============================================================

ALTER TABLE employee_advances
    ADD COLUMN recovery_start_year INT,
    ADD COLUMN recovery_start_month INT,
    ADD COLUMN remarks VARCHAR(500);

UPDATE employee_advances
SET recovery_start_year = YEAR(advance_date),
    recovery_start_month = MONTH(advance_date)
WHERE recovery_start_year IS NULL;

ALTER TABLE employee_advances
    MODIFY COLUMN recovery_start_year INT NOT NULL,
    MODIFY COLUMN recovery_start_month INT NOT NULL;
