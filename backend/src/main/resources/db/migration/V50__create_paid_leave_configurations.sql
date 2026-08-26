-- ============================================================
-- V50: Paid Leave Management - tenant-level configuration
--
-- One row per tenant, created lazily on first save - see
-- PaidLeaveConfigService.getEntityOrDefault(). A tenant with no row yet
-- uses the hardcoded defaults (2 days/month, carry-forward allowed, no
-- maximum) documented there.
-- ============================================================

CREATE TABLE paid_leave_configurations (
    client_company_id      BIGINT        NOT NULL PRIMARY KEY,
    monthly_paid_leave      INT          NOT NULL DEFAULT 2,
    allow_carry_forward     BOOLEAN      NOT NULL DEFAULT TRUE,
    maximum_carry_forward   INT,
    updated_at               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by               BIGINT,
    updated_by               BIGINT,
    CONSTRAINT fk_paid_leave_config_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;
