-- ============================================================
-- V30: Dummy attendance history for CLI0001
--
-- Fills in the last 7 days (NOT including today, so there's still
-- something fresh to try marking on the "Mark Attendance" screen
-- immediately) for all 10 sample employees, using the site assignments
-- from V29. Dates are computed relative to CURDATE() at migration-run
-- time, not hardcoded, so this stays "the last week" no matter when you
-- actually run the migration. Mostly PRESENT with a handful of
-- ABSENT/HALF_DAY/ON_LEAVE sprinkled in for a realistic-looking history.
--
-- marked_by is the client_admin user seeded in V17, matching how this
-- data would look if a Site Admin/Supervisor had actually been marking it
-- day by day.
-- ============================================================

INSERT INTO attendance (client_company_id, employee_id, site_id, attendance_date, status, remarks, marked_by)
SELECT
    c.id,
    e.id,
    s.id,
    DATE_SUB(CURDATE(), INTERVAL d.day_offset DAY),
    CASE
        WHEN map.employee_code = 'EMP0002' AND d.day_offset = 3 THEN 'ABSENT'
        WHEN map.employee_code = 'EMP0004' AND d.day_offset = 5 THEN 'ON_LEAVE'
        WHEN map.employee_code = 'EMP0006' AND d.day_offset = 2 THEN 'HALF_DAY'
        WHEN map.employee_code = 'EMP0007' AND d.day_offset = 6 THEN 'ABSENT'
        WHEN map.employee_code = 'EMP0009' AND d.day_offset = 4 THEN 'ON_LEAVE'
        WHEN map.employee_code = 'EMP0001' AND d.day_offset = 1 THEN 'HALF_DAY'
        WHEN map.employee_code = 'EMP0010' AND d.day_offset = 7 THEN 'ABSENT'
        ELSE 'PRESENT'
    END,
    CASE
        WHEN map.employee_code = 'EMP0002' AND d.day_offset = 3 THEN 'Reported sick'
        WHEN map.employee_code = 'EMP0004' AND d.day_offset = 5 THEN 'Approved leave'
        WHEN map.employee_code = 'EMP0006' AND d.day_offset = 2 THEN 'Left early - personal work'
        WHEN map.employee_code = 'EMP0007' AND d.day_offset = 6 THEN 'No show, uninformed'
        WHEN map.employee_code = 'EMP0009' AND d.day_offset = 4 THEN 'Approved leave'
        WHEN map.employee_code = 'EMP0001' AND d.day_offset = 1 THEN 'Half day - medical appointment'
        WHEN map.employee_code = 'EMP0010' AND d.day_offset = 7 THEN 'Reported sick'
        ELSE NULL
    END,
    (SELECT id FROM users WHERE username = 'client_admin')
FROM client_companies c
JOIN employees e ON e.client_company_id = c.id
JOIN (
    SELECT 'EMP0001' AS employee_code, 'SITE0001' AS site_code
    UNION ALL SELECT 'EMP0002', 'SITE0001'
    UNION ALL SELECT 'EMP0003', 'SITE0002'
    UNION ALL SELECT 'EMP0004', 'SITE0002'
    UNION ALL SELECT 'EMP0005', 'SITE0003'
    UNION ALL SELECT 'EMP0006', 'SITE0003'
    UNION ALL SELECT 'EMP0007', 'SITE0004'
    UNION ALL SELECT 'EMP0008', 'SITE0004'
    UNION ALL SELECT 'EMP0009', 'SITE0005'
    UNION ALL SELECT 'EMP0010', 'SITE0005'
) map ON map.employee_code = e.employee_code
JOIN sites s ON s.client_company_id = c.id AND s.site_code = map.site_code
JOIN (
    SELECT 1 AS day_offset UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
) d ON 1 = 1
WHERE c.company_code = 'CLI0001';
