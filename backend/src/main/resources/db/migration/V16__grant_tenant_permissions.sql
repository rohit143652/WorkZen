-- ============================================================
-- V16: Grant tenant-related permissions
--
-- SUPER_ADMIN: everything, via the same "all current permissions"
-- pattern as V5/V9 - purely database-driven, no Java conditionals.
-- CLIENT_ADMIN: the operational subset for managing a single tenant
-- (spec section 27). CLIENT_ADMIN deliberately does NOT receive
-- CLIENT_COMPANY_CREATE/ACTIVATE/DEACTIVATE - only SUPER_ADMIN
-- manages the tenant records themselves.
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
  AND p.name IN (
      'CLIENT_DASHBOARD_VIEW', 'CLIENT_PROFILE_READ', 'CLIENT_PROFILE_UPDATE',
      'EMPLOYEE_CREATE', 'EMPLOYEE_READ', 'EMPLOYEE_UPDATE', 'EMPLOYEE_DEACTIVATE', 'EMPLOYEE_ACTIVATE',
      'EMPLOYEE_ENABLE_LOGIN', 'EMPLOYEE_DISABLE_LOGIN', 'EMPLOYEE_RESET_PASSWORD', 'EMPLOYEE_ASSIGN_ROLE',
      'SITE_CREATE', 'SITE_READ', 'SITE_UPDATE', 'SITE_ACTIVATE', 'SITE_DEACTIVATE',
      'SUBCLIENT_CREATE', 'SUBCLIENT_READ', 'SUBCLIENT_UPDATE', 'SUBCLIENT_ACTIVATE', 'SUBCLIENT_DEACTIVATE',
      'EMPLOYEE_ASSIGN', 'EMPLOYEE_TRANSFER', 'EMPLOYEE_ASSIGNMENT_READ',
      'USER_READ', 'USER_UPDATE', 'DASHBOARD_VIEW', 'PASSWORD_CHANGE'
  );
