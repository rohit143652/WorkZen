-- ============================================================
-- V84: PAYSLIP_SELF_VIEW permission - gates the "My Payslip" nav item/page.
--
-- Granted to NOBODY by default - not even SUPER_ADMIN or CLIENT_ADMIN. An admin decides which
-- specific role(s) should see "My Payslip" entirely from Roles -> Edit, by checking this
-- permission for that role - nothing here pre-decides that for them.
--
-- (SUPER_ADMIN can still grant this to any role despite not holding it themselves - the "can
-- only grant what you have" ceiling in RoleService.resolvePermissionsWithCeiling() is bypassed
-- entirely for SUPER_ADMIN, same as it always has been for every other permission.)
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('PAYSLIP_SELF_VIEW', 'View and download your own payslip (My Payslip)');
