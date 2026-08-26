-- ============================================================
-- V44: Remove the legacy Designation/Employee pay-structure columns
--
-- Before the dedicated Salary Structure module existed, a simple payroll
-- structure lived directly on designations (base_pay/pf_percentage/
-- other_deductions) with per-employee overrides (salary_override/
-- pf_percentage_override/other_deductions_override). That duplicated what
-- salary_structure_module now does properly (configurable components,
-- calculation types, full history via employee_salary_structures) and was
-- a source of confusion - two different places an employee's "salary"
-- could apparently live. An employee's salary is now assigned exclusively
-- via a Salary Structure; Designations are pure organisational master data.
-- ============================================================

ALTER TABLE designations
    DROP COLUMN base_pay,
    DROP COLUMN pf_percentage,
    DROP COLUMN other_deductions;

ALTER TABLE employees
    DROP COLUMN salary_override,
    DROP COLUMN pf_percentage_override,
    DROP COLUMN other_deductions_override;
