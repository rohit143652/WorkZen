-- ============================================================
-- V65: Employee Advance permissions
--
-- CLIENT_ADMIN only, same convention as every other payroll-adjacent
-- permission in this system - advances directly affect deductions.
-- SUPER_ADMIN granted per the usual "gets every permission" invariant.
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('ADVANCE_READ',   'View an employee''s advances and recovery history'),
    ('ADVANCE_GRANT',  'Grant a new advance to an employee'),
    ('ADVANCE_UPDATE', 'Change an advance''s monthly recovery amount, or settle it manually');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name IN ('ADVANCE_READ', 'ADVANCE_GRANT', 'ADVANCE_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name IN ('ADVANCE_READ', 'ADVANCE_GRANT', 'ADVANCE_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
