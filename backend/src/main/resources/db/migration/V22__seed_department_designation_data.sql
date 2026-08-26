-- ============================================================
-- V22: Seed Department/Designation master data for CLI0001
--
-- Matches the free-text values already used by the 10 sample employees
-- from V17 (so those records remain editable once EmployeeService starts
-- validating department/designation against these master lists), plus
-- "Site Manager" and "Site Supervisor" as extra example designations
-- for a housekeeping/facility-management business.
-- ============================================================

INSERT INTO departments (client_company_id, name, status)
SELECT c.id, d.name, 'ACTIVE'
FROM client_companies c
JOIN (
    SELECT 'Operations' AS name
    UNION ALL SELECT 'Security'
    UNION ALL SELECT 'Maintenance'
    UNION ALL SELECT 'Housekeeping'
) d ON 1 = 1
WHERE c.company_code = 'CLI0001';

INSERT INTO designations (client_company_id, name, status)
SELECT c.id, d.name, 'ACTIVE'
FROM client_companies c
JOIN (
    SELECT 'Supervisor' AS name
    UNION ALL SELECT 'Housekeeping Staff'
    UNION ALL SELECT 'Security Guard'
    UNION ALL SELECT 'Technician'
    UNION ALL SELECT 'Team Lead'
    UNION ALL SELECT 'Electrician'
    UNION ALL SELECT 'Site Manager'
    UNION ALL SELECT 'Site Supervisor'
) d ON 1 = 1
WHERE c.company_code = 'CLI0001';
