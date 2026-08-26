-- ============================================================
-- V31: Designation base pay + per-employee salary override
--
-- designations.base_pay is the fixed payment structure for that
-- designation, applying to every employee holding it by default.
-- employees.salary_override is NULL for "use the designation's base pay"
-- and non-NULL for an explicit per-employee amount (e.g. the designation
-- pays 10000 but one employee was raised to 15000) - EmployeeService
-- computes the effective salary as override-if-present-else-base-pay.
-- ============================================================

ALTER TABLE designations
    ADD COLUMN base_pay DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER name;

ALTER TABLE employees
    ADD COLUMN salary_override DECIMAL(12,2) NULL AFTER designation;
