-- ============================================================
-- V62: Employee Advances - grant table
--
-- One row per advance an employee takes (spec section 12/13) - never
-- overwritten. Recovery against each advance is tracked separately in
-- advance_recovery_transactions (V63).
-- ============================================================

CREATE TABLE employee_advances (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id        BIGINT        NOT NULL,
    employee_id              BIGINT        NOT NULL,
    advance_date              DATE         NOT NULL,
    amount                   DECIMAL(12,2) NOT NULL,
    reason                   VARCHAR(255),
    payment_mode              VARCHAR(20),
    monthly_recovery_amount    DECIMAL(12,2) NOT NULL,
    status                   VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    settled_amount            DECIMAL(12,2),
    created_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by               BIGINT,
    updated_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by               BIGINT,
    CONSTRAINT fk_employee_advance_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_advance_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_employee_advance_employee ON employee_advances (employee_id, status);
CREATE INDEX idx_employee_advance_company ON employee_advances (client_company_id);
