-- ============================================================
-- V45: Monthly Attendance & Payment report export permission
--
-- Bulk (all-employees-at-once) download is restricted to CLIENT_ADMIN, per
-- the requirement that only the tenant's admin - not SITE_ADMIN,
-- SITE_SUPERVISOR, or any other operational role - can pull this report.
-- SUPER_ADMIN is granted it too, purely to keep the established "SUPER_ADMIN
-- has every permission" invariant used throughout every prior migration;
-- the report is tenant-scoped by construction (TenantContextService.
-- requireCurrentTenantId() throws for a house-only SUPER_ADMIN session), so
-- this is consistent with how EMPLOYEE_CREATE etc. are already granted.
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('MONTHLY_PAYMENT_REPORT_EXPORT', 'Download the monthly attendance & payment Excel report for all employees in the tenant');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name = 'MONTHLY_PAYMENT_REPORT_EXPORT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN'
  AND p.name = 'MONTHLY_PAYMENT_REPORT_EXPORT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
