-- ============================================================
-- V68: Payroll Run permissions
--
-- CLIENT_ADMIN only, same convention as every other payroll-adjacent
-- permission in this system. Split into separate CREATE/CALCULATE/READ/
-- APPROVE/PAY/CANCEL permissions (rather than reusing the existing
-- PAYROLL_REGISTER_EXPORT) so a future role could be granted, e.g.,
-- read-only visibility without the ability to approve/pay - not used yet,
-- but this is the natural seam for that later.
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('PAYROLL_RUN_CREATE',   'Create a new payroll run (DRAFT) for a month'),
    ('PAYROLL_RUN_CALCULATE','Calculate/recalculate a payroll run''s employee results'),
    ('PAYROLL_RUN_READ',     'View persisted payroll runs and their employee results'),
    ('PAYROLL_RUN_APPROVE',  'Approve a calculated payroll run'),
    ('PAYROLL_RUN_PAY',      'Mark an approved payroll run as paid'),
    ('PAYROLL_RUN_CANCEL',   'Cancel a draft or calculated payroll run');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name IN ('PAYROLL_RUN_CREATE', 'PAYROLL_RUN_CALCULATE', 'PAYROLL_RUN_READ',
                 'PAYROLL_RUN_APPROVE', 'PAYROLL_RUN_PAY', 'PAYROLL_RUN_CANCEL')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name IN ('PAYROLL_RUN_CREATE', 'PAYROLL_RUN_CALCULATE', 'PAYROLL_RUN_READ',
                 'PAYROLL_RUN_APPROVE', 'PAYROLL_RUN_PAY', 'PAYROLL_RUN_CANCEL')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
