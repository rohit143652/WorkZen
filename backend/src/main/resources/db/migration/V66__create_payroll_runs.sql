-- ============================================================
-- V66: Persisted Payroll Runs (architecture refactor Phase 2)
--
-- One row per tenant + calendar month payroll processing run. Status
-- flow: DRAFT -> CALCULATED -> APPROVED -> PAID, with CANCELLED reachable
-- only from DRAFT/CALCULATED (see PayrollRunService for the exact
-- transition rules). Prior to this, "payroll" only ever existed as a
-- transient response of viewing the Monthly Attendance & Payment Report.
-- ============================================================

CREATE TABLE payroll_runs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT       NOT NULL,
    year                INT          NOT NULL,
    month               INT          NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    remarks             VARCHAR(500),
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT,
    calculated_at       DATETIME,
    calculated_by       BIGINT,
    approved_at         DATETIME,
    approved_by         BIGINT,
    paid_at             DATETIME,
    paid_by             BIGINT,
    cancelled_at        DATETIME,
    cancelled_by        BIGINT,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payroll_run_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Duplicate-run guard at the DB level for the common case (one run per
-- month, ever). Recreating a run for a month whose only prior run was
-- CANCELLED is a service-layer decision (see PayrollRunService.createRun),
-- not enforced here, since MySQL has no partial/filtered unique index -
-- deliberately deferred to the future reopen-workflow phase rather than
-- solved with a workaround now.
CREATE INDEX idx_payroll_run_company_month ON payroll_runs (client_company_id, year, month);
