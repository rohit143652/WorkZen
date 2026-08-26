-- ============================================================
-- V63: Advance recovery transactions
--
-- One row per (advance, year, month) actually recovered - historical rows
-- are never rewritten, even if the advance's monthly_recovery_amount is
-- later changed (spec section 15).
-- ============================================================

CREATE TABLE advance_recovery_transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT        NOT NULL,
    employee_id         BIGINT        NOT NULL,
    advance_id          BIGINT        NOT NULL,
    year                INT           NOT NULL,
    month               INT           NOT NULL,
    recovered_amount    DECIMAL(12,2) NOT NULL,
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_advance_recovery_advance FOREIGN KEY (advance_id) REFERENCES employee_advances (id) ON DELETE CASCADE,
    CONSTRAINT fk_advance_recovery_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_advance_recovery_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_advance_recovery_month UNIQUE (advance_id, year, month)
) ENGINE=InnoDB;

CREATE INDEX idx_advance_recovery_employee_month ON advance_recovery_transactions (employee_id, year, month);
