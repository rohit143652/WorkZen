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
