-- ============================================================
-- V25: Sample tenant-scoped business roles for CLI0001
--
-- These are separate rows from the global "ADMIN" system role - the new
-- tenant-scoped uniqueness rule (V23) allows a tenant to have its own role
-- literally named "ADMIN" without colliding with the house-only global one.
-- Client Admins can freely add more roles like this themselves via
-- Roles & Permissions (ROLE_CREATE, granted in V24), and adjust these
-- starter permission sets to taste (subject to the "can't grant what you
-- don't have" guardrail in RoleService).
--
-- Every grant below is scoped by client_company_id, not just role name, so
-- this remains correct once other tenants create their own same-named roles.
-- ============================================================

INSERT INTO roles (client_company_id, name, description)
SELECT c.id, 'ADMIN', 'Full administrative access within this company (tenant-scoped equivalent of Client Admin)'
FROM client_companies c WHERE c.company_code = 'CLI0001';

INSERT INTO roles (client_company_id, name, description)
SELECT c.id, 'HR_ADMIN', 'Manages employee records and login access for this company'
FROM client_companies c WHERE c.company_code = 'CLI0001';

INSERT INTO roles (client_company_id, name, description)
SELECT c.id, 'SITE_ADMIN', 'Manages sites and employee-to-site assignments for this company'
FROM client_companies c WHERE c.company_code = 'CLI0001';

INSERT INTO roles (client_company_id, name, description)
SELECT c.id, 'SITE_SUPERVISOR', 'Read-only operational visibility into employees, sites, and assignments'
FROM client_companies c WHERE c.company_code = 'CLI0001';

INSERT INTO roles (client_company_id, name, description)
SELECT c.id, 'ACCOUNTANT', 'Read-only visibility for finance/reporting purposes'
FROM client_companies c WHERE c.company_code = 'CLI0001';

-- ADMIN: mirrors the global CLIENT_ADMIN grant, scoped to this tenant
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN (
      'CLIENT_DASHBOARD_VIEW', 'CLIENT_PROFILE_READ', 'CLIENT_PROFILE_UPDATE',
      'EMPLOYEE_CREATE', 'EMPLOYEE_READ', 'EMPLOYEE_UPDATE', 'EMPLOYEE_DEACTIVATE', 'EMPLOYEE_ACTIVATE',
      'EMPLOYEE_ENABLE_LOGIN', 'EMPLOYEE_DISABLE_LOGIN', 'EMPLOYEE_RESET_PASSWORD', 'EMPLOYEE_ASSIGN_ROLE',
      'SITE_CREATE', 'SITE_READ', 'SITE_UPDATE', 'SITE_ACTIVATE', 'SITE_DEACTIVATE',
      'EMPLOYEE_ASSIGN', 'EMPLOYEE_TRANSFER', 'EMPLOYEE_ASSIGNMENT_READ',
      'USER_READ', 'USER_UPDATE', 'DASHBOARD_VIEW', 'PASSWORD_CHANGE',
      'DEPARTMENT_READ', 'DEPARTMENT_MANAGE', 'DESIGNATION_READ', 'DESIGNATION_MANAGE',
      'ROLE_READ', 'ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_DELETE', 'PERMISSION_READ'
  );

-- HR_ADMIN: people-management focused, no sites/assignments
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'HR_ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN (
      'EMPLOYEE_CREATE', 'EMPLOYEE_READ', 'EMPLOYEE_UPDATE', 'EMPLOYEE_ACTIVATE', 'EMPLOYEE_DEACTIVATE',
      'EMPLOYEE_ENABLE_LOGIN', 'EMPLOYEE_DISABLE_LOGIN', 'EMPLOYEE_RESET_PASSWORD', 'EMPLOYEE_ASSIGN_ROLE',
      'USER_READ', 'USER_UPDATE', 'DASHBOARD_VIEW', 'PASSWORD_CHANGE',
      'DEPARTMENT_READ', 'DEPARTMENT_MANAGE', 'DESIGNATION_READ', 'DESIGNATION_MANAGE'
  );

-- SITE_ADMIN: site/workforce-allocation focused
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SITE_ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN (
      'SITE_CREATE', 'SITE_READ', 'SITE_UPDATE', 'SITE_ACTIVATE', 'SITE_DEACTIVATE',
      'EMPLOYEE_READ', 'EMPLOYEE_ASSIGN', 'EMPLOYEE_TRANSFER', 'EMPLOYEE_ASSIGNMENT_READ',
      'DASHBOARD_VIEW', 'PASSWORD_CHANGE'
  );

-- SITE_SUPERVISOR: read-only operational visibility
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SITE_SUPERVISOR'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('EMPLOYEE_READ', 'EMPLOYEE_ASSIGNMENT_READ', 'SITE_READ', 'DASHBOARD_VIEW', 'PASSWORD_CHANGE');

-- ACCOUNTANT: minimal read-only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ACCOUNTANT'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('EMPLOYEE_READ', 'CLIENT_PROFILE_READ', 'DASHBOARD_VIEW', 'PASSWORD_CHANGE');
