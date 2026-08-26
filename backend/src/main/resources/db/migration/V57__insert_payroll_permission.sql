-- ============================================================
-- V57: Payroll Register permission
--
-- CLIENT_ADMIN only, same convention as MONTHLY_PAYMENT_REPORT_EXPORT -
-- this report exposes statutory deduction figures (EPF/ESI/PT) and net
-- pay for every employee at once, so it's restricted to the tenant's
-- admin, not any operational role. SUPER_ADMIN granted per the usual
-- "SUPER_ADMIN has every permission" invariant.
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('PAYROLL_REGISTER_EXPORT', 'View/download the full Payroll Register (EPF/ESI/PT, net pay) for all employees, and manage payroll settings');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name = 'PAYROLL_REGISTER_EXPORT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name = 'PAYROLL_REGISTER_EXPORT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
