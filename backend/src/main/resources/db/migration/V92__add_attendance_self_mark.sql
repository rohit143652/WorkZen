-- ============================================================
-- V92: Self-Service "Mark My Attendance" (one-click, GPS-checked)
--
-- Distinct from ATTENDANCE_CREATE (which lets a supervisor mark ANY employee's attendance from
-- the Mark Attendance table) - this is specifically for an employee marking their OWN
-- attendance with one click, from their own device. The endpoint behind this permission
-- (AttendanceController.markMine()) always resolves the employee from the CALLER's own login
-- (findByUserId()) and never accepts an employeeId at all, so even with this permission there
-- is no way to mark anyone else's attendance - same structural guarantee already used for
-- LEAVE_REQUEST_SELF_CREATE and PAYSLIP_SELF_VIEW.
--
-- Granted broadly here (every standard role from V90/V91, plus CLIENT_ADMIN/CLIENT_USER) since
-- marking your own attendance is safe for any role to be able to do - matches the same
-- "self-service is broadly fine" reasoning as LEAVE_REQUEST_SELF_CREATE, unlike PAYSLIP_SELF_VIEW
-- which was deliberately left ungranted by default.
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('ATTENDANCE_SELF_MARK', 'Mark one''s own attendance for today (self-service, one click, GPS-checked if the site has a geofence)');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE p.name = 'ATTENDANCE_SELF_MARK'
  AND (
       r.name IN ('CLIENT_ADMIN', 'CLIENT_USER')
    OR r.name IN ('ADMIN', 'HR_ADMIN', 'SITE_ADMIN', 'SITE_SUPERVISOR', 'ACCOUNTANT')
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
