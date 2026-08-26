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
