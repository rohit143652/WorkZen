-- ============================================================
-- V36: Sample payroll structure for CLI0001
--
-- Realistic monthly INR figures for the 8 designations seeded in V22.
-- PF stays at the standard 12% for all of them (the column default from
-- V34 already applies) - "other deductions" represents things like
-- uniform/canteen charges, scaled roughly with seniority.
--
-- Also demonstrates the exact scenario from the request: a designation
-- pays a fixed amount to everyone, but one specific employee (EMP0001, a
-- Supervisor) got a personal raise - only their BASIC SALARY is
-- overridden. Their PF% and other deductions still fall through to
-- whatever the Supervisor designation currently specifies, so a future
-- change to the designation's PF%/deductions still applies to them too.
-- ============================================================

UPDATE designations d
JOIN client_companies c ON c.id = d.client_company_id AND c.company_code = 'CLI0001'
JOIN (
    SELECT 'Housekeeping Staff' AS name, 12000 AS basic_salary, 200 AS other_deductions
    UNION ALL SELECT 'Security Guard', 14000, 200
    UNION ALL SELECT 'Technician', 18000, 300
    UNION ALL SELECT 'Electrician', 20000, 300
    UNION ALL SELECT 'Supervisor', 22000, 300
    UNION ALL SELECT 'Site Supervisor', 28000, 400
    UNION ALL SELECT 'Team Lead', 26000, 400
    UNION ALL SELECT 'Site Manager', 35000, 500
) pay ON pay.name = d.name
SET d.base_pay = pay.basic_salary,
    d.other_deductions = pay.other_deductions;

-- EMP0001 (Rohit Patil, Supervisor - designation basic salary 22000) gets a
-- personal raise to 25000. pf_percentage_override and
-- other_deductions_override stay NULL, so PF% and other deductions keep
-- inheriting from the Supervisor designation.
UPDATE employees e
JOIN client_companies c ON c.id = e.client_company_id AND c.company_code = 'CLI0001'
SET e.salary_override = 25000
WHERE e.employee_code = 'EMP0001';
