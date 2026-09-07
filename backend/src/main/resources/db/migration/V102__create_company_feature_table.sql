-- ============================================================
-- V102: Company-wise feature/module access, controlled by Super Admin
--
-- One row per (client_company_id, feature_code). Absence of a row for a given feature code is
-- treated as ENABLED by the application (see FeatureAccessService.isEnabled()) so that adding a
-- brand new feature code in the future never silently locks out every existing company that has
-- no row for it yet - a company only loses access to something once the Super Admin has
-- EXPLICITLY disabled it, never by default. This migration seeds explicit TRUE rows for every
-- feature code below for every EXISTING company anyway (belt-and-suspenders), purely so the
-- Manage Features screen always has something concrete to show/toggle immediately rather than
-- inferring "not configured = on" silently the first time someone opens it.
--
-- Feature codes are plain strings (not a foreign-keyed lookup table) - matching how this
-- codebase already does permissions (permissions.name is a plain string, not an enum table
-- either) and keeping it trivial to add a new feature code later (one new string constant, no
-- schema change).
-- ============================================================

CREATE TABLE company_feature (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id  BIGINT       NOT NULL,
    feature_code       VARCHAR(60)  NOT NULL,
    enabled            BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_by         BIGINT       NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_company_feature_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_company_feature UNIQUE (client_company_id, feature_code)
) ENGINE=InnoDB;

CREATE INDEX idx_company_feature_lookup ON company_feature (client_company_id, feature_code);

-- Seed every currently-enforced feature code as TRUE for every existing tenant, preserving
-- exactly today's behavior (nothing is newly restricted by this migration).
INSERT INTO company_feature (client_company_id, feature_code, enabled)
SELECT c.id, f.code, TRUE
FROM client_companies c
CROSS JOIN (
    SELECT 'ATTENDANCE_MANAGEMENT' AS code
    UNION ALL SELECT 'EMPLOYEE_SELF_ATTENDANCE'
    UNION ALL SELECT 'ADMIN_MANUAL_ATTENDANCE'
    UNION ALL SELECT 'SUPERVISOR_MANUAL_ATTENDANCE'
    UNION ALL SELECT 'HR_MANUAL_ATTENDANCE'
) f
WHERE NOT EXISTS (
    SELECT 1 FROM company_feature cf WHERE cf.client_company_id = c.id AND cf.feature_code = f.code
);
