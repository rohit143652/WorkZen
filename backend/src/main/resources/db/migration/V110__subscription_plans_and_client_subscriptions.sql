-- ============================================================
-- V110: Subscription Plans, Plan Features, Client Subscriptions, Subscription History
--
-- EXTENDS the existing ClientCompany -> CompanyFeature -> FeatureCode -> FeatureAccessService
-- architecture rather than replacing any of it (see FeatureAccessService.java for the updated
-- effective-access logic). CompanyFeature is reused AS-IS as the client-level override layer -
-- no schema change needed there at all: a CompanyFeature row already means "explicit decision"
-- and its ABSENCE already means "nothing explicit configured" - that absence now falls through
-- to the subscription plan's default instead of always meaning "enabled" (which was the
-- pre-subscription behavior, safe only because there was no paid tier to bypass).
--
-- Every EXISTING CompanyFeature row (enabled=true or false) is preserved untouched and keeps
-- meaning exactly what it always meant: an explicit override for that tenant. Nothing here
-- deletes or rewrites a single CompanyFeature row.
-- ============================================================

CREATE TABLE subscription_plans (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_code                   VARCHAR(30)   NOT NULL UNIQUE,
    plan_name                   VARCHAR(100)  NOT NULL,
    description                 VARCHAR(500)  NULL,
    employee_limit              INT           NULL, -- NULL only when custom_employee_limit_allowed=true (Enterprise) and no fixed number applies yet
    custom_employee_limit_allowed BOOLEAN     NOT NULL DEFAULT FALSE,
    monthly_price               DECIMAL(10,2) NULL, -- NULL for "custom pricing" plans (Enterprise)
    yearly_price                DECIMAL(10,2) NULL,
    active                      BOOLEAN       NOT NULL DEFAULT TRUE,
    display_order               INT           NOT NULL DEFAULT 0,
    created_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by                  BIGINT        NULL,
    updated_by                  BIGINT        NULL
) ENGINE=InnoDB;

-- One row per (plan, feature code) the plan explicitly turns on. A feature code with NO row for
-- a given plan means that plan does not include it - same "row = explicit" convention as
-- CompanyFeature, kept deliberately consistent across both tables.
CREATE TABLE plan_features (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id       BIGINT      NOT NULL,
    feature_code  VARCHAR(60) NOT NULL,
    enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_plan_features_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans (id) ON DELETE CASCADE,
    CONSTRAINT uq_plan_feature UNIQUE (plan_id, feature_code)
) ENGINE=InnoDB;

