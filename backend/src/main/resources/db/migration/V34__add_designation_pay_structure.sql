-- ============================================================
-- V34: Proper payroll structure on designations
--
-- The existing base_pay column (added in V31) becomes "Basic Salary" -
-- same column, just a clearer name at the Java/API layer going forward
-- (DesignationService/Employee* now expose it as basicSalary). This
-- migration adds the two missing pieces of a real payroll structure:
-- PF (Provident Fund) deduction percentage, and a fixed "other
-- deductions" amount (uniform, canteen, etc.) - so Net Salary can be
-- computed as Basic - PF - Other Deductions, not just a single number.
-- ============================================================

ALTER TABLE designations
    ADD COLUMN pf_percentage DECIMAL(5,2) NOT NULL DEFAULT 12.00 AFTER base_pay;

ALTER TABLE designations
    ADD COLUMN other_deductions DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER pf_percentage;
