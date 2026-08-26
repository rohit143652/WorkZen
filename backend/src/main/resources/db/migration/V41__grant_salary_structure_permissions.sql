-- ============================================================
-- V41: Grant Salary Structure permissions
--
-- Same distribution as the earlier salary permissions (V33): SUPER_ADMIN
-- everything; CLIENT_ADMIN (global) and tenant ADMIN/HR_ADMIN (CLI0001)
-- get full CRUD + assign; ACCOUNTANT gets read-only; SITE_ADMIN/
-- SITE_SUPERVISOR get neither - payroll setup is not a site-operations
-- concern.
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
  AND p.name IN ('SALARY_STRUCTURE_CREATE', 'SALARY_STRUCTURE_READ', 'SALARY_STRUCTURE_UPDATE', 'SALARY_STRUCTURE_DELETE', 'SALARY_ASSIGN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('SALARY_STRUCTURE_CREATE', 'SALARY_STRUCTURE_READ', 'SALARY_STRUCTURE_UPDATE', 'SALARY_STRUCTURE_DELETE', 'SALARY_ASSIGN');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'HR_ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('SALARY_STRUCTURE_CREATE', 'SALARY_STRUCTURE_READ', 'SALARY_STRUCTURE_UPDATE', 'SALARY_STRUCTURE_DELETE', 'SALARY_ASSIGN');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ACCOUNTANT'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name = 'SALARY_STRUCTURE_READ';
