-- ============================================================
-- V24: Grant CLIENT_ADMIN the ability to manage their own tenant's roles
--
-- This was a pre-existing gap: CLIENT_ADMIN could enable an employee's
-- login and pick a role, but lacked ROLE_READ, so GET /api/roles would
-- 403 for them. Now they get ROLE_READ/CREATE/UPDATE/DELETE, plus
-- PERMISSION_READ (needed to populate the permission checklist when
-- building a custom role) - but see RoleService for the two security
-- guardrails this depends on:
--   1. A non-SUPER_ADMIN can only create/update/delete roles that belong
--      to their OWN tenant (roles.client_company_id) - never global/house
--      roles like SUPER_ADMIN, ADMIN, MANAGER.
--   2. A non-SUPER_ADMIN can never grant a role a permission they do not
--      themselves currently hold - closes the privilege-escalation path
--      where a Client Admin could otherwise create a custom role with
--      SUPER_ADMIN-only permissions (which PERMISSION_READ lets them see
--      the names of, but never actually attach) and assign it to an
--      employee.
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name IN ('ROLE_READ', 'ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_DELETE', 'PERMISSION_READ')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
