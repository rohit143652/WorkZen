-- ============================================================
-- V99: Permissions for check-in/check-out, correction requests, and rule config
--
-- ATTENDANCE_SELF_MARK already covers "mark my own attendance" broadly (V92) - check-in/
-- check-out reuses that SAME permission rather than a new one, since it's the exact same
-- "self-service, own record only" shape, just with times instead of a one-click status. Only
-- three genuinely new permissions are needed:
--
--   ATTENDANCE_CORRECTION_REQUEST - employee requests a fix to their own record. Granted
--   broadly, same reasoning as ATTENDANCE_SELF_MARK/LEAVE_REQUEST_SELF_CREATE: asking for a
--   correction is always safe, it does nothing on its own until approved.
--
--   ATTENDANCE_CORRECTION_REVIEW - approve/reject a correction request. Granted exactly
--   alongside ATTENDANCE_UPDATE (V28) - the same people who can already directly edit a marked
--   day are the right people to review a correction request for one.
--
--   ATTENDANCE_RULES_MANAGE - configure office hours/grace period/thresholds for the tenant.
--   SUPER_ADMIN + CLIENT_ADMIN only (company-wide policy, not a day-to-day HR/site action).
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('ATTENDANCE_CORRECTION_REQUEST', 'Request a correction to one''s own attendance record for a given date'),
    ('ATTENDANCE_CORRECTION_REVIEW', 'Approve or reject an employee''s attendance correction request'),
    ('ATTENDANCE_RULES_MANAGE', 'Configure company-wide attendance rules (office hours, grace period, half/full-day thresholds, weekly off)');

-- SUPER_ADMIN: everything, same catch-all pattern used everywhere else.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ATTENDANCE_CORRECTION_REQUEST: same broad grant as ATTENDANCE_SELF_MARK (V92).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE p.name = 'ATTENDANCE_CORRECTION_REQUEST'
  AND (
       (r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL)
    OR r.name = 'CLIENT_USER'
    OR r.name IN ('ADMIN', 'HR_ADMIN', 'SITE_ADMIN', 'SITE_SUPERVISOR', 'ACCOUNTANT')
  )
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ATTENDANCE_CORRECTION_REVIEW: wherever ATTENDANCE_UPDATE is already held.
INSERT INTO role_permissions (role_id, permission_id)
SELECT rp.role_id, p2.id
FROM role_permissions rp
JOIN permissions p1 ON p1.id = rp.permission_id AND p1.name = 'ATTENDANCE_UPDATE'
JOIN permissions p2 ON p2.name = 'ATTENDANCE_CORRECTION_REVIEW'
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions rp2 WHERE rp2.role_id = rp.role_id AND rp2.permission_id = p2.id
);

-- ATTENDANCE_RULES_MANAGE: CLIENT_ADMIN (global house role) + tenant-scoped ADMIN for CLI0001,
-- mirroring exactly how ATTENDANCE_UPDATE itself was granted in V28.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name = 'ATTENDANCE_RULES_MANAGE'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name = 'ATTENDANCE_RULES_MANAGE'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
