-- ============================================================
-- V90: Leave Request permission grants
--
-- LEAVE_REQUEST_MANAGE (add/approve/reject leave for ANY employee) - only CLIENT_ADMIN (already
-- granted in V89) and, now, SITE_SUPERVISOR in the demo tenant. Nobody else can act on someone
-- else's leave - this is enforced structurally, not just by who has the permission: the
-- self-request endpoint (LeaveRequestController.selfCreate()) always resolves the employee from
-- the CALLER's own login (findByUserId()) and never accepts an employeeId at all, so even if a
-- role somehow had LEAVE_REQUEST_SELF_CREATE without MANAGE, there is no way for them to submit
-- a request for anyone but themselves.
--
-- LEAVE_REQUEST_SELF_CREATE (apply for one's own leave) - granted broadly here, unlike
-- PAYSLIP_SELF_VIEW's "nobody by default" pattern (V84) - self-requesting one's own leave is
-- safe for any role to have, so every demo-tenant role gets it, on top of whatever else they can
-- already do.
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SITE_SUPERVISOR'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('LEAVE_REQUEST_READ', 'LEAVE_REQUEST_MANAGE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE p.name = 'LEAVE_REQUEST_SELF_CREATE'
  AND (
       r.name IN ('CLIENT_ADMIN', 'CLIENT_USER')
    OR (r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
        AND r.name IN ('ADMIN', 'HR_ADMIN', 'SITE_ADMIN', 'SITE_SUPERVISOR', 'ACCOUNTANT'))
  )
  -- Skip any role that already somehow has it, so this migration is safe to design without
  -- worrying about ordering relative to earlier grants.
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
