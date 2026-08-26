-- ============================================================
-- V76: Payroll configuration snapshot on PayrollRunEmployee (architecture refactor Phase 8)
--
-- Stores the ACTUAL rates used for this employee's calculation, not just
-- the resulting amounts - so a payslip/audit view can explain "12% of
-- 15,000 = 1,800" for a payroll from months ago even after the tenant's
-- current PF rate has since changed to something else. Nullable because
-- a rate is meaningless when its deduction wasn't applicable that month
-- (e.g. epf_employee_percent_used is null when the employee had PF off).
-- ============================================================

ALTER TABLE payroll_run_employees
    ADD COLUMN epf_employee_percent_used DECIMAL(5,2) AFTER epf_employer,
    ADD COLUMN epf_employer_percent_used DECIMAL(5,2) AFTER epf_employee_percent_used,
    ADD COLUMN esi_employee_percent_used DECIMAL(5,2) AFTER esi_employer,
    ADD COLUMN esi_employer_percent_used DECIMAL(5,2) AFTER esi_employee_percent_used;
