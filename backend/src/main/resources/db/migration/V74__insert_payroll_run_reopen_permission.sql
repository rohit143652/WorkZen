-- ============================================================
-- V74: Payroll Run reopen permission (architecture refactor Phase 7)
--
-- Separate from PAYROLL_RUN_APPROVE/CALCULATE - reopening an already-
-- approved payroll reverses a decision that was already made, so it's
-- gated by its own permission rather than reusing an existing one, per
-- "only authorized payroll administrators can reopen APPROVED payroll."
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('PAYROLL_RUN_REOPEN', 'Reopen an APPROVED payroll run back to CALCULATED for correction');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name = 'PAYROLL_RUN_REOPEN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name = 'PAYROLL_RUN_REOPEN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
