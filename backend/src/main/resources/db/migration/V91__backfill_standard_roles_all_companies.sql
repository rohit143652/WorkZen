-- ============================================================
-- V91: Backfill the standard starter roles for EVERY EXISTING client company
--
-- StarterRoleSeederService now creates ADMIN/HR_ADMIN/SITE_ADMIN/SITE_SUPERVISOR/ACCOUNTANT
-- automatically for every NEW client company from now on - this migration does the same thing
-- for every company that already existed before that code went in, so nobody has to be a
-- "second-class" tenant that's missing the standard role set just because of when they signed
-- up. Every INSERT below is guarded with NOT EXISTS, so this is safe to run against a company
-- that already has some/all of these roles (e.g. the demo tenant, which has had them since
-- V25) - it only ever adds what's genuinely missing, never duplicates or removes anything.
-- ============================================================

-- ---- 1. Create any missing role rows, one per existing company ----

INSERT INTO roles (client_company_id, name, description)
SELECT c.id, 'ADMIN', 'Full administrative access within this company (tenant-scoped equivalent of Client Admin)'
FROM client_companies c
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.client_company_id = c.id AND r.name = 'ADMIN');

INSERT INTO roles (client_company_id, name, description)
SELECT c.id, 'HR_ADMIN', 'Manages employee records, login access, leave, and exits for this company'
FROM client_companies c
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.client_company_id = c.id AND r.name = 'HR_ADMIN');

INSERT INTO roles (client_company_id, name, description)
SELECT c.id, 'SITE_ADMIN', 'Manages sites and employee-to-site assignments for this company'
FROM client_companies c
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.client_company_id = c.id AND r.name = 'SITE_ADMIN');

INSERT INTO roles (client_company_id, name, description)
SELECT c.id, 'SITE_SUPERVISOR', 'Day-to-day site operations - attendance, leave, and read-only visibility into employees/sites/assignments'
FROM client_companies c
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.client_company_id = c.id AND r.name = 'SITE_SUPERVISOR');

INSERT INTO roles (client_company_id, name, description)
SELECT c.id, 'ACCOUNTANT', 'Read-only visibility for finance/reporting purposes'
FROM client_companies c
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.client_company_id = c.id AND r.name = 'ACCOUNTANT');

-- ---- 2. Grant each role's default permissions (only what's missing, per role instance) ----

-- ADMIN mirrors whatever the system CLIENT_ADMIN role currently has - same reasoning as
-- StarterRoleSeederService.copyClientAdminPermissionNames(), just expressed in SQL: copy the
-- permission set from the one global CLIENT_ADMIN role onto every company's own ADMIN role.
INSERT INTO role_permissions (role_id, permission_id)
SELECT admin_role.id, ca_perm.permission_id
FROM roles admin_role
JOIN (
    SELECT rp.permission_id
    FROM role_permissions rp
    JOIN roles ca ON ca.id = rp.role_id
    WHERE ca.name = 'CLIENT_ADMIN' AND ca.client_company_id IS NULL
) ca_perm
WHERE admin_role.name = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp2
    WHERE rp2.role_id = admin_role.id AND rp2.permission_id = ca_perm.permission_id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'HR_ADMIN'
  AND p.name IN (
    'DASHBOARD_VIEW', 'PASSWORD_CHANGE',
    'EMPLOYEE_READ', 'EMPLOYEE_CREATE', 'EMPLOYEE_UPDATE', 'EMPLOYEE_ACTIVATE', 'EMPLOYEE_DEACTIVATE',
    'EMPLOYEE_ENABLE_LOGIN', 'EMPLOYEE_DISABLE_LOGIN', 'EMPLOYEE_RESET_PASSWORD', 'EMPLOYEE_ASSIGN_ROLE',
    'DEPARTMENT_READ', 'DEPARTMENT_MANAGE', 'DESIGNATION_READ', 'DESIGNATION_MANAGE',
    'SITE_READ', 'EMPLOYEE_ASSIGNMENT_READ',
    'ATTENDANCE_READ', 'HOLIDAY_READ',
    'LEAVE_REQUEST_READ', 'LEAVE_REQUEST_MANAGE', 'LEAVE_REQUEST_SELF_CREATE',
    'EMPLOYEE_EXIT_READ', 'EMPLOYEE_EXIT_CREATE', 'EMPLOYEE_EXIT_SETTLE'
  )
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SITE_ADMIN'
  AND p.name IN (
    'DASHBOARD_VIEW', 'PASSWORD_CHANGE',
    'SITE_CREATE', 'SITE_READ', 'SITE_UPDATE', 'SITE_ACTIVATE', 'SITE_DEACTIVATE',
    'EMPLOYEE_READ', 'EMPLOYEE_ASSIGN', 'EMPLOYEE_TRANSFER', 'EMPLOYEE_ASSIGNMENT_READ',
    'ATTENDANCE_CREATE', 'ATTENDANCE_READ', 'LEAVE_REQUEST_SELF_CREATE'
  )
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SITE_SUPERVISOR'
  AND p.name IN (
    'DASHBOARD_VIEW', 'PASSWORD_CHANGE',
    'EMPLOYEE_READ', 'EMPLOYEE_ASSIGNMENT_READ', 'SITE_READ',
    'ATTENDANCE_CREATE', 'ATTENDANCE_READ', 'HOLIDAY_READ',
    'LEAVE_REQUEST_READ', 'LEAVE_REQUEST_MANAGE', 'LEAVE_REQUEST_SELF_CREATE'
  )
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ACCOUNTANT'
  AND p.name IN (
    'DASHBOARD_VIEW', 'PASSWORD_CHANGE',
    'PAYROLL_RUN_READ', 'PAYROLL_REGISTER_EXPORT', 'SALARY_STRUCTURE_READ',
    'ADVANCE_READ', 'MONTHLY_PAYMENT_REPORT_EXPORT', 'LEAVE_REQUEST_SELF_CREATE'
  )
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
