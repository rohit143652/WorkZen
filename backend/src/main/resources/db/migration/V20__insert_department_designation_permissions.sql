-- ============================================================
-- V20: Department & Designation permissions
-- ============================================================

INSERT INTO permissions (name, description) VALUES
    ('DEPARTMENT_READ',    'View departments (needed to populate the Employee form dropdown)'),
    ('DEPARTMENT_MANAGE',  'Add, rename, activate, or deactivate departments'),
    ('DESIGNATION_READ',   'View designations (needed to populate the Employee form dropdown)'),
    ('DESIGNATION_MANAGE', 'Add, rename, activate, or deactivate designations');
