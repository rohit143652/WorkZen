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
