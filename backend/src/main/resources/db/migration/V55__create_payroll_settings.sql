-- ============================================================
-- V55: Payroll Register - tenant-level statutory deduction settings
--
-- One row per tenant, created lazily on first save - see
-- PayrollSettingsService.getEntityOrDefault(). Drives the Payroll
-- Register's EPF/ESI/Professional Tax/HRA calculations; every rate is
-- configurable, not hardcoded, since these vary by state and change
-- over time.
-- ============================================================

CREATE TABLE payroll_settings (
    client_company_id       BIGINT        NOT NULL PRIMARY KEY,
    epf_enabled              BOOLEAN      NOT NULL DEFAULT TRUE,
    epf_employee_percent     DECIMAL(5,2) NOT NULL DEFAULT 12.00,
    epf_employer_percent     DECIMAL(5,2) NOT NULL DEFAULT 13.00,
    esi_enabled              BOOLEAN      NOT NULL DEFAULT TRUE,
    esi_employee_percent     DECIMAL(5,2) NOT NULL DEFAULT 0.75,
    esi_employer_percent     DECIMAL(5,2) NOT NULL DEFAULT 3.25,
    esi_wage_ceiling         DECIMAL(12,2) DEFAULT 21000.00,
    pt_enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    professional_tax         DECIMAL(10,2) NOT NULL DEFAULT 200.00,
    hra_percent              DECIMAL(5,2) NOT NULL DEFAULT 5.00,
    updated_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by               BIGINT,
    CONSTRAINT fk_payroll_settings_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;
