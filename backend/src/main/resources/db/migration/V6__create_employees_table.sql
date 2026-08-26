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
