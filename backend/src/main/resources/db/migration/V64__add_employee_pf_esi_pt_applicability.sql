-- ============================================================
-- V64: Per-employee PF/ESI/PT applicability
--
-- "Never assume every employee has the same payroll rules" - these three
-- flags let an individual employee opt out of a deduction the tenant's
-- PayrollSettings would otherwise apply (e.g. a fixed-payment employee
-- with PF/ESI/PT all off). Default TRUE preserves existing behaviour for
-- every already-created employee.
-- ============================================================

ALTER TABLE employees
    ADD COLUMN pf_applicable  BOOLEAN NOT NULL DEFAULT TRUE AFTER status,
    ADD COLUMN esi_applicable BOOLEAN NOT NULL DEFAULT TRUE AFTER pf_applicable,
    ADD COLUMN pt_applicable  BOOLEAN NOT NULL DEFAULT TRUE AFTER esi_applicable;
