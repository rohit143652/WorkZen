-- ============================================================
-- V93: ATTENDANCE_SELF_MARK should not be on administrator roles
--
-- CLIENT_ADMIN (the login a Super Admin creates for a new company) and its tenant-scoped
-- equivalent ADMIN manage the company - they don't personally check in at a site, so V92's
-- broad grant was too broad for these two specifically. Every OTHER role (HR_ADMIN, SITE_ADMIN,
-- SITE_SUPERVISOR, ACCOUNTANT, CLIENT_USER) keeps it, matching StarterRoleSeederService, which
-- now bakes this same distinction into every future new company too.
-- ============================================================

DELETE rp FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE p.name = 'ATTENDANCE_SELF_MARK'
  AND r.name IN ('CLIENT_ADMIN', 'ADMIN');
