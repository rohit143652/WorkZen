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
