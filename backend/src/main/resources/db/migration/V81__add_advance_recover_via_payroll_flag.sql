-- ============================================================
-- V81: Pause/resume payroll-based recovery per advance
--
-- Lets an admin turn payroll recovery OFF for a specific advance without
-- touching its monthlyRecoveryAmount or status - e.g. the employee already
-- paid this month's installment in cash (via Settle Partial), so payroll
-- should skip it this run; admin turns it back ON before next month's run.
-- EmployeeAdvanceService.computeMonthlyRecovery() simply skips any
-- advance with recover_via_payroll = FALSE. Manual settlement (Settle
-- Partial/Full) is completely unaffected either way - it was always
-- independent of payroll and still is.
-- ============================================================

ALTER TABLE employee_advances
    ADD COLUMN recover_via_payroll BOOLEAN NOT NULL DEFAULT TRUE;
