-- ============================================================
-- V29: Dummy employee-site assignments for CLI0001
--
-- Attendance can only be marked for an actively-assigned employee (see
-- AttendanceService.mark()), and the 10 sample employees from V17 had no
-- assignments at all - so the "Mark Attendance" screen would show nothing
-- to try it against. This assigns 2 employees to each of the 5 sample
-- sites, starting well in the past, so both the Employee Assignment board
-- and the Attendance module have real data to demo immediately.
-- ============================================================

INSERT INTO employee_site_assignments (client_company_id, employee_id, site_id, assignment_type, start_date, is_primary, status, remarks)
SELECT c.id, e.id, s.id, 'REGULAR', map.start_date, TRUE, 'ACTIVE', 'Seed data for local development'
FROM client_companies c
JOIN employees e ON e.client_company_id = c.id
JOIN sites s ON s.client_company_id = c.id
JOIN (
    SELECT 'EMP0001' AS employee_code, 'SITE0001' AS site_code, '2025-05-01' AS start_date
    UNION ALL SELECT 'EMP0002', 'SITE0001', '2025-05-01'
    UNION ALL SELECT 'EMP0003', 'SITE0002', '2025-05-01'
    UNION ALL SELECT 'EMP0004', 'SITE0002', '2025-05-01'
    UNION ALL SELECT 'EMP0005', 'SITE0003', '2025-05-01'
    UNION ALL SELECT 'EMP0006', 'SITE0003', '2025-05-01'
    UNION ALL SELECT 'EMP0007', 'SITE0004', '2025-05-01'
    UNION ALL SELECT 'EMP0008', 'SITE0004', '2025-05-01'
    UNION ALL SELECT 'EMP0009', 'SITE0005', '2025-05-01'
    UNION ALL SELECT 'EMP0010', 'SITE0005', '2025-05-01'
) map ON map.employee_code = e.employee_code AND map.site_code = s.site_code
WHERE c.company_code = 'CLI0001';
