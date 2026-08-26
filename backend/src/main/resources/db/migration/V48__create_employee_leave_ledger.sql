-- ============================================================
-- V48: Employee leave ledger (paid-leave balance + carry-forward)
--
-- One row per employee per calendar month. This is what makes "unused
-- paid leave carries forward to next month" actually work: each month's
-- opening_balance is read from the PREVIOUS month's closing_balance for
-- that employee (see MonthlyAttendanceReportService), accrued is that
-- month's earn from the tenant/employee paid-leave policy, paid_days_used
-- is how many of the month's ON_LEAVE days were actually paid (auto-
-- calculated as min(leave taken, opening+accrued), or a specific number
-- if manual_override = TRUE, e.g. an admin correction made directly in the
-- Monthly Report table), and closing_balance = opening + accrued -
-- paid_days_used carries into next month's opening_balance.
--
-- Rows are upserted every time the report is (re)computed for that
-- employee+month, so viewing the same month twice is idempotent - it is
-- NOT a strict once-only accounting ledger. Editing an old month after
-- later months were already generated does not retroactively recompute
-- those later months; regenerate them in order if that happens.
-- ============================================================

CREATE TABLE employee_leave_ledger (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT        NOT NULL,
    employee_id         BIGINT        NOT NULL,
    ledger_year         INT           NOT NULL,
    ledger_month        INT           NOT NULL,
    opening_balance      DECIMAL(6,2) NOT NULL DEFAULT 0,
    accrued              DECIMAL(6,2) NOT NULL DEFAULT 0,
    leave_taken_days      INT          NOT NULL DEFAULT 0,
    paid_days_used        DECIMAL(6,2) NOT NULL DEFAULT 0,
    manual_override       BOOLEAN      NOT NULL DEFAULT FALSE,
    closing_balance       DECIMAL(6,2) NOT NULL DEFAULT 0,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by            BIGINT,
    CONSTRAINT fk_leave_ledger_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_ledger_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_leave_ledger_employee_month UNIQUE (employee_id, ledger_year, ledger_month)
) ENGINE=InnoDB;

CREATE INDEX idx_leave_ledger_company_month ON employee_leave_ledger (client_company_id, ledger_year, ledger_month);
