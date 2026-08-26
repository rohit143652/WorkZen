-- ============================================================
-- V17: Sample multi-tenant data for local development / demos
--
-- Creates:
--   1 Client Company   : ABC Facility Management (CLI0001)
--   1 Client Admin user: client_admin / admin123 (must_change_password = true)
--   5 Sub-Clients       : SUB0001 - SUB0005
--   5 Sites (1 per sub) : SITE0001 - SITE0005
--  10 Employees          : EMP0001 - EMP0010 (no login accounts - Employee
--                          Management can enable login for any of them later)
--
-- This mirrors exactly the "auto-generated code" scheme the application
-- uses at runtime (CodeGeneratorService), so these rows sit naturally
-- ahead of whatever the UI creates next (the 11th employee created via
-- the UI will correctly become EMP0011, etc).
-- ============================================================

INSERT INTO client_companies (company_code, company_name, legal_name, email, phone, address, city, state, country, pincode,
                               contact_person_name, contact_person_email, contact_person_phone, status)
VALUES ('CLI0001', 'ABC Facility Management', 'ABC Facility Management Pvt Ltd', 'contact@abcfm.example',
        '+91-9000000001', '12 MG Road', 'Pune', 'Maharashtra', 'India', '411001',
        'Suresh Kulkarni', 'suresh.kulkarni@abcfm.example', '+91-9000000002', 'ACTIVE');

-- Client Admin login for CLI0001. Password: admin123 (same verified BCrypt hash used for super_admin).
-- must_change_password = true, matching the same "admin-issued temporary password" pattern used
-- everywhere else in the app (see EmployeeService.createLoginUser).
INSERT INTO users (client_company_id, username, email, password, first_name, is_active, is_locked, must_change_password, password_changed_at)
SELECT c.id, 'client_admin', 'client_admin@abcfm.example',
       '$2b$12$Vg5B2vmiI7t1Mr31z1DrhufpruExhhDE3GZCvORNyh0IZmxfeWIvS',
       'ABC Facility Management', TRUE, FALSE, TRUE, CURRENT_TIMESTAMP
FROM client_companies c WHERE c.company_code = 'CLI0001';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'client_admin' AND r.name = 'CLIENT_ADMIN';

-- Sub-clients
INSERT INTO sub_clients (client_company_id, sub_client_code, name, description, contact_person_name, contact_person_email,
                          contact_person_phone, city, state, country, status)
SELECT c.id, sub.code, sub.name, sub.description, sub.contact_name, sub.contact_email, sub.contact_phone,
       sub.city, sub.state, 'India', 'ACTIVE'
FROM client_companies c
JOIN (
    SELECT 'SUB0001' AS code, 'XYZ IT Park'   AS name, 'IT park campus facility contract' AS description, 'Anil Mehta'  AS contact_name, 'anil.mehta@xyzitpark.example'  AS contact_email, '+91-9100000001' AS contact_phone, 'Pune'    AS city, 'Maharashtra' AS state
    UNION ALL SELECT 'SUB0002', 'ABC Mall',      'Retail mall housekeeping & security',    'Priya Nair',   'priya.nair@abcmall.example',     '+91-9100000002', 'Mumbai',  'Maharashtra'
    UNION ALL SELECT 'SUB0003', 'DEF Hospital',  'Hospital facility management',            'Ramesh Iyer',  'ramesh.iyer@defhospital.example', '+91-9100000003', 'Pune',    'Maharashtra'
    UNION ALL SELECT 'SUB0004', 'PQR Tower',     'Commercial office tower',                  'Sneha Rao',    'sneha.rao@pqrtower.example',     '+91-9100000004', 'Bengaluru', 'Karnataka'
    UNION ALL SELECT 'SUB0005', 'LMN School',    'Educational campus housekeeping',          'Vikram Joshi', 'vikram.joshi@lmnschool.example', '+91-9100000005', 'Pune',    'Maharashtra'
) sub ON 1 = 1
WHERE c.company_code = 'CLI0001';

