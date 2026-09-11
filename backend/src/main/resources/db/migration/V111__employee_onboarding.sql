-- ============================================================
-- V111: Employee Self-Onboarding, Login Invitation & Profile Completion
--
-- No existing password-reset/email-verification/invitation mechanism was found anywhere in this
-- codebase (only RefreshToken exists, which is JWT refresh - unrelated) - employee_onboarding_
-- invitations is a genuinely new table, not a duplicate of anything.
--
-- Reuses the EXISTING Employee<->User one-to-one link (employees.user_id, already unique) and
-- the existing password encoder/hashing - this table only adds the INVITATION layer on top:
-- token + one-time code, both hashed, never the plain values, single-use, expiring.
-- ============================================================

CREATE TABLE employee_onboarding_invitations (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id       BIGINT       NOT NULL,
    employee_id             BIGINT       NOT NULL,
    user_id                 BIGINT       NOT NULL,
    token_hash              VARCHAR(255) NOT NULL,
    verification_code_hash  VARCHAR(255) NOT NULL,
    expires_at              DATETIME     NOT NULL,
    used_at                 DATETIME     NULL,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempt_count           INT          NOT NULL DEFAULT 0,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              BIGINT       NULL,
    resent_at               DATETIME     NULL,
    CONSTRAINT fk_onboarding_invitation_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_onboarding_invitation_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- token_hash needs a fast unique lookup (the public "open this link" endpoint looks up by the
-- hash of whatever token was in the URL) but NOT a UNIQUE constraint - resending intentionally
-- leaves the OLD row in place (marked SUPERSEDED, see EmployeeOnboardingService), so token
-- hashes are only guaranteed unique among PENDING rows, not globally across history.
CREATE INDEX idx_onboarding_invitation_token ON employee_onboarding_invitations (token_hash);
CREATE INDEX idx_onboarding_invitation_employee ON employee_onboarding_invitations (employee_id, status);

-- Onboarding status - deliberately separate from employees.status (ACTIVE/INACTIVE, whether the
-- person still works here) and users.is_active (whether their login is enabled at all). This
-- tracks progress through the invitation+profile-completion journey specifically.
ALTER TABLE employees
    ADD COLUMN onboarding_status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED' AFTER status;

-- Existing employees (created before this feature) already have full profiles filled in by an
-- admin directly - marking them PROFILE_COMPLETE avoids every pre-existing employee suddenly
-- showing up as "onboarding incomplete" the moment this migration runs.
UPDATE employees SET onboarding_status = 'PROFILE_COMPLETE' WHERE user_id IS NOT NULL;
UPDATE employees SET onboarding_status = 'NOT_STARTED' WHERE user_id IS NULL;

-- Emergency Contact - a genuinely new section (no equivalent fields existed on Employee at all)
-- needed for profile completion; the other candidate sections (Personal/Contact/Address/Bank/
-- Statutory/Photo) already exist on this table.
ALTER TABLE employees
    ADD COLUMN emergency_contact_name         VARCHAR(150) NULL,
    ADD COLUMN emergency_contact_relationship VARCHAR(50)  NULL,
    ADD COLUMN emergency_contact_mobile       VARCHAR(20)  NULL;

-- New permissions - resending an invitation and viewing onboarding status are admin-tier actions
-- on the same footing as the existing EMPLOYEE_ASSIGN_ROLE-style permissions, not something every
-- EMPLOYEE_READ holder should be able to do.
INSERT INTO permissions (name, description) VALUES
    ('EMPLOYEE_ONBOARDING_MANAGE', 'Enable login, resend invitations, and view onboarding status for employees');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE p.name = 'EMPLOYEE_ONBOARDING_MANAGE'
  AND r.name IN ('SUPER_ADMIN', 'CLIENT_ADMIN', 'ADMIN', 'HR_ADMIN')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
