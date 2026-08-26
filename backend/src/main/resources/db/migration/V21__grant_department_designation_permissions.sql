-- ============================================================
-- V21: Grant Department & Designation permissions
--
-- SUPER_ADMIN: everything, via the same "all current permissions" pattern
-- used everywhere else. CLIENT_ADMIN: both READ and MANAGE for each, so a
-- Client Admin can add new departments/designations (e.g. "Site Manager",
-- "Site Supervisor") without SUPER_ADMIN involvement, exactly as employees
-- and sites already work.
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN'
  AND p.name IN ('DEPARTMENT_READ', 'DEPARTMENT_MANAGE', 'DESIGNATION_READ', 'DESIGNATION_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