-- One ACTIVE-at-a-time subscription per client company. Changing plan/cycle/status creates a
-- NEW row here is NOT how this works - this table holds the CURRENT state; subscription_history
-- below is the append-only log of every change, so nothing about switching plans requires
-- guessing which of several client_subscriptions rows is "the current one".
CREATE TABLE client_subscriptions (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id        BIGINT        NOT NULL UNIQUE,
    plan_id                   BIGINT        NOT NULL,
    billing_cycle             VARCHAR(20)   NOT NULL DEFAULT 'MONTHLY',
    start_date                DATE          NOT NULL,
    end_date                  DATE          NULL,
    status                    VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    employee_limit_override   INT           NULL,
    monthly_price_override    DECIMAL(10,2) NULL,
    yearly_price_override     DECIMAL(10,2) NULL,
    notes                     VARCHAR(500)  NULL,
    created_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by                BIGINT        NULL,
    updated_by                BIGINT        NULL,
    CONSTRAINT fk_client_subscription_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_client_subscription_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE subscription_history (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id     BIGINT       NOT NULL,
    previous_plan_id       BIGINT       NULL,
    new_plan_id            BIGINT       NULL,
    previous_billing_cycle VARCHAR(20)  NULL,
    new_billing_cycle      VARCHAR(20)  NULL,
    previous_employee_limit INT         NULL,
    new_employee_limit     INT          NULL,
    previous_status         VARCHAR(20) NULL,
    new_status              VARCHAR(20) NULL,
    change_date             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason                  VARCHAR(255) NULL,
    changed_by              BIGINT      NULL,
    CONSTRAINT fk_subscription_history_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_subscription_history_company ON subscription_history (client_company_id, change_date);

-- ---------- Permissions - Super Admin only, same catch-all pattern as every other migration ----------
INSERT INTO permissions (name, description) VALUES
    ('SUBSCRIPTION_PLAN_READ', 'View subscription plans and their features'),
    ('SUBSCRIPTION_PLAN_CREATE', 'Create a new subscription plan'),
    ('SUBSCRIPTION_PLAN_UPDATE', 'Edit a subscription plan and its features'),
    ('SUBSCRIPTION_PLAN_ACTIVATE', 'Activate a subscription plan'),
    ('SUBSCRIPTION_PLAN_DEACTIVATE', 'Deactivate a subscription plan'),
    ('CLIENT_SUBSCRIPTION_READ', 'View a client company''s subscription and history'),
    ('CLIENT_SUBSCRIPTION_UPDATE', 'Change a client company''s plan, billing cycle, or dates'),
    ('CLIENT_SUBSCRIPTION_MANAGE', 'Suspend, cancel, or override a client company''s subscription/employee limit');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name IN ('SUBSCRIPTION_PLAN_READ', 'SUBSCRIPTION_PLAN_CREATE', 'SUBSCRIPTION_PLAN_UPDATE',
                 'SUBSCRIPTION_PLAN_ACTIVATE', 'SUBSCRIPTION_PLAN_DEACTIVATE', 'CLIENT_SUBSCRIPTION_READ',
                 'CLIENT_SUBSCRIPTION_UPDATE', 'CLIENT_SUBSCRIPTION_MANAGE')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ---------- Default plans ----------
-- Prices/limits per the client's spec exactly. Nothing about these numbers is hardcoded in
-- application code - Super Admin can edit every field here from the Plans screen afterward.
INSERT INTO subscription_plans (plan_code, plan_name, description, employee_limit, custom_employee_limit_allowed, monthly_price, yearly_price, active, display_order) VALUES
    ('STARTER',      'Starter',      'For small teams just getting started',           25,  FALSE, 1999.00, 19990.00, TRUE, 1),
    ('BASIC',        'Basic',        'Attendance and leave management for growing teams', 50,  FALSE, 2999.00, 29990.00, TRUE, 2),
    ('PROFESSIONAL', 'Professional', 'Full payroll and salary management',              100, FALSE, 4999.00, 49990.00, TRUE, 3),
    ('BUSINESS',     'Business',     'Everything, including overtime tracking',         250, FALSE, 9999.00, 99990.00, TRUE, 4),
    ('ENTERPRISE',   'Enterprise',   'Custom limits and pricing for large organizations', NULL, TRUE, NULL, NULL, TRUE, 5);

-- ---------- Default plan features ----------
-- ONLY features with a real, working module behind them are ever set here (see FeatureCode.java
-- javadoc and this migration's own audit notes below) - SHIFT_MANAGEMENT, EXPENSE_MANAGEMENT,
-- RECRUITMENT, and ASSET_MANAGEMENT are deliberately absent from EVERY plan including Enterprise,
-- because none of those modules actually exist in this codebase yet (no entity, no service, no
-- controller, no frontend page - confirmed by direct inspection, not assumption). Advertising
-- them as "included" in a paid plan would be selling something that doesn't exist.
--
-- STARTER: the essentials - employee records, attendance, self check-in, holidays.
INSERT INTO plan_features (plan_id, feature_code, enabled)
SELECT p.id, f.code, TRUE FROM subscription_plans p
CROSS JOIN (
    SELECT 'EMPLOYEE_MANAGEMENT' AS code UNION ALL SELECT 'ATTENDANCE_MANAGEMENT'
    UNION ALL SELECT 'EMPLOYEE_SELF_ATTENDANCE' UNION ALL SELECT 'HOLIDAY_CALENDAR'
) f
WHERE p.plan_code = 'STARTER';

-- BASIC: Starter + leave requests, admin manual attendance marking, reports.
INSERT INTO plan_features (plan_id, feature_code, enabled)
SELECT p.id, f.code, TRUE FROM subscription_plans p
CROSS JOIN (
    SELECT 'EMPLOYEE_MANAGEMENT' AS code UNION ALL SELECT 'ATTENDANCE_MANAGEMENT'
    UNION ALL SELECT 'EMPLOYEE_SELF_ATTENDANCE' UNION ALL SELECT 'HOLIDAY_CALENDAR'
    UNION ALL SELECT 'LEAVE_MANAGEMENT' UNION ALL SELECT 'ADMIN_MANUAL_ATTENDANCE' UNION ALL SELECT 'REPORTS'
) f
WHERE p.plan_code = 'BASIC';

-- PROFESSIONAL: Basic + salary structures, full payroll, supervisor/HR manual attendance.
INSERT INTO plan_features (plan_id, feature_code, enabled)
SELECT p.id, f.code, TRUE FROM subscription_plans p
CROSS JOIN (
    SELECT 'EMPLOYEE_MANAGEMENT' AS code UNION ALL SELECT 'ATTENDANCE_MANAGEMENT'
    UNION ALL SELECT 'EMPLOYEE_SELF_ATTENDANCE' UNION ALL SELECT 'HOLIDAY_CALENDAR'
    UNION ALL SELECT 'LEAVE_MANAGEMENT' UNION ALL SELECT 'ADMIN_MANUAL_ATTENDANCE' UNION ALL SELECT 'REPORTS'
    UNION ALL SELECT 'SALARY_MANAGEMENT' UNION ALL SELECT 'PAYROLL'
    UNION ALL SELECT 'SUPERVISOR_MANUAL_ATTENDANCE' UNION ALL SELECT 'HR_MANUAL_ATTENDANCE'
) f
WHERE p.plan_code = 'PROFESSIONAL';

-- BUSINESS: Professional + overtime.
INSERT INTO plan_features (plan_id, feature_code, enabled)
SELECT p.id, f.code, TRUE FROM subscription_plans p
CROSS JOIN (
    SELECT 'EMPLOYEE_MANAGEMENT' AS code UNION ALL SELECT 'ATTENDANCE_MANAGEMENT'
    UNION ALL SELECT 'EMPLOYEE_SELF_ATTENDANCE' UNION ALL SELECT 'HOLIDAY_CALENDAR'
    UNION ALL SELECT 'LEAVE_MANAGEMENT' UNION ALL SELECT 'ADMIN_MANUAL_ATTENDANCE' UNION ALL SELECT 'REPORTS'
    UNION ALL SELECT 'SALARY_MANAGEMENT' UNION ALL SELECT 'PAYROLL'
    UNION ALL SELECT 'SUPERVISOR_MANUAL_ATTENDANCE' UNION ALL SELECT 'HR_MANUAL_ATTENDANCE'
    UNION ALL SELECT 'OVERTIME_MANAGEMENT'
) f
WHERE p.plan_code = 'BUSINESS';

-- ENTERPRISE: every genuinely implemented feature.
INSERT INTO plan_features (plan_id, feature_code, enabled)
SELECT p.id, f.code, TRUE FROM subscription_plans p
CROSS JOIN (
    SELECT 'EMPLOYEE_MANAGEMENT' AS code UNION ALL SELECT 'ATTENDANCE_MANAGEMENT'
    UNION ALL SELECT 'EMPLOYEE_SELF_ATTENDANCE' UNION ALL SELECT 'HOLIDAY_CALENDAR'
    UNION ALL SELECT 'LEAVE_MANAGEMENT' UNION ALL SELECT 'ADMIN_MANUAL_ATTENDANCE' UNION ALL SELECT 'REPORTS'
    UNION ALL SELECT 'SALARY_MANAGEMENT' UNION ALL SELECT 'PAYROLL'
    UNION ALL SELECT 'SUPERVISOR_MANUAL_ATTENDANCE' UNION ALL SELECT 'HR_MANUAL_ATTENDANCE'
    UNION ALL SELECT 'OVERTIME_MANAGEMENT'
) f
WHERE p.plan_code = 'ENTERPRISE';

-- ---------- Existing client migration ----------
-- Every EXISTING client company gets an ACTIVE Enterprise-plan subscription with NO end date
-- and NO fixed employee limit override (i.e. effectively unlimited, matching the "count active
-- employees, cap at limit" rule never actually applying to them). This is deliberately the
-- MOST PERMISSIVE plan, not an arbitrary "safe-looking" middle tier: every existing client's
-- CompanyFeature rows already fully control their actual feature access exactly as before (see
-- FeatureAccessService - overrides still take priority over plan defaults), so the ONLY new
-- constraint this migration could introduce for them is the employee limit - and giving every
-- existing client "no fixed limit" means that constraint effectively does not apply to anyone
-- who didn't already agree to a specific plan. A real commercial plan can be assigned to each
-- one manually afterward, at the business's own pace, without any surprise lockout on day one.
INSERT INTO client_subscriptions (client_company_id, plan_id, billing_cycle, start_date, end_date, status, employee_limit_override, notes)
SELECT c.id,
       (SELECT id FROM subscription_plans WHERE plan_code = 'ENTERPRISE'),
       'MONTHLY', COALESCE(c.created_at, CURDATE()), NULL, 'ACTIVE', NULL,
       'Auto-created during subscription system migration - existing client, unrestricted by design (see V110 migration notes)'
FROM client_companies c
WHERE NOT EXISTS (SELECT 1 FROM client_subscriptions cs WHERE cs.client_company_id = c.id);

INSERT INTO subscription_history (client_company_id, previous_plan_id, new_plan_id, previous_billing_cycle, new_billing_cycle,
                                   previous_employee_limit, new_employee_limit, previous_status, new_status, reason)
SELECT cs.client_company_id, NULL, cs.plan_id, NULL, cs.billing_cycle, NULL, NULL, NULL, cs.status,
       'Initial subscription created during migration to the subscription plan system'
FROM client_subscriptions cs
WHERE cs.notes LIKE 'Auto-created during subscription system migration%';
