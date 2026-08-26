-- ============================================================
-- production-schema-superadmin-only.sql   (MANUAL SCRIPT - NOT a Flyway migration)
--
-- Consolidated database setup for a FRESH server/production deployment:
--   - Full schema: every table, foreign key, and INDEX from V1 through V82
--   - Only the SUPER_ADMIN login (username: super_admin / password: admin123)
--   - NO demo/dummy data: no sample client company, no demo employees,
--     no dummy attendance/payroll/advance/salary-structure records
--
-- This is the concatenation of every migration EXCEPT the ones that seed
-- demo/dummy data for local development (V17, V22, V25, V29, V30, V36,
-- V42, V49, V59, V60, V78, V83) - each of those creates a fake sample
-- tenant (CLI0001), fake employees, or fake attendance/payroll/advance
-- test records meant only for trying the app out locally, never for a
-- real deployment.
--
-- USAGE:
--   1. Create an empty database: CREATE DATABASE workforce_auth CHARACTER SET utf8mb4;
--   2. Run this whole file against it (MySQL Workbench, or:
--        mysql -u root -p workforce_auth < production-schema-superadmin-only.sql
--   3. Start the backend normally - Flyway will see all these versions as
--      already applied (it still needs its own bookkeeping - see the note
--      at the very end of this file) and will only run any NEW migrations
--      you add after V82 from then on.
--
-- IMMEDIATELY after first login as super_admin, change the password -
-- "admin123" is a known default sitting in this file and in the source
-- Flyway migration it came from (V4__insert_super_admin.sql).
-- ============================================================

-- ============================================================
-- Source: V1__create_auth_tables.sql
-- ============================================================
-- ============================================================
-- V1: Core authentication, RBAC and audit tables
-- ============================================================

CREATE TABLE users (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    username                VARCHAR(100)  NOT NULL,
    email                   VARCHAR(150)  NOT NULL,
    password                VARCHAR(255)  NOT NULL,
    first_name              VARCHAR(100),
    last_name               VARCHAR(100),
    is_active               BOOLEAN       NOT NULL DEFAULT TRUE,
    is_locked               BOOLEAN       NOT NULL DEFAULT FALSE,
    failed_login_attempts   INT           NOT NULL DEFAULT 0,
    last_login_at           DATETIME,
    password_changed_at     DATETIME,
    created_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE roles (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    description  VARCHAR(255),
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_roles_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE permissions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    description  VARCHAR(255),
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_permissions_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE role_permissions (
    role_id        BIGINT NOT NULL,
    permission_id  BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    token        VARCHAR(512)  NOT NULL,
    expiry_date  DATETIME      NOT NULL,
    revoked      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE login_attempts (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100)  NOT NULL,
    ip_address    VARCHAR(64),
    user_agent    VARCHAR(255),
    success       BOOLEAN       NOT NULL,
    attempted_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT,
    action       VARCHAR(100)  NOT NULL,
    ip_address   VARCHAR(64),
    user_agent   VARCHAR(255),
    description  VARCHAR(500),
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Indexes to support common lookups
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens (expiry_date);
CREATE INDEX idx_login_attempts_username ON login_attempts (username);
CREATE INDEX idx_login_attempts_attempted_at ON login_attempts (attempted_at);
CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);


-- ============================================================
-- Source: V2__insert_roles.sql
-- ============================================================
-- ============================================================
-- V2: Default roles
-- ============================================================

INSERT INTO roles (name, description) VALUES
    ('SUPER_ADMIN', 'Full system access via database-driven permissions'),
    ('ADMIN',       'User management and dashboard access'),
    ('MANAGER',     'Dashboard access and assigned management functionality'),
    ('USER',        'Standard dashboard access'),
    ('CLIENT',      'Client-related functionality only');


-- ============================================================
-- Source: V3__insert_permissions.sql
-- ============================================================
-- ============================================================
-- V3: Default permissions
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('USER_CREATE',        'Create users'),
    ('USER_READ',          'View users'),
    ('USER_UPDATE',        'Update users, assign roles, activate/deactivate/unlock'),
    ('USER_DELETE',        'Delete users'),

    ('ROLE_CREATE',        'Create roles'),
    ('ROLE_READ',          'View roles'),
    ('ROLE_UPDATE',        'Update roles and their permissions'),
    ('ROLE_DELETE',        'Delete roles'),

    ('PERMISSION_CREATE',  'Create permissions'),
    ('PERMISSION_READ',    'View permissions'),
    ('PERMISSION_UPDATE',  'Update permissions'),
    ('PERMISSION_DELETE',  'Delete permissions'),

    ('CLIENT_CREATE',      'Create clients'),
    ('CLIENT_READ',        'View clients'),
    ('CLIENT_UPDATE',      'Update clients'),
    ('CLIENT_DELETE',      'Delete clients'),

    ('DASHBOARD_VIEW',     'View the dashboard'),
    ('AUDIT_LOG_READ',     'View audit logs'),
    ('PASSWORD_CHANGE',    'Change own password');


-- ============================================================
-- Source: V4__insert_super_admin.sql
-- ============================================================
-- ============================================================
-- V4: Default SUPER_ADMIN user
-- Username: super_admin
-- Password: admin123  (BCrypt hash below, strength 12)
--
-- IMPORTANT: this default password MUST be changed immediately
-- after first login in any real deployment.
-- ============================================================

INSERT INTO users (username, email, password, first_name, last_name, is_active, is_locked, password_changed_at)
VALUES (
    'super_admin',
    'super_admin@workforce.local',
    '$2b$12$Vg5B2vmiI7t1Mr31z1DrhufpruExhhDE3GZCvORNyh0IZmxfeWIvS',
    'Super',
    'Admin',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'super_admin' AND r.name = 'SUPER_ADMIN';


-- ============================================================
-- Source: V5__insert_role_permissions.sql
-- ============================================================
-- ============================================================
-- V5: Role -> Permission mapping
--
-- SUPER_ADMIN receives every permission purely through this
-- database relationship - it is never hardcoded as "all access"
-- in Java. If a new permission is added later, it must also be
-- explicitly granted here (or via the Role Management API) for
-- SUPER_ADMIN, or any other role, to receive it.
-- ============================================================

-- SUPER_ADMIN: every permission that currently exists
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN';

-- ADMIN: user management + role/permission visibility + dashboard + audit
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN (
      'USER_CREATE', 'USER_READ', 'USER_UPDATE', 'USER_DELETE',
      'ROLE_READ', 'PERMISSION_READ',
      'DASHBOARD_VIEW', 'AUDIT_LOG_READ', 'PASSWORD_CHANGE'
  );

-- MANAGER: dashboard + client management + own password
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'MANAGER'
  AND p.name IN (
      'DASHBOARD_VIEW', 'CLIENT_READ', 'CLIENT_UPDATE', 'PASSWORD_CHANGE'
  );

-- USER: dashboard + own password only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'USER'
  AND p.name IN ('DASHBOARD_VIEW', 'PASSWORD_CHANGE');

-- CLIENT: client-scoped read access + own password
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT'
  AND p.name IN ('CLIENT_READ', 'PASSWORD_CHANGE');


-- ============================================================
-- Source: V6__create_employees_table.sql
-- ============================================================
-- ============================================================
-- V6: Employees table
--
-- One Employee -> Zero or One User (employees.user_id, nullable, unique).
-- An employee can exist with no login account at all (user_id IS NULL).
-- ============================================================

CREATE TABLE employees (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_code             VARCHAR(50)   NOT NULL,
    first_name                VARCHAR(100)  NOT NULL,
    middle_name                VARCHAR(100),
    last_name                 VARCHAR(100)  NOT NULL,
    email                     VARCHAR(150)  NOT NULL,
    mobile_number             VARCHAR(30),
    alternate_mobile_number   VARCHAR(30),
    date_of_birth             DATE,
    gender                    VARCHAR(20),
    joining_date              DATE          NOT NULL,
    department                VARCHAR(100)  NOT NULL,
    designation               VARCHAR(100)  NOT NULL,
    employment_type           VARCHAR(50),
    address                   VARCHAR(255),
    city                      VARCHAR(100),
    state                     VARCHAR(100),
    country                   VARCHAR(100),
    pincode                   VARCHAR(20),
    status                    VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    user_id                   BIGINT,
    created_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_employees_code UNIQUE (employee_code),
    CONSTRAINT uq_employees_email UNIQUE (email),
    CONSTRAINT uq_employees_user_id UNIQUE (user_id),
    CONSTRAINT fk_employees_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_employees_department ON employees (department);
CREATE INDEX idx_employees_status ON employees (status);
CREATE INDEX idx_employees_last_name ON employees (last_name);


-- ============================================================
-- Source: V7__add_must_change_password.sql
-- ============================================================
-- ============================================================
-- V7: Force password change after admin-issued temporary passwords
-- ============================================================

ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE AFTER password_changed_at;


-- ============================================================
-- Source: V8__insert_employee_permissions.sql
-- ============================================================
-- ============================================================
-- V8: Employee & Dashboard permissions
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('EMPLOYEE_CREATE',         'Create employees'),
    ('EMPLOYEE_READ',           'View employees'),
    ('EMPLOYEE_UPDATE',         'Update employee information'),
    ('EMPLOYEE_DELETE',         'Deactivate/soft-delete employees'),
    ('EMPLOYEE_ACTIVATE',       'Reactivate a deactivated employee'),
    ('EMPLOYEE_DEACTIVATE',     'Deactivate an employee'),
    ('EMPLOYEE_ENABLE_LOGIN',   'Create or reactivate a login account for an employee'),
    ('EMPLOYEE_DISABLE_LOGIN',  'Disable an employee''s login account'),
    ('EMPLOYEE_RESET_PASSWORD', 'Issue a temporary password for an employee''s login account'),
    ('EMPLOYEE_ASSIGN_ROLE',    'Change the role assigned to an employee''s login account'),
    ('DASHBOARD_ANALYTICS',     'View dashboard summary statistics');


-- ============================================================
-- Source: V9__grant_employee_permissions.sql
-- ============================================================
-- ============================================================
-- V9: Grant employee/dashboard permissions to roles
--
-- SUPER_ADMIN receives everything, purely via this database
-- relationship, matching the existing V5 pattern. ADMIN receives
-- a sensible operational subset; MANAGER/USER/CLIENT are left
-- untouched (no employee-management capability by default).
-- ============================================================

-- SUPER_ADMIN: every permission that currently exists (including new ones)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ADMIN: employee management + login-lifecycle management + dashboard analytics
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN (
      'EMPLOYEE_CREATE', 'EMPLOYEE_READ', 'EMPLOYEE_UPDATE',
      'EMPLOYEE_ACTIVATE', 'EMPLOYEE_DEACTIVATE',
      'EMPLOYEE_ENABLE_LOGIN', 'EMPLOYEE_DISABLE_LOGIN',
      'EMPLOYEE_RESET_PASSWORD', 'EMPLOYEE_ASSIGN_ROLE',
      'DASHBOARD_ANALYTICS'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );


-- ============================================================
-- Source: V10__create_client_companies_table.sql
-- ============================================================
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


-- ============================================================
-- Source: V11__create_subclients_and_sites.sql
-- ============================================================
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


-- ============================================================
-- Source: V12__employee_tenant_and_assignments.sql
-- ============================================================
-- ============================================================
-- V12: Employee tenant ownership + Employee <-> Site assignment history
-- ============================================================

ALTER TABLE employees
    ADD COLUMN client_company_id BIGINT AFTER id,
    ADD CONSTRAINT fk_employees_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE RESTRICT;

CREATE INDEX idx_employees_company_status ON employees (client_company_id, status);

-- Employee codes were globally unique (V6). For multi-tenant use they should
-- be unique per tenant instead (two different client companies may both use
-- "EMP001"). Replace the global constraint with a composite one. Existing
-- rows have client_company_id = NULL (pre-tenant "house" employees, if any);
-- MySQL unique indexes treat each NULL as distinct, so this remains safe.
ALTER TABLE employees DROP INDEX uq_employees_code;
ALTER TABLE employees ADD CONSTRAINT uq_employees_company_code UNIQUE (client_company_id, employee_code);

CREATE TABLE employee_site_assignments (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id    BIGINT        NOT NULL,
    employee_id          BIGINT        NOT NULL,
    site_id              BIGINT        NOT NULL,
    assignment_type      VARCHAR(30)   NOT NULL DEFAULT 'REGULAR',
    start_date           DATE          NOT NULL,
    end_date             DATE,
    is_primary           BOOLEAN       NOT NULL DEFAULT TRUE,
    status                VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    remarks               VARCHAR(255),
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by            BIGINT,
    updated_by            BIGINT,
    CONSTRAINT fk_assignments_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_site FOREIGN KEY (site_id) REFERENCES sites (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_assignments_company_site ON employee_site_assignments (client_company_id, site_id);
CREATE INDEX idx_assignments_employee_status ON employee_site_assignments (employee_id, status);
CREATE INDEX idx_assignments_site_status ON employee_site_assignments (site_id, status);
CREATE INDEX idx_assignments_dates ON employee_site_assignments (start_date, end_date);


-- ============================================================
-- Source: V13__users_tenant_ownership.sql
-- ============================================================
-- ============================================================
-- V13: Tenant ownership on users
--
-- SUPER_ADMIN and any other "house" user: client_company_id IS NULL.
-- CLIENT_ADMIN / employee-linked logins: client_company_id = their tenant.
-- This is set exclusively by the backend (from TenantContextService or
-- from the owning Employee's client_company_id) - it is never accepted
-- from a request body.
-- ============================================================

ALTER TABLE users
    ADD COLUMN client_company_id BIGINT AFTER id,
    ADD CONSTRAINT fk_users_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE RESTRICT;

CREATE INDEX idx_users_company ON users (client_company_id);


-- ============================================================
-- Source: V14__insert_tenant_roles.sql
-- ============================================================
-- ============================================================
-- V14: New roles for the multi-tenant model
-- Existing roles (SUPER_ADMIN, ADMIN, MANAGER, USER, CLIENT) are untouched.
-- ============================================================

INSERT INTO roles (name, description) VALUES
    ('CLIENT_ADMIN', 'Administrator for a single Client Company tenant - manages that tenant''s employees, sub-clients, sites and assignments'),
    ('CLIENT_USER',  'Limited-access user scoped to a single Client Company tenant');


-- ============================================================
-- Source: V15__insert_tenant_permissions.sql
-- ============================================================
-- ============================================================
-- V15: New permissions for Client Companies, Sub-Clients, Sites,
-- Employee Assignments, and the client-scoped dashboard.
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('CLIENT_COMPANY_CREATE',     'Create client companies (tenants)'),
    ('CLIENT_COMPANY_READ',       'View client companies'),
    ('CLIENT_COMPANY_UPDATE',     'Update client company profile'),
    ('CLIENT_COMPANY_ACTIVATE',   'Reactivate a deactivated client company'),
    ('CLIENT_COMPANY_DEACTIVATE', 'Deactivate a client company'),

    ('CLIENT_DASHBOARD_VIEW',     'View the tenant-scoped client dashboard'),
    ('CLIENT_PROFILE_READ',       'View own client company profile'),
    ('CLIENT_PROFILE_UPDATE',     'Update own client company profile'),

    ('SUBCLIENT_CREATE',          'Create sub-clients within the current tenant'),
    ('SUBCLIENT_READ',            'View sub-clients'),
    ('SUBCLIENT_UPDATE',          'Update sub-clients'),
    ('SUBCLIENT_ACTIVATE',        'Reactivate a deactivated sub-client'),
    ('SUBCLIENT_DEACTIVATE',      'Deactivate a sub-client'),

    ('SITE_CREATE',               'Create sites within the current tenant'),
    ('SITE_READ',                 'View sites'),
    ('SITE_UPDATE',               'Update sites'),
    ('SITE_ACTIVATE',             'Reactivate a deactivated site'),
    ('SITE_DEACTIVATE',           'Deactivate a site'),

    ('EMPLOYEE_ASSIGN',           'Assign employees to sites, including bulk assignment'),
    ('EMPLOYEE_TRANSFER',         'Transfer an employee from one site to another'),
    ('EMPLOYEE_ASSIGNMENT_READ',  'View employee-site assignment history');


-- ============================================================
-- Source: V16__grant_tenant_permissions.sql
-- ============================================================
-- ============================================================
-- V16: Grant tenant-related permissions
--
-- SUPER_ADMIN: everything, via the same "all current permissions"
-- pattern as V5/V9 - purely database-driven, no Java conditionals.
-- CLIENT_ADMIN: the operational subset for managing a single tenant
-- (spec section 27). CLIENT_ADMIN deliberately does NOT receive
-- CLIENT_COMPANY_CREATE/ACTIVATE/DEACTIVATE - only SUPER_ADMIN
-- manages the tenant records themselves.
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN'
  AND p.name IN (
      'CLIENT_DASHBOARD_VIEW', 'CLIENT_PROFILE_READ', 'CLIENT_PROFILE_UPDATE',
      'EMPLOYEE_CREATE', 'EMPLOYEE_READ', 'EMPLOYEE_UPDATE', 'EMPLOYEE_DEACTIVATE', 'EMPLOYEE_ACTIVATE',
      'EMPLOYEE_ENABLE_LOGIN', 'EMPLOYEE_DISABLE_LOGIN', 'EMPLOYEE_RESET_PASSWORD', 'EMPLOYEE_ASSIGN_ROLE',
      'SITE_CREATE', 'SITE_READ', 'SITE_UPDATE', 'SITE_ACTIVATE', 'SITE_DEACTIVATE',
      'SUBCLIENT_CREATE', 'SUBCLIENT_READ', 'SUBCLIENT_UPDATE', 'SUBCLIENT_ACTIVATE', 'SUBCLIENT_DEACTIVATE',
      'EMPLOYEE_ASSIGN', 'EMPLOYEE_TRANSFER', 'EMPLOYEE_ASSIGNMENT_READ',
      'USER_READ', 'USER_UPDATE', 'DASHBOARD_VIEW', 'PASSWORD_CHANGE'
  );


-- ============================================================
-- Source: V18__remove_subclient_layer.sql
-- ============================================================
-- ============================================================
-- V18: Remove the Sub-Client layer entirely
--
-- Hierarchy simplified from ClientCompany -> SubClient -> Site
-- to ClientCompany -> Site directly, per business decision. This
-- is an additive forward migration (never edits V11/V17 in place)
-- so it's safe to apply even if those already ran in a real
-- environment.
-- ============================================================

-- Drop the FK + its supporting index, then the column, before dropping the table itself.
ALTER TABLE sites DROP FOREIGN KEY fk_sites_sub_client;
ALTER TABLE sites DROP INDEX idx_sites_sub_client;
ALTER TABLE sites DROP COLUMN sub_client_id;

DROP TABLE sub_clients;

-- Remove the now-meaningless SUBCLIENT_* permissions and their grants.
DELETE rp FROM role_permissions rp
JOIN permissions p ON p.id = rp.permission_id
WHERE p.name LIKE 'SUBCLIENT_%';

DELETE FROM permissions WHERE name LIKE 'SUBCLIENT_%';

-- Cosmetic: the 5 sites seeded in V17 were named as generic buildings under a
-- sub-client (e.g. "Main Building" under sub-client "XYZ IT Park"). Now that a
-- Site belongs directly to the Client Company, rename them to read naturally
-- as the client's own sites.
UPDATE sites SET site_name = 'XYZ IT Park'  WHERE site_code = 'SITE0001';
UPDATE sites SET site_name = 'ABC Mall'     WHERE site_code = 'SITE0002';
UPDATE sites SET site_name = 'DEF Hospital' WHERE site_code = 'SITE0003';
UPDATE sites SET site_name = 'PQR Tower'    WHERE site_code = 'SITE0004';
UPDATE sites SET site_name = 'LMN School'   WHERE site_code = 'SITE0005';


-- ============================================================
-- Source: V19__create_department_designation_tables.sql
-- ============================================================
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


-- ============================================================
-- Source: V20__insert_department_designation_permissions.sql
-- ============================================================
-- ============================================================
-- V20: Department & Designation permissions
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('DEPARTMENT_READ',    'View departments (needed to populate the Employee form dropdown)'),
    ('DEPARTMENT_MANAGE',  'Add, rename, activate, or deactivate departments'),
    ('DESIGNATION_READ',   'View designations (needed to populate the Employee form dropdown)'),
    ('DESIGNATION_MANAGE', 'Add, rename, activate, or deactivate designations');


-- ============================================================
-- Source: V21__grant_department_designation_permissions.sql
-- ============================================================
-- ============================================================
-- V21: Grant Department & Designation permissions
--
-- SUPER_ADMIN: everything, via the same "all current permissions" pattern
-- used everywhere else. CLIENT_ADMIN: both READ and MANAGE for each, so a
-- Client Admin can add new departments/designations (e.g. "Site Manager",
-- "Site Supervisor") without SUPER_ADMIN involvement, exactly as employees
-- and sites already work.
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN'
  AND p.name IN ('DEPARTMENT_READ', 'DEPARTMENT_MANAGE', 'DESIGNATION_READ', 'DESIGNATION_MANAGE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );


-- ============================================================
-- Source: V23__tenant_scoped_roles.sql
-- ============================================================
-- ============================================================
-- V23: Tenant-scoped custom roles
--
-- roles.client_company_id NULL = "system"/"house" role (SUPER_ADMIN, ADMIN,
-- MANAGER, USER, CLIENT, CLIENT_ADMIN, CLIENT_USER - all existing rows stay
-- NULL, untouched). Non-NULL = a custom role created by that tenant's own
-- Client Admin (e.g. "HR_ADMIN", "SITE_SUPERVISOR").
--
-- The old global UNIQUE(name) is replaced with UNIQUE(client_company_id, name).
-- MySQL/InnoDB treats each NULL in a unique index as distinct from every
-- other NULL, so this constraint alone does NOT stop two different system
-- (NULL-scope) roles from sharing a name at the database level - but system
-- roles are only ever created by trusted Flyway migrations or by SUPER_ADMIN
-- through RoleService, which already checks
-- existsByClientCompanyIdIsNullAndNameIgnoreCase() before insert, so this is
-- an acceptable trade-off. What the constraint DOES fully guarantee at the
-- database level is what actually matters here: each tenant's own custom
-- role names are unique within that tenant.
--
-- (An earlier version of this migration used a STORED generated column to
-- get airtight global+per-tenant uniqueness in one index. That hit a known
-- MySQL/InnoDB limitation - Error 1215 "Cannot add foreign key constraint" -
-- when adding a STORED generated column to a table that already has a
-- foreign key, even though the statement doesn't touch any FK. Simpler and
-- more portable to drop that approach entirely.)
-- ============================================================

ALTER TABLE roles
    ADD COLUMN client_company_id BIGINT NULL AFTER id;

ALTER TABLE roles
    ADD CONSTRAINT fk_roles_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE;

ALTER TABLE roles
    DROP INDEX uq_roles_name;

ALTER TABLE roles
    ADD CONSTRAINT uq_roles_company_name UNIQUE (client_company_id, name);

CREATE INDEX idx_roles_company ON roles (client_company_id);


-- ============================================================
-- Source: V24__grant_client_admin_role_management.sql
-- ============================================================
-- ============================================================
-- V24: Grant CLIENT_ADMIN the ability to manage their own tenant's roles
--
-- This was a pre-existing gap: CLIENT_ADMIN could enable an employee's
-- login and pick a role, but lacked ROLE_READ, so GET /api/roles would
-- 403 for them. Now they get ROLE_READ/CREATE/UPDATE/DELETE, plus
-- PERMISSION_READ (needed to populate the permission checklist when
-- building a custom role) - but see RoleService for the two security
-- guardrails this depends on:
--   1. A non-SUPER_ADMIN can only create/update/delete roles that belong
--      to their OWN tenant (roles.client_company_id) - never global/house
--      roles like SUPER_ADMIN, ADMIN, MANAGER.
--   2. A non-SUPER_ADMIN can never grant a role a permission they do not
--      themselves currently hold - closes the privilege-escalation path
--      where a Client Admin could otherwise create a custom role with
--      SUPER_ADMIN-only permissions (which PERMISSION_READ lets them see
--      the names of, but never actually attach) and assign it to an
--      employee.
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name IN ('ROLE_READ', 'ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_DELETE', 'PERMISSION_READ')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );


-- ============================================================
-- Source: V26__create_attendance_table.sql
-- ============================================================
-- ============================================================
-- V26: Attendance
--
-- One row per employee per calendar day. UNIQUE(client_company_id,
-- employee_id, attendance_date) is the database-level backstop for the
-- "mark once" business rule - AttendanceService.mark() also checks this
-- explicitly first for a clean error message, but the constraint means
-- even a race between two concurrent requests can't create a duplicate.
-- ============================================================

CREATE TABLE attendance (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT        NOT NULL,
    employee_id         BIGINT        NOT NULL,
    site_id             BIGINT        NOT NULL,
    attendance_date     DATE          NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    remarks             VARCHAR(255),
    marked_by           BIGINT,
    updated_by          BIGINT,
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_site FOREIGN KEY (site_id) REFERENCES sites (id) ON DELETE CASCADE,
    CONSTRAINT uq_attendance_employee_date UNIQUE (client_company_id, employee_id, attendance_date)
) ENGINE=InnoDB;

CREATE INDEX idx_attendance_company_date ON attendance (client_company_id, attendance_date);
CREATE INDEX idx_attendance_site_date ON attendance (site_id, attendance_date);
CREATE INDEX idx_attendance_employee_date ON attendance (employee_id, attendance_date);


-- ============================================================
-- Source: V27__insert_attendance_permissions.sql
-- ============================================================
-- ============================================================
-- V27: Attendance permissions
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('ATTENDANCE_CREATE', 'Mark attendance for today or a previous date, for an actively-assigned employee'),
    ('ATTENDANCE_READ',   'View attendance records and history'),
    ('ATTENDANCE_UPDATE', 'Change an already-marked attendance record (normally Client Admin only)');


-- ============================================================
-- Source: V28__grant_attendance_permissions.sql
-- ============================================================
-- ============================================================
-- V28: Grant attendance permissions
--
-- SUPER_ADMIN: everything, via the same catch-all pattern used everywhere
-- else. CLIENT_ADMIN (global house role): full CREATE/READ/UPDATE, since a
-- Client Admin is who's meant to make corrections. For CLI0001's
-- tenant-scoped example roles (V25): SITE_ADMIN and SITE_SUPERVISOR get
-- CREATE+READ only (per spec: they can mark attendance and view history,
-- but cannot change an already-marked day); the tenant-scoped "ADMIN" role
-- mirrors CLIENT_ADMIN and also gets UPDATE.
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name IN ('ATTENDANCE_CREATE', 'ATTENDANCE_READ', 'ATTENDANCE_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Tenant-scoped ADMIN for CLI0001 (mirrors CLIENT_ADMIN)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('ATTENDANCE_CREATE', 'ATTENDANCE_READ', 'ATTENDANCE_UPDATE');

-- SITE_ADMIN for CLI0001: can mark and view, cannot change once marked
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SITE_ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('ATTENDANCE_CREATE', 'ATTENDANCE_READ');

-- SITE_SUPERVISOR for CLI0001: can mark and view, cannot change once marked
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SITE_SUPERVISOR'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('ATTENDANCE_CREATE', 'ATTENDANCE_READ');


-- ============================================================
-- Source: V31__add_pay_structure_columns.sql
-- ============================================================
-- ============================================================
-- V31: Designation base pay + per-employee salary override
--
-- designations.base_pay is the fixed payment structure for that
-- designation, applying to every employee holding it by default.
-- employees.salary_override is NULL for "use the designation's base pay"
-- and non-NULL for an explicit per-employee amount (e.g. the designation
-- pays 10000 but one employee was raised to 15000) - EmployeeService
-- computes the effective salary as override-if-present-else-base-pay.
-- ============================================================

ALTER TABLE designations
    ADD COLUMN base_pay DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER name;

ALTER TABLE employees
    ADD COLUMN salary_override DECIMAL(12,2) NULL AFTER designation;


-- ============================================================
-- Source: V32__insert_salary_permissions.sql
-- ============================================================
-- ============================================================
-- V32: Employee salary permissions
--
-- Deliberately separate from EMPLOYEE_READ/EMPLOYEE_UPDATE - salary is
-- more sensitive than general employee data, so a role can view/edit an
-- employee's profile without automatically seeing or changing pay figures.
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('EMPLOYEE_SALARY_READ',   'View an employee''s effective salary (designation base pay or personal override)'),
    ('EMPLOYEE_SALARY_UPDATE', 'Set or clear an employee''s personal salary override');


-- ============================================================
-- Source: V33__grant_salary_permissions.sql
-- ============================================================
-- ============================================================
-- V33: Grant salary permissions
--
-- SUPER_ADMIN: everything, via the usual catch-all. CLIENT_ADMIN (global)
-- and the tenant-scoped ADMIN/HR_ADMIN roles for CLI0001 get both
-- READ+UPDATE (HR is who'd plausibly set pay). ACCOUNTANT gets READ only
-- (their job is visibility into pay, not changing it). SITE_ADMIN and
-- SITE_SUPERVISOR deliberately get neither - they manage site operations
-- and attendance, not payroll.
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name IN ('EMPLOYEE_SALARY_READ', 'EMPLOYEE_SALARY_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('EMPLOYEE_SALARY_READ', 'EMPLOYEE_SALARY_UPDATE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'HR_ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('EMPLOYEE_SALARY_READ', 'EMPLOYEE_SALARY_UPDATE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ACCOUNTANT'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name = 'EMPLOYEE_SALARY_READ';


-- ============================================================
-- Source: V34__add_designation_pay_structure.sql
-- ============================================================
-- ============================================================
-- V34: Proper payroll structure on designations
--
-- The existing base_pay column (added in V31) becomes "Basic Salary" -
-- same column, just a clearer name at the Java/API layer going forward
-- (DesignationService/Employee* now expose it as basicSalary). This
-- migration adds the two missing pieces of a real payroll structure:
-- PF (Provident Fund) deduction percentage, and a fixed "other
-- deductions" amount (uniform, canteen, etc.) - so Net Salary can be
-- computed as Basic - PF - Other Deductions, not just a single number.
-- ============================================================

ALTER TABLE designations
    ADD COLUMN pf_percentage DECIMAL(5,2) NOT NULL DEFAULT 12.00 AFTER base_pay;

ALTER TABLE designations
    ADD COLUMN other_deductions DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER pf_percentage;


-- ============================================================
-- Source: V35__add_employee_pay_overrides.sql
-- ============================================================
-- ============================================================
-- V35: Proper payroll structure on employees (personal overrides)
--
-- The existing salary_override column (added in V31) becomes
-- "Basic Salary Override" (same column, clearer Java/API name:
-- basicSalaryOverride). Adds the matching PF%/other-deductions override
-- columns. Each of the three is INDEPENDENTLY nullable: an employee can
-- have just a basic salary raise (the common case - matches "designation
-- pays 10000, this one employee got raised to 15000") while still
-- inheriting the designation's PF% and other deductions, or override any
-- combination of the three.
-- ============================================================

ALTER TABLE employees
    ADD COLUMN pf_percentage_override DECIMAL(5,2) NULL AFTER salary_override;

ALTER TABLE employees
    ADD COLUMN other_deductions_override DECIMAL(12,2) NULL AFTER pf_percentage_override;


-- ============================================================
-- Source: V37__create_salary_components_table.sql
-- ============================================================
-- ============================================================
-- V37: Salary Components (database-driven, not hardcoded)
--
-- A component is one line item on a payslip: BASIC, HRA, CONVEYANCE, PF,
-- ESI, etc. component_type says which side of the payslip it belongs on;
-- calculation_type says how its amount is derived within a Salary
-- Structure. FORMULA calculation is deliberately NOT supported (no unsafe
-- dynamic expression evaluation) - only FIXED, PERCENTAGE_OF_BASIC,
-- PERCENTAGE_OF_GROSS, and MANUAL are implemented; PER_DAY/PER_HOUR belong
-- to Payroll Processing (attendance-driven), not the static structure
-- definition, and will be handled there when that module is built.
-- ============================================================

CREATE TABLE salary_components (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT        NOT NULL,
    component_code      VARCHAR(50)   NOT NULL,
    component_name      VARCHAR(150)  NOT NULL,
    component_type      VARCHAR(30)   NOT NULL,
    calculation_type     VARCHAR(30)   NOT NULL,
    value                DECIMAL(12,2),
    percentage           DECIMAL(5,2),
    is_taxable           BOOLEAN       NOT NULL DEFAULT TRUE,
    is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
    display_order        INT           NOT NULL DEFAULT 0,
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           BIGINT,
    CONSTRAINT fk_salary_components_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_salary_components_company_code UNIQUE (client_company_id, component_code)
) ENGINE=InnoDB;

CREATE INDEX idx_salary_components_company_active ON salary_components (client_company_id, is_active);


-- ============================================================
-- Source: V38__create_salary_structures_tables.sql
-- ============================================================
-- ============================================================
-- V38: Salary Structures + their component line items
--
-- A Salary Structure is a named, reusable payroll template (e.g.
-- "Housekeeping Staff Grade A") built from a set of Salary Components,
-- each with its own calculation type/amount/percentage WITHIN this
-- structure (salary_structure_components.calculation_type/amount/
-- percentage override the component's own defaults, so the same "HRA"
-- component can be 3000 fixed in one structure and 10% of basic in
-- another).
-- ============================================================

CREATE TABLE salary_structures (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT        NOT NULL,
    structure_code       VARCHAR(50)   NOT NULL,
    structure_name       VARCHAR(150)  NOT NULL,
    description          VARCHAR(255),
    effective_from       DATE          NOT NULL,
    effective_to         DATE,
    status               VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           BIGINT,
    CONSTRAINT fk_salary_structures_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT uq_salary_structures_company_code UNIQUE (client_company_id, structure_code)
) ENGINE=InnoDB;

CREATE INDEX idx_salary_structures_company_status ON salary_structures (client_company_id, status);

CREATE TABLE salary_structure_components (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    salary_structure_id   BIGINT        NOT NULL,
    salary_component_id   BIGINT        NOT NULL,
    calculation_type       VARCHAR(30)   NOT NULL,
    amount                 DECIMAL(12,2),
    percentage              DECIMAL(5,2),
    is_active               BOOLEAN       NOT NULL DEFAULT TRUE,
    display_order           INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_ssc_structure FOREIGN KEY (salary_structure_id) REFERENCES salary_structures (id) ON DELETE CASCADE,
    CONSTRAINT fk_ssc_component FOREIGN KEY (salary_component_id) REFERENCES salary_components (id) ON DELETE RESTRICT,
    CONSTRAINT uq_ssc_structure_component UNIQUE (salary_structure_id, salary_component_id)
) ENGINE=InnoDB;

CREATE INDEX idx_ssc_structure ON salary_structure_components (salary_structure_id);


-- ============================================================
-- Source: V39__create_employee_salary_structures.sql
-- ============================================================
-- ============================================================
-- V39: Employee <-> Salary Structure assignment history
--
-- Mirrors the same "never overwrite, always end the old row and start a
-- new one" pattern already used for employee_site_assignments. An
-- employee's current structure is the row with status='ACTIVE' and
-- effective_to IS NULL; assigning a new structure ends the previous one
-- (effective_to = day before the new structure's effective_from) rather
-- than deleting or mutating it, so payroll for a past period can always
-- resolve "which structure applied on that date" correctly.
-- ============================================================

CREATE TABLE employee_salary_structures (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id     BIGINT        NOT NULL,
    employee_id           BIGINT        NOT NULL,
    salary_structure_id   BIGINT        NOT NULL,
    effective_from        DATE          NOT NULL,
    effective_to          DATE,
    status                VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by            BIGINT,
    CONSTRAINT fk_ess_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_ess_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_ess_structure FOREIGN KEY (salary_structure_id) REFERENCES salary_structures (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_ess_employee_status ON employee_salary_structures (employee_id, status);
CREATE INDEX idx_ess_company ON employee_salary_structures (client_company_id);
CREATE INDEX idx_ess_dates ON employee_salary_structures (effective_from, effective_to);


-- ============================================================
-- Source: V40__insert_salary_structure_permissions.sql
-- ============================================================
-- ============================================================
-- V40: Salary Structure permissions
--
-- Component CRUD is deliberately gated by the same SALARY_STRUCTURE_*
-- permissions rather than a separate set - components are a sub-concern
-- of structures, and the master spec's permission list (section 72) only
-- names structure-level + SALARY_ASSIGN, not component-level permissions.
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('SALARY_STRUCTURE_CREATE', 'Create salary structures and salary components'),
    ('SALARY_STRUCTURE_READ',   'View salary structures, their components, and employee assignments'),
    ('SALARY_STRUCTURE_UPDATE', 'Edit salary structures and salary components, activate/deactivate'),
    ('SALARY_STRUCTURE_DELETE', 'Delete a salary structure that has never been assigned to an employee'),
    ('SALARY_ASSIGN',           'Assign a salary structure to an employee');


-- ============================================================
-- Source: V41__grant_salary_structure_permissions.sql
-- ============================================================
-- ============================================================
-- V41: Grant Salary Structure permissions
--
-- Same distribution as the earlier salary permissions (V33): SUPER_ADMIN
-- everything; CLIENT_ADMIN (global) and tenant ADMIN/HR_ADMIN (CLI0001)
-- get full CRUD + assign; ACCOUNTANT gets read-only; SITE_ADMIN/
-- SITE_SUPERVISOR get neither - payroll setup is not a site-operations
-- concern.
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name IN ('SALARY_STRUCTURE_CREATE', 'SALARY_STRUCTURE_READ', 'SALARY_STRUCTURE_UPDATE', 'SALARY_STRUCTURE_DELETE', 'SALARY_ASSIGN')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('SALARY_STRUCTURE_CREATE', 'SALARY_STRUCTURE_READ', 'SALARY_STRUCTURE_UPDATE', 'SALARY_STRUCTURE_DELETE', 'SALARY_ASSIGN');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'HR_ADMIN'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name IN ('SALARY_STRUCTURE_CREATE', 'SALARY_STRUCTURE_READ', 'SALARY_STRUCTURE_UPDATE', 'SALARY_STRUCTURE_DELETE', 'SALARY_ASSIGN');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ACCOUNTANT'
  AND r.client_company_id = (SELECT id FROM client_companies WHERE company_code = 'CLI0001')
  AND p.name = 'SALARY_STRUCTURE_READ';


-- ============================================================
-- Source: V43__add_salary_type_to_structures.sql
-- ============================================================
-- ============================================================
-- V43: Salary Type on Salary Structures (spec sections 8, 13-16)
--
-- MONTHLY/DAILY/HOURLY/CONTRACT is a property of the structure itself
-- (a "Housekeeping Daily Wager" template is always daily-rated), not of
-- an individual employee's assignment. daily_rate/hourly_rate are plain
-- reference values for future Attendance/Payroll to multiply against;
-- they are NOT calculated here (no attendance data available yet) and are
-- independent of the salary_structure_components rows, which still drive
-- gross_earnings/net_salary for MONTHLY/CONTRACT structures.
-- ============================================================

ALTER TABLE salary_structures
    ADD COLUMN salary_type VARCHAR(20) NOT NULL DEFAULT 'MONTHLY' AFTER structure_name,
    ADD COLUMN daily_rate  DECIMAL(12,2) AFTER description,
    ADD COLUMN hourly_rate DECIMAL(12,2) AFTER daily_rate;

CREATE INDEX idx_salary_structures_company_type ON salary_structures (client_company_id, salary_type);


-- ============================================================
-- Source: V44__drop_legacy_designation_pay_columns.sql
-- ============================================================
-- ============================================================
-- V44: Remove the legacy Designation/Employee pay-structure columns
--
-- Before the dedicated Salary Structure module existed, a simple payroll
-- structure lived directly on designations (base_pay/pf_percentage/
-- other_deductions) with per-employee overrides (salary_override/
-- pf_percentage_override/other_deductions_override). That duplicated what
-- salary_structure_module now does properly (configurable components,
-- calculation types, full history via employee_salary_structures) and was
-- a source of confusion - two different places an employee's "salary"
-- could apparently live. An employee's salary is now assigned exclusively
-- via a Salary Structure; Designations are pure organisational master data.
-- ============================================================

ALTER TABLE designations
    DROP COLUMN base_pay,
    DROP COLUMN pf_percentage,
    DROP COLUMN other_deductions;

ALTER TABLE employees
    DROP COLUMN salary_override,
    DROP COLUMN pf_percentage_override,
    DROP COLUMN other_deductions_override;


-- ============================================================
-- Source: V45__insert_monthly_payment_report_permission.sql
-- ============================================================
-- ============================================================
-- V45: Monthly Attendance & Payment report export permission
--
-- Bulk (all-employees-at-once) download is restricted to CLIENT_ADMIN, per
-- the requirement that only the tenant's admin - not SITE_ADMIN,
-- SITE_SUPERVISOR, or any other operational role - can pull this report.
-- SUPER_ADMIN is granted it too, purely to keep the established "SUPER_ADMIN
-- has every permission" invariant used throughout every prior migration;
-- the report is tenant-scoped by construction (TenantContextService.
-- requireCurrentTenantId() throws for a house-only SUPER_ADMIN session), so
-- this is consistent with how EMPLOYEE_CREATE etc. are already granted.
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('MONTHLY_PAYMENT_REPORT_EXPORT', 'Download the monthly attendance & payment Excel report for all employees in the tenant');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name = 'MONTHLY_PAYMENT_REPORT_EXPORT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN'
  AND p.name = 'MONTHLY_PAYMENT_REPORT_EXPORT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );


-- ============================================================
-- Source: V46__create_attendance_settings.sql
-- ============================================================
-- ============================================================
-- V46: Attendance settings (paid-leave policy)
--
-- One row per tenant, created lazily on first save - see
-- AttendanceSettingsService.getEntityOrDefault(). No default rows are
-- seeded here; a tenant with no row simply uses the hardcoded defaults
-- (paid, 2 days/month) until it explicitly saves a preference.
-- ============================================================

CREATE TABLE attendance_settings (
    client_company_id          BIGINT        NOT NULL PRIMARY KEY,
    paid_leave_enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    paid_leave_days_per_month  INT           NOT NULL DEFAULT 2,
    updated_at                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by                 BIGINT,
    CONSTRAINT fk_attendance_settings_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- ============================================================
-- Source: V47__create_employee_paid_leave_overrides.sql
-- ============================================================
-- ============================================================
-- V47: Per-employee paid-leave overrides
--
-- Lets an admin give a specific employee more (or fewer, or zero) paid
-- leave days than the tenant default in attendance_settings - e.g. someone
-- on approved paid medical leave needing 3 paid days this month instead of
-- the usual 2. Both columns are independently nullable: a null value means
-- "inherit the tenant default for this field" (see AttendanceSettingsService).
-- ============================================================

CREATE TABLE employee_paid_leave_overrides (
    employee_id                BIGINT        NOT NULL PRIMARY KEY,
    client_company_id          BIGINT        NOT NULL,
    paid_leave_enabled         BOOLEAN,
    paid_leave_days_per_month  INT,
    updated_at                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by                 BIGINT,
    CONSTRAINT fk_paid_leave_override_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_paid_leave_override_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_paid_leave_override_company ON employee_paid_leave_overrides (client_company_id);


-- ============================================================
-- Source: V48__create_employee_leave_ledger.sql
-- ============================================================
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


-- ============================================================
-- Source: V50__create_paid_leave_configurations.sql
-- ============================================================
-- ============================================================
-- V50: Paid Leave Management - tenant-level configuration
--
-- One row per tenant, created lazily on first save - see
-- PaidLeaveConfigService.getEntityOrDefault(). A tenant with no row yet
-- uses the hardcoded defaults (2 days/month, carry-forward allowed, no
-- maximum) documented there.
-- ============================================================

CREATE TABLE paid_leave_configurations (
    client_company_id      BIGINT        NOT NULL PRIMARY KEY,
    monthly_paid_leave      INT          NOT NULL DEFAULT 2,
    allow_carry_forward     BOOLEAN      NOT NULL DEFAULT TRUE,
    maximum_carry_forward   INT,
    updated_at               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by               BIGINT,
    updated_by               BIGINT,
    CONSTRAINT fk_paid_leave_config_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- ============================================================
-- Source: V51__create_employee_paid_leave_balances.sql
-- ============================================================
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


-- ============================================================
-- Source: V52__create_employee_extra_paid_leaves.sql
-- ============================================================
-- ============================================================
-- V52: Paid Leave Management - extra/additional leave grant history
--
-- A one-time additional Paid Leave grant (e.g. 30 days Medical Leave),
-- kept completely separate from the monthly allocation (spec section 16).
-- Never overwritten - cancelling one sets status=CANCELLED but the row
-- (and its original created_at/created_by) is preserved for history
-- (spec section 6).
-- ============================================================

CREATE TABLE employee_extra_paid_leaves (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_company_id   BIGINT        NOT NULL,
    employee_id         BIGINT        NOT NULL,
    leave_days          DECIMAL(6,2)  NOT NULL,
    reason              VARCHAR(20)   NOT NULL,
    start_date          DATE          NOT NULL,
    end_date            DATE,
    remark              VARCHAR(255),
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT,
    CONSTRAINT fk_extra_paid_leave_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_extra_paid_leave_company FOREIGN KEY (client_company_id) REFERENCES client_companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_extra_paid_leave_employee ON employee_extra_paid_leaves (employee_id, start_date);
CREATE INDEX idx_extra_paid_leave_company ON employee_extra_paid_leaves (client_company_id);


-- ============================================================
-- Source: V53__insert_paid_leave_permissions.sql
-- ============================================================
-- ============================================================
-- V53: Paid Leave Management permissions
--
-- SUPER_ADMIN: everything, via the same "all current permissions" pattern
-- as every prior permission migration. CLIENT_ADMIN: full control within
-- their own tenant (spec section 10/12) - grant/update/cancel extra leave,
-- view any employee's balance/history, and manage the tenant's
-- configuration. Ordinary employees need none of these to view their OWN
-- leave - that's handled by self-access in EmployeePaidLeaveService, not
-- by a permission grant (spec section 10: an employee must never be able
-- to grant leave, modify balances, modify configuration, or view another
-- employee's leave - giving them a permission row here would risk exactly
-- that if a future page ever forgets to add the self-check).
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('PAID_LEAVE_READ',          'View any employee''s Paid Leave balance and monthly history'),
    ('PAID_LEAVE_GRANT',         'Grant additional (extra) Paid Leave to an employee'),
    ('PAID_LEAVE_UPDATE',        'Update or cancel an extra Paid Leave grant, or manually correct a month''s used leave'),
    ('PAID_LEAVE_HISTORY_READ',  'View an employee''s extra Paid Leave grant history'),
    ('PAID_LEAVE_CONFIG_READ',   'View the tenant''s Paid Leave configuration'),
    ('PAID_LEAVE_CONFIG_UPDATE', 'Change the tenant''s Paid Leave configuration (monthly allocation, carry-forward rules)');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name IN ('PAID_LEAVE_READ', 'PAID_LEAVE_GRANT', 'PAID_LEAVE_UPDATE', 'PAID_LEAVE_HISTORY_READ',
                 'PAID_LEAVE_CONFIG_READ', 'PAID_LEAVE_CONFIG_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name IN ('PAID_LEAVE_READ', 'PAID_LEAVE_GRANT', 'PAID_LEAVE_UPDATE', 'PAID_LEAVE_HISTORY_READ',
                 'PAID_LEAVE_CONFIG_READ', 'PAID_LEAVE_CONFIG_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );


-- ============================================================
-- Source: V54__drop_legacy_attendance_leave_tables.sql
-- ============================================================
-- ============================================================
-- V54: Drop the superseded ad-hoc paid-leave tables
--
-- Before the dedicated Paid Leave Management module (leave_module) existed,
-- a simpler paid-leave mechanism lived directly in attendance_module:
-- attendance_settings (tenant default), employee_paid_leave_overrides
-- (per-employee rate override), and employee_leave_ledger (monthly
-- balance). All of that is now superseded by paid_leave_configurations,
-- employee_extra_paid_leaves, and employee_paid_leave_balances (V50-52),
-- which match the Paid Leave Management spec exactly and are the only
-- system MonthlyAttendanceReportService now reads from. Keeping both
-- systems around would recreate the same "two places leave lives"
-- confusion this project has already run into once with Designation-based
-- pay vs. Salary Structures.
-- ============================================================

DROP TABLE IF EXISTS employee_leave_ledger;
DROP TABLE IF EXISTS employee_paid_leave_overrides;
DROP TABLE IF EXISTS attendance_settings;


-- ============================================================
-- Source: V55__create_payroll_settings.sql
-- ============================================================
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


-- ============================================================
-- Source: V56__create_employee_payroll_adjustments.sql
-- ============================================================
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


-- ============================================================
-- Source: V57__insert_payroll_permission.sql
-- ============================================================
-- ============================================================
-- V57: Payroll Register permission
--
-- CLIENT_ADMIN only, same convention as MONTHLY_PAYMENT_REPORT_EXPORT -
-- this report exposes statutory deduction figures (EPF/ESI/PT) and net
-- pay for every employee at once, so it's restricted to the tenant's
-- admin, not any operational role. SUPER_ADMIN granted per the usual
-- "SUPER_ADMIN has every permission" invariant.
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('PAYROLL_REGISTER_EXPORT', 'View/download the full Payroll Register (EPF/ESI/PT, net pay) for all employees, and manage payroll settings');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name = 'PAYROLL_REGISTER_EXPORT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name = 'PAYROLL_REGISTER_EXPORT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );


-- ============================================================
-- Source: V58__drop_unused_hra_percent.sql
-- ============================================================
-- ============================================================
-- V58: Drop unused hra_percent column from payroll_settings
--
-- Payroll Register was folded into the Monthly Attendance & Payment
-- Report (one report instead of two, per user request to simplify the
-- flow). The merged report gets its gross figure directly from the
-- employee's Salary Structure, so a separate HRA% setting was never
-- actually applied - keeping it around would just be a config option
-- that silently does nothing, which is its own source of confusion.
-- ============================================================

ALTER TABLE payroll_settings DROP COLUMN hra_percent;


-- ============================================================
-- Source: V61__add_reset_annually_to_paid_leave.sql
-- ============================================================
-- ============================================================
-- V61: Annual carry-forward reset option for Paid Leave
--
-- reset_annually = TRUE means carry-forward only applies within the same
-- calendar year - the balance automatically resets to 0 every January
-- (see EmployeePaidLeaveService.resolveCarryForward). Default FALSE keeps
-- the existing behaviour: carry-forward continues indefinitely across
-- year boundaries.
-- ============================================================

ALTER TABLE paid_leave_configurations
    ADD COLUMN reset_annually BOOLEAN NOT NULL DEFAULT FALSE AFTER maximum_carry_forward;


-- ============================================================
-- Source: V62__create_employee_advances.sql
-- ============================================================
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


-- ============================================================
-- Source: V63__create_advance_recovery_transactions.sql
-- ============================================================
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


-- ============================================================
-- Source: V64__add_employee_pf_esi_pt_applicability.sql
-- ============================================================
-- ============================================================
-- V64: Per-employee PF/ESI/PT applicability
--
-- "Never assume every employee has the same payroll rules" - these three
-- flags let an individual employee opt out of a deduction the tenant's
-- PayrollSettings would otherwise apply (e.g. a fixed-payment employee
-- with PF/ESI/PT all off). Default TRUE preserves existing behaviour for
-- every already-created employee.
-- ============================================================

ALTER TABLE employees
    ADD COLUMN pf_applicable  BOOLEAN NOT NULL DEFAULT TRUE AFTER status,
    ADD COLUMN esi_applicable BOOLEAN NOT NULL DEFAULT TRUE AFTER pf_applicable,
    ADD COLUMN pt_applicable  BOOLEAN NOT NULL DEFAULT TRUE AFTER esi_applicable;


-- ============================================================
-- Source: V65__insert_advance_permissions.sql
-- ============================================================
-- ============================================================
-- V65: Employee Advance permissions
--
-- CLIENT_ADMIN only, same convention as every other payroll-adjacent
-- permission in this system - advances directly affect deductions.
-- SUPER_ADMIN granted per the usual "gets every permission" invariant.
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('ADVANCE_READ',   'View an employee''s advances and recovery history'),
    ('ADVANCE_GRANT',  'Grant a new advance to an employee'),
    ('ADVANCE_UPDATE', 'Change an advance''s monthly recovery amount, or settle it manually');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name IN ('ADVANCE_READ', 'ADVANCE_GRANT', 'ADVANCE_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name IN ('ADVANCE_READ', 'ADVANCE_GRANT', 'ADVANCE_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );


-- ============================================================
-- Source: V66__create_payroll_runs.sql
-- ============================================================
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


-- ============================================================
-- Source: V67__create_payroll_run_employees.sql
-- ============================================================
-- ============================================================
-- V67: Persisted Payroll Run Employee snapshots (architecture refactor Phase 2)
--
-- One row per employee per PayrollRun - a permanent monthly snapshot that
-- never changes just because attendance/leave/salary-structure/PF-ESI-PT
-- settings change afterward. Only re-running calculate() while the parent
-- run is still DRAFT/CALCULATED ever overwrites a row here (enforced at
-- the service layer, not the DB, since MySQL can't express "immutable
-- once parent status is X" as a constraint).
-- ============================================================

CREATE TABLE payroll_run_employees (
    id                                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    payroll_run_id                         BIGINT        NOT NULL,
    employee_id                            BIGINT        NOT NULL,
    employee_code                          VARCHAR(50)   NOT NULL,
    employee_name                          VARCHAR(200)  NOT NULL,
    department                             VARCHAR(150),
    designation                            VARCHAR(150),
    site_name                              VARCHAR(150),
    salary_structure_name                  VARCHAR(150),
    salary_type                            VARCHAR(20),

    total_calendar_days                     INT           NOT NULL,
    present_days                            INT           NOT NULL,
    half_days                               INT           NOT NULL,
    on_leave_days                           INT           NOT NULL,
    absent_days                             INT           NOT NULL,
    paid_leave_days                         DECIMAL(6,2)  NOT NULL DEFAULT 0,
    unpaid_leave_days                       DECIMAL(6,2)  NOT NULL DEFAULT 0,
    payable_days                            DECIMAL(6,2)  NOT NULL DEFAULT 0,
    leave_balance_closing                   DECIMAL(6,2),

    basic_salary                            DECIMAL(12,2) NOT NULL DEFAULT 0,
    da                                      DECIMAL(12,2) NOT NULL DEFAULT 0,
    gross_salary                            DECIMAL(12,2) NOT NULL DEFAULT 0,

    allowance                               DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_earnings                          DECIMAL(12,2) NOT NULL DEFAULT 0,

    epf_employee                            DECIMAL(12,2) NOT NULL DEFAULT 0,
    epf_employer                            DECIMAL(12,2) NOT NULL DEFAULT 0,
    esi_employee                            DECIMAL(12,2) NOT NULL DEFAULT 0,
    esi_employer                            DECIMAL(12,2) NOT NULL DEFAULT 0,
    professional_tax                        DECIMAL(12,2) NOT NULL DEFAULT 0,
    other_manual_deduction                  DECIMAL(12,2) NOT NULL DEFAULT 0,
    advance_recovery                        DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_deductions                        DECIMAL(12,2) NOT NULL DEFAULT 0,

    advance_outstanding_before_recovery      DECIMAL(12,2) NOT NULL DEFAULT 0,
    advance_outstanding_after_recovery       DECIMAL(12,2) NOT NULL DEFAULT 0,

    total_salary_ctc                        DECIMAL(12,2) NOT NULL DEFAULT 0,
    net_pay                                 DECIMAL(12,2) NOT NULL DEFAULT 0,
    note                                    VARCHAR(500),

    created_at                              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_payroll_run_employee_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_payroll_run_employee_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT uq_payroll_run_employee UNIQUE (payroll_run_id, employee_id)
) ENGINE=InnoDB;

CREATE INDEX idx_payroll_run_employee_employee ON payroll_run_employees (employee_id);


-- ============================================================
-- Source: V68__insert_payroll_run_permissions.sql
-- ============================================================
-- ============================================================
-- V68: Payroll Run permissions
--
-- CLIENT_ADMIN only, same convention as every other payroll-adjacent
-- permission in this system. Split into separate CREATE/CALCULATE/READ/
-- APPROVE/PAY/CANCEL permissions (rather than reusing the existing
-- PAYROLL_REGISTER_EXPORT) so a future role could be granted, e.g.,
-- read-only visibility without the ability to approve/pay - not used yet,
-- but this is the natural seam for that later.
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('PAYROLL_RUN_CREATE',   'Create a new payroll run (DRAFT) for a month'),
    ('PAYROLL_RUN_CALCULATE','Calculate/recalculate a payroll run''s employee results'),
    ('PAYROLL_RUN_READ',     'View persisted payroll runs and their employee results'),
    ('PAYROLL_RUN_APPROVE',  'Approve a calculated payroll run'),
    ('PAYROLL_RUN_PAY',      'Mark an approved payroll run as paid'),
    ('PAYROLL_RUN_CANCEL',   'Cancel a draft or calculated payroll run');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name IN ('PAYROLL_RUN_CREATE', 'PAYROLL_RUN_CALCULATE', 'PAYROLL_RUN_READ',
                 'PAYROLL_RUN_APPROVE', 'PAYROLL_RUN_PAY', 'PAYROLL_RUN_CANCEL')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name IN ('PAYROLL_RUN_CREATE', 'PAYROLL_RUN_CALCULATE', 'PAYROLL_RUN_READ',
                 'PAYROLL_RUN_APPROVE', 'PAYROLL_RUN_PAY', 'PAYROLL_RUN_CANCEL')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );


-- ============================================================
-- Source: V69__rename_advance_uniform_deduction.sql
-- ============================================================
-- ============================================================
-- V69: Rename employee_payroll_adjustments.advance_uniform_deduction
--
-- Architecture refactor Phase 5: this column name incorrectly implied it
-- could represent an employee advance. It never did - EmployeeAdvance and
-- advance_recovery_transactions are the ONLY source of truth for actual
-- advances and their recovery (see V62/V63). Renamed so the word "advance"
-- cannot appear in a generic manual-deduction column anywhere in the
-- schema. Existing data is preserved - this is a rename, not a drop/recreate.
-- ============================================================

ALTER TABLE employee_payroll_adjustments
    CHANGE COLUMN advance_uniform_deduction other_manual_deduction DECIMAL(10,2) NOT NULL DEFAULT 0;


-- ============================================================
-- Source: V70__add_advance_recovery_traceability.sql
-- ============================================================
-- ============================================================
-- V70: Advance recovery traceability + manual settlement support
--
-- Architecture refactor Phase 5:
--   payroll_run_id - answers "which payroll recovered this amount?" for
--     PAYROLL-sourced rows (null for manual settlements).
--   source - PAYROLL (created by PayrollCalculationService during a
--     Payroll Run calculation) or MANUAL_SETTLEMENT (employee paid some or
--     all of the outstanding amount outside payroll - see
--     EmployeeAdvanceService.settlePartial()). Existing rows all predate
--     this column and are unambiguously PAYROLL-sourced, so the DEFAULT
--     backfills them correctly with no data migration needed.
-- ============================================================

ALTER TABLE advance_recovery_transactions
    ADD COLUMN payroll_run_id BIGINT AFTER advance_id,
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'PAYROLL' AFTER recovered_amount,
    ADD COLUMN created_by BIGINT AFTER created_at,
    ADD CONSTRAINT fk_advance_recovery_payroll_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs (id) ON DELETE SET NULL;

CREATE INDEX idx_advance_recovery_payroll_run ON advance_recovery_transactions (payroll_run_id);


-- ============================================================
-- Source: V71__fix_advance_recovery_uniqueness.sql
-- ============================================================
-- ============================================================
-- V71: Fix advance_recovery_transactions uniqueness for manual settlements
--
-- The original UNIQUE (advance_id, year, month) from V63 assumed exactly
-- one recovery row could ever exist per advance per month - true when only
-- PAYROLL-sourced rows existed, but V70 introduced MANUAL_SETTLEMENT rows
-- (spec section 17) which can legitimately coexist with a PAYROLL row in
-- the same month (e.g. payroll already recovered its usual amount this
-- month, and the employee separately pays down more of the balance in
-- cash the same month). Without this fix, EmployeeAdvanceService
-- .settlePartial() would throw a duplicate-key error whenever a payroll
-- recovery already existed for that month.
--
-- New constraint allows one row per (advance, month, source) - i.e. at
-- most one PAYROLL row and one MANUAL_SETTLEMENT row per advance per
-- month. Known limitation, accepted for now: two separate manual
-- settlements against the same advance within the same calendar month
-- still collide (the second call updates/replaces the first's amount
-- rather than adding a third row) - documented in
-- EmployeeAdvanceService.settlePartial().
-- ============================================================

ALTER TABLE advance_recovery_transactions
    DROP INDEX uq_advance_recovery_month,
    ADD CONSTRAINT uq_advance_recovery_month_source UNIQUE (advance_id, year, month, source);


-- ============================================================
-- Source: V72__add_bonus_overtime_arrears.sql
-- ============================================================
-- ============================================================
-- V72: Bonus/Overtime/Arrears for manual payroll adjustments
--
-- Architecture refactor Phase 6: PayrollCalculationService's Total
-- Earnings breakdown needs Bonus/Overtime/Arrears as their own explicit
-- figures, kept separate from Other Manual Deduction and Allowance
-- (same narrow, explicit-field design established in Phase 5 - no
-- generic "everything" table).
-- ============================================================

ALTER TABLE employee_payroll_adjustments
    ADD COLUMN bonus    DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER allowance,
    ADD COLUMN overtime DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER bonus,
    ADD COLUMN arrears  DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER overtime;


-- ============================================================
-- Source: V73__add_payroll_run_approval_locking.sql
-- ============================================================
-- ============================================================
-- V73: Payroll Run approval/finalization/locking (architecture refactor Phase 7)
--
-- cancellation_reason - now mandatory when cancelling (spec section 10).
-- reopened_at/reopened_by/reopen_reason - the controlled APPROVED -> CALCULATED
--   reopen path (spec section 11) - PAID payroll is never reopenable through
--   this workflow.
-- version - optimistic locking (spec section 30) so two admins cannot both
--   approve/recalculate/reopen the same run from a stale read; the second
--   save fails with a 409 instead of silently overwriting the first admin's
--   change. No other entity in the project uses @Version yet - introduced
--   here specifically because PayrollRun is where a lost-update race
--   genuinely matters (see GlobalExceptionHandler for the resulting 409).
-- ============================================================

ALTER TABLE payroll_runs
    ADD COLUMN cancellation_reason VARCHAR(500) AFTER cancelled_by,
    ADD COLUMN reopened_at DATETIME AFTER cancellation_reason,
    ADD COLUMN reopened_by BIGINT AFTER reopened_at,
    ADD COLUMN reopen_reason VARCHAR(500) AFTER reopened_by,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER reopen_reason;


-- ============================================================
-- Source: V74__insert_payroll_run_reopen_permission.sql
-- ============================================================
-- ============================================================
-- V74: Payroll Run reopen permission (architecture refactor Phase 7)
--
-- Separate from PAYROLL_RUN_APPROVE/CALCULATE - reopening an already-
-- approved payroll reverses a decision that was already made, so it's
-- gated by its own permission rather than reusing an existing one, per
-- "only authorized payroll administrators can reopen APPROVED payroll."
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('PAYROLL_RUN_REOPEN', 'Reopen an APPROVED payroll run back to CALCULATED for correction');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN'
  AND p.name = 'PAYROLL_RUN_REOPEN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'CLIENT_ADMIN' AND r.client_company_id IS NULL
  AND p.name = 'PAYROLL_RUN_REOPEN'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );


-- ============================================================
-- Source: V75__make_payroll_settings_effective_dated.sql
-- ============================================================
-- ============================================================
-- V75: Refactor PayrollSettings into an effective-dated model (architecture refactor Phase 8)
--
-- Was: one mutable row per tenant, PK = client_company_id - editing it
-- always changed "the" settings, with no way to know what rate applied to
-- a past month. Now: any number of rows per tenant, each with its own
-- effective_from/effective_to window - PayrollSettingsResolver picks the
-- one whose window covers a given payroll month. This is the same
-- entity/table refactored in place (spec section 3 explicitly allows
-- this instead of a separate History table), not a second system.
--
-- Existing rows get effective_from = 2000-01-01 (safely before any real
-- payroll data in this system) and status = ACTIVE, so they continue to
-- apply to every month calculated so far - no historical payroll's
-- applicable configuration changes as a result of this migration.
-- ============================================================

ALTER TABLE payroll_settings
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ADD COLUMN effective_from DATE NOT NULL DEFAULT '2000-01-01',
    ADD COLUMN effective_to DATE NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by BIGINT,
    ADD INDEX idx_payroll_settings_company_effective (client_company_id, effective_from);

UPDATE payroll_settings SET created_at = updated_at, created_by = updated_by WHERE created_by IS NULL;


-- ============================================================
-- Source: V76__add_payroll_rate_snapshot.sql
-- ============================================================
-- ============================================================
-- V76: Payroll configuration snapshot on PayrollRunEmployee (architecture refactor Phase 8)
--
-- Stores the ACTUAL rates used for this employee's calculation, not just
-- the resulting amounts - so a payslip/audit view can explain "12% of
-- 15,000 = 1,800" for a payroll from months ago even after the tenant's
-- current PF rate has since changed to something else. Nullable because
-- a rate is meaningless when its deduction wasn't applicable that month
-- (e.g. epf_employee_percent_used is null when the employee had PF off).
-- ============================================================

ALTER TABLE payroll_run_employees
    ADD COLUMN epf_employee_percent_used DECIMAL(5,2) AFTER epf_employer,
    ADD COLUMN epf_employer_percent_used DECIMAL(5,2) AFTER epf_employee_percent_used,
    ADD COLUMN esi_employee_percent_used DECIMAL(5,2) AFTER esi_employer,
    ADD COLUMN esi_employer_percent_used DECIMAL(5,2) AFTER esi_employee_percent_used;


-- ============================================================
-- Source: V77__make_paid_leave_config_effective_dated.sql
-- ============================================================
-- ============================================================
-- V77: Refactor PaidLeaveConfiguration into an effective-dated model (architecture refactor Phase 9)
--
-- Was: one mutable row per tenant, PK = client_company_id - editing it
-- always changed "the" policy, with no way to know what policy applied to
-- a past month (the exact bug the original architecture audit flagged:
-- resolveCarryForward() always read "today's" config). Now: any number of
-- rows per tenant, each with its own effective_from/effective_to window -
-- LeavePolicyResolver picks the one whose window covers a given month.
-- Refactored in place (same pattern as V75 for PayrollSettings), not a
-- separate LeavePolicyHistory table.
--
-- Existing rows get effective_from = 2000-01-01 (safely before any real
-- leave data in this system) and status = ACTIVE, so they continue to
-- apply to every month already resolved - no historical leave balance's
-- applicable policy changes as a result of this migration.
-- ============================================================

ALTER TABLE paid_leave_configurations
    DROP PRIMARY KEY,
    ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ADD COLUMN effective_from DATE NOT NULL DEFAULT '2000-01-01',
    ADD COLUMN effective_to DATE NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD INDEX idx_paid_leave_config_company_effective (client_company_id, effective_from);

UPDATE paid_leave_configurations SET created_at = updated_at WHERE updated_at IS NOT NULL;


-- ============================================================
-- Source: V79__add_advance_recovery_start_month.sql
-- ============================================================
-- ============================================================
-- V79: Advance Recovery Start Month + Remarks
--
-- Genuine gap found during the Phase 1-9 completion audit: there was no
-- way to say "this advance shouldn't start being recovered until a future
-- payroll month" - EmployeeAdvanceService.computeMonthlyRecovery() would
-- try to recover from an advance the very first month it existed. Adds
-- recovery_start_year/recovery_start_month (backfilled from advance_date
-- for existing rows, so their behavior is unchanged - they already
-- started recovering the month they were granted) and a separate
-- free-text remarks column (distinct from the existing short "reason").
-- ============================================================

ALTER TABLE employee_advances
    ADD COLUMN recovery_start_year INT,
    ADD COLUMN recovery_start_month INT,
    ADD COLUMN remarks VARCHAR(500);

UPDATE employee_advances
SET recovery_start_year = YEAR(advance_date),
    recovery_start_month = MONTH(advance_date)
WHERE recovery_start_year IS NULL;

ALTER TABLE employee_advances
    MODIFY COLUMN recovery_start_year INT NOT NULL,
    MODIFY COLUMN recovery_start_month INT NOT NULL;


-- ============================================================
-- Source: V80__allow_multiple_manual_settlements.sql
-- ============================================================
-- ============================================================
-- V80: Allow multiple MANUAL_SETTLEMENT entries per advance per month
--
-- V71's UNIQUE (advance_id, year, month, source) constraint meant a
-- second manual settlement against the same advance in the same calendar
-- month would silently merge into the first row's amount rather than
-- becoming its own entry - so paying an advance down twice in one day
-- only ever showed up as one history line. That was a known, documented
-- limitation; this closes it by simply removing the composite constraint.
--
-- PAYROLL idempotency (never double-recovering the same advance+month) is
-- enforced at the APPLICATION level instead, in EmployeeAdvanceService
-- .computeMonthlyRecovery(): it looks up any existing PAYROLL row for
-- that (advance, year, month) via findByAdvanceIdAndYearAndMonthAndSource()
-- and updates it in place rather than inserting a second one - see that
-- method for the exact find-or-create logic. An earlier version of this
-- migration also tried to enforce that same guarantee at the database
-- level via a generated column + a second unique index, but that
-- triggered a MySQL error (1215, "Cannot add foreign key constraint")
-- on some MySQL 8.0 builds when combining a STORED generated column with
-- a table that already has foreign keys - not worth the fragility for a
-- belt-and-suspenders guarantee the application layer already provides.
--
-- DEFENSIVE / IDEMPOTENT: every step checks information_schema first, so
-- this migration is safe to re-run on a database where an earlier
-- attempt partially applied.
-- ============================================================

-- Step 0: the fk_advance_recovery_advance foreign key on advance_id has never had its own
-- index - it has always relied on uq_advance_recovery_month_source (advance_id is its leftmost
-- column) as its supporting index, since V63 never created a standalone one. MySQL/InnoDB
-- refuses to drop an index still needed by a foreign key, so a plain index on advance_id alone
-- must exist FIRST, before the old composite unique constraint can be dropped in Step 1.
SET @plain_idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'advance_recovery_transactions'
      AND INDEX_NAME = 'idx_advance_recovery_advance_id'
);
SET @sql = IF(@plain_idx_exists = 0,
    'CREATE INDEX idx_advance_recovery_advance_id ON advance_recovery_transactions (advance_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 1: drop the old constraint, only if it still exists. Nothing replaces it - see the
-- header comment above for why the application layer is responsible for PAYROLL idempotency.
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'advance_recovery_transactions'
      AND INDEX_NAME = 'uq_advance_recovery_month_source'
);
SET @sql = IF(@idx_exists > 0,
    'ALTER TABLE advance_recovery_transactions DROP INDEX uq_advance_recovery_month_source',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 2 (cleanup, not strictly required once the constraint above is gone, but harmless):
-- merge any pre-existing duplicate PAYROLL rows for the same (advance, year, month) that may
-- already exist from earlier testing, so history views don't show stale duplicate entries.
UPDATE advance_recovery_transactions t
JOIN (
    SELECT advance_id, year, month, MIN(id) AS keep_id, SUM(recovered_amount) AS total_amount
    FROM advance_recovery_transactions
    WHERE source = 'PAYROLL'
    GROUP BY advance_id, year, month
    HAVING COUNT(*) > 1
) dupes ON t.id = dupes.keep_id
SET t.recovered_amount = dupes.total_amount;

DELETE t FROM advance_recovery_transactions t
JOIN (
    SELECT advance_id, year, month, MIN(id) AS keep_id
    FROM advance_recovery_transactions
    WHERE source = 'PAYROLL'
    GROUP BY advance_id, year, month
    HAVING COUNT(*) > 1
) dupes ON t.advance_id = dupes.advance_id AND t.year = dupes.year AND t.month = dupes.month
    AND t.source = 'PAYROLL' AND t.id <> dupes.keep_id;


-- ============================================================
-- Source: V81__add_advance_recover_via_payroll_flag.sql
-- ============================================================
-- ============================================================
-- V81: Pause/resume payroll-based recovery per advance
--
-- Lets an admin turn payroll recovery OFF for a specific advance without
-- touching its monthlyRecoveryAmount or status - e.g. the employee already
-- paid this month's installment in cash (via Settle Partial), so payroll
-- should skip it this run; admin turns it back ON before next month's run.
-- EmployeeAdvanceService.computeMonthlyRecovery() simply skips any
-- advance with recover_via_payroll = FALSE. Manual settlement (Settle
-- Partial/Full) is completely unaffected either way - it was always
-- independent of payroll and still is.
-- ============================================================

ALTER TABLE employee_advances
    ADD COLUMN recover_via_payroll BOOLEAN NOT NULL DEFAULT TRUE;


-- ============================================================
-- Source: V82__add_paid_leave_enabled_flag.sql
-- ============================================================
-- ============================================================
-- V82: Master enable/disable switch for Paid Leave
--
-- Some clients don't want to grant any monthly paid leave at all - rather
-- than admins having to remember "set Monthly Paid Leave to 0" (easy to
-- forget, and reads as a number to configure rather than a deliberate
-- policy decision), this adds an explicit "Paid Leave Enabled" switch,
-- effective-dated exactly like every other setting on this table.
--
-- When disabled for a given month, LeavePolicyResolver treats that
-- month's monthly entitlement as 0 - already-carried-forward balance from
-- an earlier ACTIVE period is untouched and can still be used, but no NEW
-- leave accrues while disabled.
-- ============================================================

ALTER TABLE paid_leave_configurations
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;


-- ============================================================
-- IMPORTANT - Flyway bookkeeping after running this file manually
--
-- The backend uses Flyway (baseline-on-migrate: true in application.yml)
-- to track which migrations have been applied, via its own internal
-- flyway_schema_history table - which this file does NOT create, since
-- it's just the raw table/data statements copied out of each migration.
--
-- Before starting the backend for the first time against a database set
-- up with this file, set ONE extra Spring property so Flyway knows
-- "everything through V82 is already done, don't try to re-run it":
--
--   SPRING_FLYWAY_BASELINE_VERSION=82
--   (or add spring.flyway.baseline-version: 82 under the flyway: section
--   in application.yml / application-prod.yml)
--
-- Without this, Flyway's default baseline-version (1) means it will try
-- to re-run V2 onward on its very first startup and fail immediately,
-- since those tables/columns already exist from this script.
--
-- This baseline-version setting only matters for THIS ONE first startup
-- against a database seeded this way - once flyway_schema_history exists,
-- you can remove it again (or just leave it; it's harmless afterward).
-- ============================================================
