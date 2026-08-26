-- ============================================================
-- V10: Client Companies (tenants)
--
-- SUPER_ADMIN (users.client_company_id IS NULL) owns/manages every
-- Client Company. Each Client Company is a tenant boundary: every
-- tenant-scoped table below carries a client_company_id and every
-- tenant-scoped query MUST filter by it - see TenantContextService
-- and the *_module repositories for enforcement.
-- ============================================================

CREATE TABLE client_companies (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_code           VARCHAR(50)   NOT NULL,
    company_name           VARCHAR(150)  NOT NULL,
    legal_name             VARCHAR(150),
    email                  VARCHAR(150),
    phone                  VARCHAR(30),
    alternate_phone        VARCHAR(30),
    address                VARCHAR(255),
    city                   VARCHAR(100),
    state                  VARCHAR(100),
    country                VARCHAR(100),
    pincode                VARCHAR(20),
    contact_person_name    VARCHAR(150),
    contact_person_email   VARCHAR(150),
    contact_person_phone   VARCHAR(30),
    status                 VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by             BIGINT,
    updated_by             BIGINT,
    CONSTRAINT uq_client_companies_code UNIQUE (company_code)
) ENGINE=InnoDB;

CREATE INDEX idx_client_companies_status ON client_companies (status);
