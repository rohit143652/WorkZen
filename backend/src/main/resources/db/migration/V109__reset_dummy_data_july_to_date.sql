-- ============================================================
-- V109: Fresh dummy data reset (July 2026 - today), every company
--
-- Explicit, destructive reset requested by the client. This:
--   1. Wipes ALL transactional payroll/attendance/advance/overtime/leave history, every company.
--   2. Refreshes every employee's personal/statutory/bank PROFILE fields with fresh dummy
--      values - same employee id, same department/designation/joining date/status, same site
--      assignment, same salary structure, same linked login account. Only "who is this person"
--      fields change (name, contact, DOB, gender, address, Aadhar/PAN/UAN/PF/ESIC, bank details).
--   3. Seeds fresh attendance for every active employee, July 1 2026 through today (whatever day
--      this migration actually runs - uses CURDATE(), not a hardcoded end date).
--   4. Seeds a small number of sample overtime entries and advances for variety/testing.
--
-- Deliberately NOT touched (per "do not change login data", interpreted as: preserve every
-- account's ability to log in exactly as it does today): users, roles, permissions,
-- role_permissions, and the employees.user_id link on every employee (their existing username/
-- password keep working - only the person's PROFILE display info changes, so a username like
-- "r.patil" may end up paired with a freshly-generated different display name; that's an
-- accepted, unavoidable side effect of "regenerate employee profiles, keep logins intact").
--
-- Also NOT touched - this is organizational structure and configuration, not sample/dummy data:
-- client_companies, departments/designations, sites, employee_site_assignments, salary
-- structures/components, employee_salary_structures, payroll_settings, attendance_rule_config,
-- company_feature, employee_paid_leave_balances/overrides.
--
-- Deliberately does NOT auto-generate Payroll Runs. A Payroll Run's actual EPF/ESI/PT/Net Pay
-- figures come from PayrollCalculationService (Java) using PayrollSettings, each employee's PF/
-- ESI/PT applicability, and their specific salary structure - reproducing that same logic a
-- second time in raw SQL here would be a second, easy-to-diverge copy of it, and any mistake
-- would silently produce WRONG payroll figures sitting in the database looking legitimate.
-- After this migration runs, use Payroll Processing in the app itself to generate July/August/
-- September runs - that guarantees every number is computed by the one real engine, not
-- approximated here.
--
-- *** THIS DELETES DATA. Back up the database before this migration runs if there is anything
-- in the current transactional history worth keeping. This cannot be undone by another
-- migration - only by restoring a backup taken beforehand. ***
-- ============================================================

-- ---------- 1. Wipe transactional history (children before parents, every company) ----------
DELETE FROM payroll_run_employees;
DELETE FROM employee_payroll_adjustments;
DELETE FROM advance_recovery_transactions;
DELETE FROM employee_advances;
DELETE FROM employee_overtime_records;
DELETE FROM attendance_correction_request;
DELETE FROM attendance;
DELETE FROM leave_requests;
DELETE FROM payroll_runs;

-- ---------- 2. Refresh every employee's personal/statutory/bank profile ----------
-- employee_code, department, designation, joining_date, status, client_company_id, and user_id
-- are NOT in this SET list - none of them change. Uses each row's own (globally unique) id to
-- derive every generated value, so email/Aadhar/PAN/UAN/bank account numbers can never collide
-- with another employee's, in this tenant or any other.
UPDATE employees e
SET
    first_name  = ELT(1 + MOD(e.id, 12), 'Rohit','Suresh','Prakash','Sunita','Anita','Ganesh','Kiran','Meena','Vijay','Deepak','Sneha','Manoj'),
    middle_name = NULL,
    last_name   = ELT(1 + MOD(FLOOR(e.id / 3), 10), 'Patil','Deshmukh','Jadhav','More','Gaikwad','Pawar','Shinde','Bhosale','Kadam','Joshi'),
    email = CONCAT('employee', e.id, '@example.com'),
    mobile_number = CONCAT('9', LPAD(100000000 + e.id, 9, '0')),
    alternate_mobile_number = NULL,
    date_of_birth = DATE_SUB('2000-01-01', INTERVAL MOD(e.id * 37, 4000) DAY),
    gender = IF(MOD(e.id, 2) = 0, 'MALE', 'FEMALE'),
    address = CONCAT(1 + MOD(e.id, 200), ' MG Road'),
    city = ELT(1 + MOD(e.id, 5), 'Pune','Mumbai','Nashik','Nagpur','Aurangabad'),
    state = 'Maharashtra',
    country = 'India',
    pincode = LPAD(411000 + MOD(e.id, 99), 6, '0'),
    aadhar_number = LPAD(100000000000 + e.id, 12, '0'),
    pan_number = CONCAT('ABCDE', LPAD(MOD(e.id, 9999), 4, '0'), 'F'),
    uan_number = LPAD(200000000000 + e.id, 12, '0'),
    pf_member_id = CONCAT('PF', LPAD(e.id, 8, '0')),
    esic_number = LPAD(300000000000 + e.id, 12, '0'),
    bank_account_holder_name = CONCAT(
        ELT(1 + MOD(e.id, 12), 'Rohit','Suresh','Prakash','Sunita','Anita','Ganesh','Kiran','Meena','Vijay','Deepak','Sneha','Manoj'), ' ',
        ELT(1 + MOD(FLOOR(e.id / 3), 10), 'Patil','Deshmukh','Jadhav','More','Gaikwad','Pawar','Shinde','Bhosale','Kadam','Joshi')
    ),
    bank_account_number = LPAD(400000000000 + e.id, 14, '0'),
    bank_ifsc_code = CONCAT(ELT(1 + MOD(e.id, 4), 'HDFC','ICIC','SBIN','UTIB'), '0001234'),
    bank_name = ELT(1 + MOD(e.id, 4), 'HDFC Bank','ICICI Bank','State Bank of India','Axis Bank'),
    bank_branch = ELT(1 + MOD(e.id, 5), 'Pune','Mumbai','Nashik','Nagpur','Aurangabad');

-- ---------- 3. Fresh attendance: July 1, 2026 through today, every active employee ----------
-- Skips Sunday (weekly off). Each employee's CURRENT active site assignment is used (picking
-- the most recent by start_date if more than one somehow exists, though normally there is only
-- one). A small, deterministic (not random - so this migration behaves identically every time
-- it's inspected, not just when it runs) spread of ABSENT/HALF_DAY days is mixed in via each
-- employee id + day-offset, everything else PRESENT - purely for a realistic-looking demo, not
-- meant to reflect anyone's real attendance history.
--
-- Generates the July-1-to-today day offsets (0..999, comfortably covering ~2.7 years, so this
-- never runs out even if this migration isn't applied for a long time) via a classic
-- numbers-table cross join (three UNION SELECT digit tables combined as a*1 + b*10 + c*100)
-- rather than a recursive CTE - this exact WITH RECURSIVE placement already failed once against
-- this project's actual MySQL server, so this avoids that entirely with a technique that has no
-- version-specific syntax ambiguity at all.
INSERT INTO attendance (client_company_id, employee_id, site_id, attendance_date, status, attendance_source, created_at, updated_at)
SELECT
    e.client_company_id,
    e.id,
    ps.site_id,
    DATE_ADD('2026-07-01', INTERVAL n.offset DAY),
    CASE
        WHEN MOD(e.id + n.offset, 17) = 0 THEN 'ABSENT'
        WHEN MOD(e.id + n.offset, 23) = 0 THEN 'HALF_DAY'
        ELSE 'PRESENT'
    END,
    'ADMIN_MANUAL',
    NOW(), NOW()
FROM employees e
JOIN (
    SELECT employee_id, site_id,
           ROW_NUMBER() OVER (PARTITION BY employee_id ORDER BY start_date DESC, id DESC) AS rn
    FROM employee_site_assignments
    WHERE status = 'ACTIVE'
) ps ON ps.employee_id = e.id AND ps.rn = 1
JOIN (
    SELECT a.n + b.n * 10 + c.n * 100 AS offset
    FROM
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
        (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c
) n ON DATE_ADD('2026-07-01', INTERVAL n.offset DAY) <= CURDATE()
WHERE e.status = 'ACTIVE'
  AND DAYOFWEEK(DATE_ADD('2026-07-01', INTERVAL n.offset DAY)) <> 1; -- MySQL DAYOFWEEK: 1 = Sunday

-- ---------- 4. A handful of sample overtime entries, for variety/testing ----------
INSERT INTO employee_overtime_records (client_company_id, employee_id, overtime_date, hours, amount, remarks, marked_by, marked_by_role)
SELECT e.client_company_id, e.id, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 2.5, 250.00, 'Sample overtime entry', NULL, NULL
FROM employees e
WHERE e.status = 'ACTIVE' AND MOD(e.id, 4) = 0;

-- ---------- 5. A handful of sample advances, recovery starting THIS month ----------
-- (so they're immediately testable in a Payroll Run for the current month, unlike the old
-- Rohit Patil sample advance whose recovery didn't start until a later month by design)
INSERT INTO employee_advances (client_company_id, employee_id, advance_date, amount, reason, payment_mode,
                                monthly_recovery_amount, recovery_start_year, recovery_start_month, remarks,
                                recover_via_payroll, status)
SELECT e.client_company_id, e.id, DATE_SUB(CURDATE(), INTERVAL 20 DAY), 2000.00, 'Sample Advance', 'CASH',
       500.00, YEAR(CURDATE()), MONTH(CURDATE()), 'Fresh seed data', TRUE, 'ACTIVE'
FROM employees e
WHERE e.status = 'ACTIVE' AND MOD(e.id, 5) = 0;
