-- ============================================================
-- V28: Grant attendance permissions
--
-- SUPER_ADMIN: everything, via the same catch-all pattern used everywhere
-- else. CLIENT_ADMIN (global house role): full CREATE/READ/UPDATE, since a
-- Client Admin is who's meant to make corrections. For CLI0001's
-- tenant-scoped example roles (V25): SITE_ADMIN and SITE_SUPERVISOR get
-- CREATE+READ only (per spec: they can mark attendance and view history,
-- but cannot change an already-marked day); the tenant-scoped "ADMIN" role
-- mirrors CLIENT_ADMIN and also gets UPDATE.
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
  AND p.name IN ('ATTENDANCE_CREATE', 'ATTENDANCE_READ', 'ATTENDANCE_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Tenant-scoped ADMIN for CLI0001 (mirrors CLIENT_ADMIN)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('ATTENDANCE_CREATE', 'ATTENDANCE_READ', 'ATTENDANCE_UPDATE');

-- SITE_ADMIN for CLI0001: can mark and view, cannot change once marked
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SITE_ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('ATTENDANCE_CREATE', 'ATTENDANCE_READ');

-- SITE_SUPERVISOR for CLI0001: can mark and view, cannot change once marked
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SITE_SUPERVISOR'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('ATTENDANCE_CREATE', 'ATTENDANCE_READ');
