-- ============================================================
-- V19: Department and Designation master data
--
-- Tenant-scoped admin-managed lookup lists that back the Employee form's
-- Department/Designation dropdowns. Deactivating a row never deletes it -
-- existing employees keep a valid reference, it just stops being offered
-- as a selectable option going forward.
-- ============================================================

CREATE TABLE departments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT        NOT NULL,
    name                VARCHAR(100)  NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          BIGINT,
    CONSTRAINT fk_departments_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_departments_company_name UNIQUE (client_company_id, name)
) ENGINE=InnoDB;

CREATE TABLE designations (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT        NOT NULL,
    name                VARCHAR(100)  NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          BIGINT,
    CONSTRAINT fk_designations_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_designations_company_name UNIQUE (client_company_id, name)
) ENGINE=InnoDB;

CREATE INDEX idx_departments_company_status ON departments (client_company_id, status);
CREATE INDEX idx_designations_company_status ON designations (client_company_id, status);
