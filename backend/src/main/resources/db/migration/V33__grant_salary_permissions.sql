-- ============================================================
-- V33: Grant salary permissions
--
-- SUPER_ADMIN: everything, via the usual catch-all. CLIENT_ADMIN (global)
-- and the tenant-scoped ADMIN/HR_ADMIN roles for CLI0001 get both
-- READ+UPDATE (HR is who'd plausibly set pay). ACCOUNTANT gets READ only
-- (their job is visibility into pay, not changing it). SITE_ADMIN and
-- SITE_SUPERVISOR deliberately get neither - they manage site operations
-- and attendance, not payroll.
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name IN ('EMPLOYEE_SALARY_READ', 'EMPLOYEE_SALARY_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('EMPLOYEE_SALARY_READ', 'EMPLOYEE_SALARY_UPDATE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'HR_ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('EMPLOYEE_SALARY_READ', 'EMPLOYEE_SALARY_UPDATE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ACCOUNTANT'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name = 'EMPLOYEE_SALARY_READ';
