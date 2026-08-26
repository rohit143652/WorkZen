-- ============================================================
-- V59: Second month of varied dummy attendance for CLI0001
--
-- V49 seeded the previous calendar month (month - 1). This adds the
-- month before that (month - 2), so there are now 2 full months of
-- data for all 10 sample employees - useful for trying the Monthly
-- Attendance & Payment Report's carry-forward-across-months behaviour
-- (paid leave balance, etc.) instead of just a single isolated month.
--
-- Same deterministic formula as V49, just shifted one month further
-- back and with a different multiplier so the two months don't look
-- identical: roughly ~76% PRESENT, ~9% HALF_DAY, ~9% ON_LEAVE, ~6% ABSENT.
-- ============================================================

INSERT INTO attendance (client_company_id, employee_id, site_id, attendance_date, status, remarks, marked_by)
SELECT
    c.id,
    e.id,
    s.id,
    d.the_date,
    CASE (MOD(e.id * 17 + DAY(d.the_date) * 11, 20))
        WHEN 0 THEN 'ABSENT'
        WHEN 1 THEN 'ON_LEAVE'
        WHEN 2 THEN 'ON_LEAVE'
        WHEN 3 THEN 'HALF_DAY'
        WHEN 4 THEN 'HALF_DAY'
        ELSE 'PRESENT'
    END AS status,
    CASE (MOD(e.id * 17 + DAY(d.the_date) * 11, 20))
        WHEN 0 THEN 'No show, uninformed'
        WHEN 1 THEN 'Approved leave'
        WHEN 2 THEN 'Approved leave'
        WHEN 3 THEN 'Left early - personal work'
        WHEN 4 THEN 'Reported late'
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
        SELECT DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 2 MONTH) AS month_start
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
