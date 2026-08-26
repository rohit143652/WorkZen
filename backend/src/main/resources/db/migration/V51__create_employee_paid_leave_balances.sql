-- ============================================================
-- V51: Paid Leave Management - monthly employee balance ledger
--
-- One row per employee per calendar month. monthly_allocation,
-- carry_forward, extra_leave, and used_leave are kept as separate columns
-- (spec section 5 - never merged); available_leave is their computed total,
-- stored for fast reads. See EmployeePaidLeaveService.resolveMonth() for the
-- idempotent generation logic and EmployeePaidLeaveBalance's class comment
-- for manual_override's role in future Attendance/Leave-Application
-- integration.
-- ============================================================

CREATE TABLE employee_paid_leave_balances (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT        NOT NULL,
    employee_id         BIGINT        NOT NULL,
    year                INT           NOT NULL,
    month               INT           NOT NULL,
    monthly_allocation   DECIMAL(6,2) NOT NULL DEFAULT 0,
    carry_forward        DECIMAL(6,2) NOT NULL DEFAULT 0,
    extra_leave          DECIMAL(6,2) NOT NULL DEFAULT 0,
    used_leave           DECIMAL(6,2) NOT NULL DEFAULT 0,
    available_leave      DECIMAL(6,2) NOT NULL DEFAULT 0,
    manual_override      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_paid_leave_balance_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_paid_leave_balance_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_paid_leave_balance_employee_month UNIQUE (employee_id, year, month)
) ENGINE=InnoDB;

CREATE INDEX idx_paid_leave_balance_company_month ON employee_paid_leave_balances (client_company_id, year, month);
