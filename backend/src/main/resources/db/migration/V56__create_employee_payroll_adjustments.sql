-- ============================================================
-- V56: Payroll Register - per-employee-per-month manual adjustments
--
-- Advance/Uniform deduction and Allowance are the only two figures on the
-- Payroll Register with no other source of truth (EPF/ESI/PT are formula-
-- derived from payroll_settings) - entered directly against one employee's
-- one month, never affecting any other month.
-- ============================================================

CREATE TABLE employee_payroll_adjustments (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id           BIGINT        NOT NULL,
    employee_id                 BIGINT        NOT NULL,
    year                        INT           NOT NULL,
    month                       INT           NOT NULL,
    advance_uniform_deduction    DECIMAL(10,2) NOT NULL DEFAULT 0,
    allowance                    DECIMAL(10,2) NOT NULL DEFAULT 0,
    updated_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by                  BIGINT,
    CONSTRAINT fk_payroll_adjustment_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_payroll_adjustment_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_payroll_adjustment_employee_month UNIQUE (employee_id, year, month)
) ENGINE=InnoDB;

CREATE INDEX idx_payroll_adjustment_company_month ON employee_payroll_adjustments (client_company_id, year, month);