-- One site per sub-client
INSERT INTO sites (client_company_id, sub_client_id, site_code, site_name, description, city, state, country,
                    site_contact_person, site_contact_number, required_employee_count, allow_over_allocation, status)
SELECT c.id, sc.id, site.code, site.name, site.description, sc.city, sc.state, 'India',
       sc.contact_person_name, sc.contact_person_phone, site.required_count, FALSE, 'ACTIVE'
FROM client_companies c
JOIN sub_clients sc ON sc.client_company_id = c.id
JOIN (
    SELECT 'SUB0001' AS sub_code, 'SITE0001' AS code, 'Main Building' AS name, 'Primary site for XYZ IT Park' AS description, 20 AS required_count
    UNION ALL SELECT 'SUB0002', 'SITE0002', 'Ground Floor Retail', 'Main mall floor',            15
    UNION ALL SELECT 'SUB0003', 'SITE0003', 'Hospital Block A',    'Main hospital building',     10
    UNION ALL SELECT 'SUB0004', 'SITE0004', 'Tower Reception',     'Ground floor reception/lobby', 8
    UNION ALL SELECT 'SUB0005', 'SITE0005', 'School Main Campus',  'Primary school campus',        5
) site ON site.sub_code = sc.sub_client_code
WHERE c.company_code = 'CLI0001';

-- 10 employees, no login accounts (Employee Management can enable login for any of them from the UI)
INSERT INTO employees (client_company_id, employee_code, first_name, last_name, email, mobile_number,
                        joining_date, department, designation, employment_type, city, state, country, status)
SELECT c.id, emp.code, emp.first_name, emp.last_name, emp.email, emp.mobile,
       emp.joining_date, emp.department, emp.designation, 'FULL_TIME', 'Pune', 'Maharashtra', 'India', 'ACTIVE'
FROM client_companies c
JOIN (
    SELECT 'EMP0001' AS code, 'Rohit'   AS first_name, 'Patil'    AS last_name, 'rohit.patil@abcfm.example'   AS email, '+91-9200000001' AS mobile, '2025-01-15' AS joining_date, 'Operations'   AS department, 'Supervisor'        AS designation
    UNION ALL SELECT 'EMP0002', 'Amit',    'Sharma',   'amit.sharma@abcfm.example',   '+91-9200000002', '2025-02-01', 'Operations',    'Housekeeping Staff'
    UNION ALL SELECT 'EMP0003', 'Suresh',  'Deshmukh', 'suresh.deshmukh@abcfm.example', '+91-9200000003', '2025-02-10', 'Security',      'Security Guard'
    UNION ALL SELECT 'EMP0004', 'Prakash', 'Jadhav',   'prakash.jadhav@abcfm.example', '+91-9200000004', '2025-02-15', 'Operations',    'Housekeeping Staff'
    UNION ALL SELECT 'EMP0005', 'Vijay',   'Kadam',    'vijay.kadam@abcfm.example',    '+91-9200000005', '2025-03-01', 'Maintenance',   'Technician'
    UNION ALL SELECT 'EMP0006', 'Sunita',  'More',     'sunita.more@abcfm.example',    '+91-9200000006', '2025-03-05', 'Housekeeping',  'Housekeeping Staff'
    UNION ALL SELECT 'EMP0007', 'Anita',   'Gaikwad',  'anita.gaikwad@abcfm.example',  '+91-9200000007', '2025-03-10', 'Housekeeping',  'Team Lead'
    UNION ALL SELECT 'EMP0008', 'Ganesh',  'Pawar',    'ganesh.pawar@abcfm.example',   '+91-9200000008', '2025-03-15', 'Security',      'Security Guard'
    UNION ALL SELECT 'EMP0009', 'Kiran',   'Shinde',   'kiran.shinde@abcfm.example',   '+91-9200000009', '2025-04-01', 'Maintenance',   'Electrician'
    UNION ALL SELECT 'EMP0010', 'Meena',   'Bhosale',  'meena.bhosale@abcfm.example',  '+91-9200000010', '2025-04-05', 'Operations',    'Housekeeping Staff'
) emp ON 1 = 1
WHERE c.company_code = 'CLI0001';
