-- ============================================================
-- V9: Grant employee/dashboard permissions to roles
--
-- SUPER_ADMIN receives everything, purely via this database
-- relationship, matching the existing V5 pattern. ADMIN receives
-- a sensible operational subset; MANAGER/USER/CLIENT are left
-- untouched (no employee-management capability by default).
-- ============================================================

-- SUPER_ADMIN: every permission that currently exists (including new ones)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ADMIN: employee management + login-lifecycle management + dashboard analytics
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN (
      'EMPLOYEE_CREATE', 'EMPLOYEE_READ', 'EMPLOYEE_UPDATE',
      'EMPLOYEE_ACTIVATE', 'EMPLOYEE_DEACTIVATE',
      'EMPLOYEE_ENABLE_LOGIN', 'EMPLOYEE_DISABLE_LOGIN',
      'EMPLOYEE_RESET_PASSWORD', 'EMPLOYEE_ASSIGN_ROLE',
      'DASHBOARD_ANALYTICS'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
