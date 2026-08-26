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
