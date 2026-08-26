-- ============================================================
-- V11: Sub-Clients and Sites
--
-- ClientCompany -> SubClient -> Site. Both carry client_company_id
-- directly (denormalized from the SubClient) so tenant-scoped
-- queries never need a join through sub_clients just to filter
-- by tenant.
-- ============================================================

CREATE TABLE sub_clients (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id      BIGINT        NOT NULL,
    sub_client_code        VARCHAR(50)   NOT NULL,
    name                   VARCHAR(150)  NOT NULL,
    description            VARCHAR(255),
    contact_person_name    VARCHAR(150),
    contact_person_email   VARCHAR(150),
    contact_person_phone   VARCHAR(30),
    address                VARCHAR(255),
    city                   VARCHAR(100),
    state                  VARCHAR(100),
    country                VARCHAR(100),
    pincode                VARCHAR(20),
    status                 VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by             BIGINT,
    updated_by             BIGINT,
    CONSTRAINT fk_sub_clients_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_sub_clients_company_code UNIQUE (client_company_id, sub_client_code)
) ENGINE=InnoDB;

CREATE INDEX idx_sub_clients_company_status ON sub_clients (client_company_id, status);

CREATE TABLE sites (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id         BIGINT        NOT NULL,
    sub_client_id              BIGINT        NOT NULL,
    site_code                 VARCHAR(50)   NOT NULL,
    site_name                 VARCHAR(150)  NOT NULL,
    description                VARCHAR(255),
    address                    VARCHAR(255),
    city                       VARCHAR(100),
    state                      VARCHAR(100),
    country                    VARCHAR(100),
    pincode                    VARCHAR(20),
    site_contact_person        VARCHAR(150),
    site_contact_number        VARCHAR(30),
    required_employee_count    INT           NOT NULL DEFAULT 0,
    allow_over_allocation      BOOLEAN       NOT NULL DEFAULT FALSE,
    status                     VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by                 BIGINT,
    updated_by                 BIGINT,
    CONSTRAINT fk_sites_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_sites_sub_client FOREIGN KEY (sub_client_id) REFERENCES sub_clients (id) ON DELETE CASCADE,
    CONSTRAINT uq_sites_company_code UNIQUE (client_company_id, site_code)
) ENGINE=InnoDB;

CREATE INDEX idx_sites_company_status ON sites (client_company_id, status);
CREATE INDEX idx_sites_sub_client ON sites (sub_client_id);
