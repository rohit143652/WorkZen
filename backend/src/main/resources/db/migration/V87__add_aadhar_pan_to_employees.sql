-- ============================================================
-- V87: Aadhar Card and PAN Card numbers on Employee
--
-- Nullable at the DB level (so this migration never breaks on existing employee rows that
-- predate this field), but mandatory going forward at the application layer - see
-- EmployeeRequest/EmployeeUpdateRequest's @NotBlank + @Pattern validation. Existing employees
-- without a value will simply need one filled in the next time they're edited.
--
-- Unique per tenant (a duplicate Aadhar/PAN across two employee records in the same company is
-- almost certainly a data-entry mistake or the same person being added twice) - MySQL unique
-- indexes allow any number of NULLs, so this never conflicts for the existing rows that don't
-- have a value yet.
-- ============================================================

ALTER TABLE employees
    ADD COLUMN aadhar_number VARCHAR(12) NULL AFTER pincode,
    ADD COLUMN pan_number    VARCHAR(10) NULL AFTER aadhar_number;

CREATE UNIQUE INDEX uq_employee_aadhar ON employees (client_company_id, aadhar_number);
CREATE UNIQUE INDEX uq_employee_pan    ON employees (client_company_id, pan_number);
