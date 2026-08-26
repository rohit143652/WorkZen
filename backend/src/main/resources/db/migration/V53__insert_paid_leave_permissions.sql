-- ============================================================
-- V53: Paid Leave Management permissions
--
-- SUPER_ADMIN: everything, via the same "all current permissions" pattern
-- as every prior permission migration. CLIENT_ADMIN: full control within
-- their own tenant (spec section 10/12) - grant/update/cancel extra leave,
-- view any employee's balance/history, and manage the tenant's
-- configuration. Ordinary employees need none of these to view their OWN
-- leave - that's handled by self-access in EmployeePaidLeaveService, not
-- by a permission grant (spec section 10: an employee must never be able
-- to grant leave, modify balances, modify configuration, or view another
-- employee's leave - giving them a permission row here would risk exactly
-- that if a future page ever forgets to add the self-check).
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('PAID_LEAVE_READ',          'View any employee''s Paid Leave balance and monthly history'),
    ('PAID_LEAVE_GRANT',         'Grant additional (extra) Paid Leave to an employee'),
    ('PAID_LEAVE_UPDATE',        'Update or cancel an extra Paid Leave grant, or manually correct a month''s used leave'),
    ('PAID_LEAVE_HISTORY_READ',  'View an employee''s extra Paid Leave grant history'),
    ('PAID_LEAVE_CONFIG_READ',   'View the tenant''s Paid Leave configuration'),
    ('PAID_LEAVE_CONFIG_UPDATE', 'Change the tenant''s Paid Leave configuration (monthly allocation, carry-forward rules)');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name IN ('PAID_LEAVE_READ', 'PAID_LEAVE_GRANT', 'PAID_LEAVE_UPDATE', 'PAID_LEAVE_HISTORY_READ',
                 'PAID_LEAVE_CONFIG_READ', 'PAID_LEAVE_CONFIG_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name IN ('PAID_LEAVE_READ', 'PAID_LEAVE_GRANT', 'PAID_LEAVE_UPDATE', 'PAID_LEAVE_HISTORY_READ',
                 'PAID_LEAVE_CONFIG_READ', 'PAID_LEAVE_CONFIG_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
