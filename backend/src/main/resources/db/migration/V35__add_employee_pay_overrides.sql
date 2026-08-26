-- ============================================================
-- V35: Proper payroll structure on employees (personal overrides)
--
-- The existing salary_override column (added in V31) becomes
-- "Basic Salary Override" (same column, clearer Java/API name:
-- basicSalaryOverride). Adds the matching PF%/other-deductions override
-- columns. Each of the three is INDEPENDENTLY nullable: an employee can
-- have just a basic salary raise (the common case - matches "designation
-- pays 10000, this one employee got raised to 15000") while still
-- inheriting the designation's PF% and other deductions, or override any
-- combination of the three.
-- ============================================================

ALTER TABLE employees
    ADD COLUMN pf_percentage_override DECIMAL(5,2) NULL AFTER salary_override;

ALTER TABLE employees
    ADD COLUMN other_deductions_override DECIMAL(12,2) NULL AFTER pf_percentage_override;
