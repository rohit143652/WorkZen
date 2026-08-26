-- ============================================================
-- V5: Role -> Permission mapping
--
-- SUPER_ADMIN receives every permission purely through this
-- database relationship - it is never hardcoded as "all access"
-- in Java. If a new permission is added later, it must also be
-- explicitly granted here (or via the Role Management API) for
-- SUPER_ADMIN, or any other role, to receive it.
-- ============================================================

-- SUPER_ADMIN: every permission that currently exists
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN';

-- ADMIN: user management + role/permission visibility + dashboard + audit
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN (
      'USER_CREATE', 'USER_READ', 'USER_UPDATE', 'USER_DELETE',
      'ROLE_READ', 'PERMISSION_READ',
      'DASHBOARD_VIEW', 'AUDIT_LOG_READ', 'PASSWORD_CHANGE'
  );

-- MANAGER: dashboard + client management + own password
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'MANAGER'
  AND p.name IN (
      'DASHBOARD_VIEW', 'CLIENT_READ', 'CLIENT_UPDATE', 'PASSWORD_CHANGE'
  );

-- USER: dashboard + own password only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'USER'
  AND p.name IN ('DASHBOARD_VIEW', 'PASSWORD_CHANGE');

-- CLIENT: client-scoped read access + own password
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT'
  AND p.name IN ('CLIENT_READ', 'PASSWORD_CHANGE');
