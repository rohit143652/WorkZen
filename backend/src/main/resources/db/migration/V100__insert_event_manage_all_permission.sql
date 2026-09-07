-- ============================================================
-- V100: A dedicated permission for "see every event on the calendar, including
-- SELECTED_USERS meetings I'm not personally invited to"
--
-- EVENT_CREATE/EVENT_READ/EVENT_UPDATE/EVENT_DELETE (V94) are deliberately granted broadly to
-- almost every role - creating a personal calendar entry is a normal day-to-day action, not an
-- admin-only one. That means EVENT_CREATE can NOT be used as the signal for "should see
-- everyone's meetings" - nearly every employee holds it, and using it as the check would leak
-- every SELECTED_USERS meeting's existence and details to the entire company, defeating the
-- whole point of that visibility option.
--
-- EVENT_MANAGE_ALL is a new, separate, narrowly-granted permission for exactly this: it does
-- not change who can create/edit/delete their OWN events (still EVENT_CREATE/UPDATE/DELETE,
-- unchanged) - it only changes what a Client Admin (who scheduled a SELECTED_USERS meeting for
-- other people) sees back when viewing the calendar: their own meeting, not just other people's
-- ALL_USERS ones. Granted following the exact same "admin-tier only" pattern as
-- ATTENDANCE_RULES_MANAGE (V99): CLIENT_ADMIN (global house role) + tenant-scoped ADMIN for
-- CLI0001.
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('EVENT_MANAGE_ALL', 'View every calendar event in the company, including SELECTED_USERS meetings not personally invited to');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name = 'EVENT_MANAGE_ALL'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name = 'EVENT_MANAGE_ALL'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
