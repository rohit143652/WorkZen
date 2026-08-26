-- ============================================================
-- V49: One full month of varied dummy attendance for CLI0001
--
-- V30 only seeds the last 7 days (for trying "Mark Attendance" itself).
-- This fills in the ENTIRE previous calendar month (relative to
-- CURDATE() at migration-run time, so it's always "last month" whenever
-- you actually run this) for the same 10 sample employees, so there is
-- immediately a full month of realistic-looking data to open in the
-- Monthly Attendance & Payment Report.
--
-- Status per employee+day is picked with a deterministic-but-varied
-- formula (based on employee id and day-of-month, not a real random()) so
-- re-running this migration always produces the same result: roughly
-- ~78% PRESENT, ~8% HALF_DAY, ~8% ON_LEAVE, ~6% ABSENT. Every employee
-- gets a slightly different mix since the formula is seeded per employee.
--
-- Uses a recursive CTE to generate the day sequence 1..last_day_of_month -
-- MySQL 8's WITH RECURSIVE, same engine this project already assumes.
-- ============================================================

INSERT INTO attendance (client_company_id, employee_id, site_id, attendance_date, status, remarks, marked_by)
SELECT
    c.id,
    e.id,
    s.id,
    d.the_date,
    CASE (MOD(e.id * 31 + DAY(d.the_date) * 7, 20))
        WHEN 0 THEN 'ABSENT'
        WHEN 1 THEN 'ON_LEAVE'
        WHEN 2 THEN 'ON_LEAVE'
        WHEN 3 THEN 'HALF_DAY'
        ELSE 'PRESENT'
    END AS status,
    CASE (MOD(e.id * 31 + DAY(d.the_date) * 7, 20))
        WHEN 0 THEN 'No show, uninformed'
        WHEN 1 THEN 'Approved leave'
        WHEN 2 THEN 'Approved leave'
        WHEN 3 THEN 'Left early - personal work'
        ELSE NULL
    END AS remarks,
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
    WITH RECURSIVE month_bounds AS (
        SELECT DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH) AS month_start
    ),
    days AS (
        SELECT month_start AS the_date FROM month_bounds
        UNION ALL
        SELECT DATE_ADD(the_date, INTERVAL 1 DAY)
        FROM days, month_bounds
        WHERE DATE_ADD(the_date, INTERVAL 1 DAY) < DATE_ADD(month_bounds.month_start, INTERVAL 1 MONTH)
    )
    SELECT the_date FROM days
) d ON 1 = 1
WHERE c.company_code = 'CLI0001'
  AND NOT EXISTS (
      SELECT 1 FROM attendance a
      WHERE a.employee_id = e.id AND a.attendance_date = d.the_date
  );
