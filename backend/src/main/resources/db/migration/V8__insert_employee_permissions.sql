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
