-- ============================================================
-- V103: Accountant can VIEW which site an employee is assigned to, but not manage it
--
-- ACCOUNTANT was seeded (V25) with only EMPLOYEE_READ/CLIENT_PROFILE_READ/DASHBOARD_VIEW/
-- PASSWORD_CHANGE - it never had EMPLOYEE_ASSIGNMENT_READ at all. That meant the Employee
-- Details page's "Site Assignment" section silently failed to load for an Accountant (a 403 on
-- GET /api/employees/{id}/assignments swallowed the error and just left the section looking like
-- the employee had no site, which is wrong - not "not assigned", just "couldn't check").
--
-- EMPLOYEE_ASSIGN/EMPLOYEE_TRANSFER (which actually let someone change the assignment) are
-- deliberately NOT granted here - those stay admin-only. This is read-only visibility, matching
-- ACCOUNTANT's existing "minimal read-only" role description exactly.
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ACCOUNTANT'
  AND p.name = 'EMPLOYEE_ASSIGNMENT_READ'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
