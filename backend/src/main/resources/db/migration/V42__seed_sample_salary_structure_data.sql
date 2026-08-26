-- ============================================================
-- V42: Sample Salary Components + Structures for CLI0001
--
-- Matches spec section 7's exact worked example: a "Housekeeping Staff
-- Grade A" structure with Basic 15000, HRA 3000, Conveyance 1500, Special
-- Allowance 1000 -> Gross 20500, plus PF at 12% of basic and a small fixed
-- ESI deduction, giving a concrete, non-trivial Net Salary figure that
-- exercises every resolution path in SalaryStructureService.calculate()
-- (FIXED, PERCENTAGE_OF_BASIC, PERCENTAGE_OF_GROSS all appear below).
-- ============================================================

INSERT INTO salary_components (client_company_id, component_code, component_name, component_type, calculation_type,
                                value, percentage, is_taxable, is_active, display_order)
SELECT c.id, comp.code, comp.name, comp.type, comp.calc_type, comp.value, comp.percentage, comp.taxable, TRUE, comp.ord
FROM client_companies c
JOIN (
    SELECT 'BASIC'      AS code, 'Basic Salary'      AS name, 'EARNING'    AS type, 'FIXED'               AS calc_type, 15000 AS value, NULL AS percentage, TRUE  AS taxable, 1 AS ord
    UNION ALL SELECT 'HRA',       'House Rent Allowance', 'EARNING',    'FIXED',               3000,  NULL, TRUE,  2
    UNION ALL SELECT 'CONVEYANCE','Conveyance Allowance',  'EARNING',    'FIXED',               1500,  NULL, FALSE, 3
    UNION ALL SELECT 'SPECIAL_ALLOWANCE', 'Special Allowance', 'EARNING', 'FIXED',              1000,  NULL, TRUE,  4
    UNION ALL SELECT 'PF',        'Provident Fund',       'DEDUCTION',  'PERCENTAGE_OF_BASIC', NULL,  12.00, FALSE, 5
    UNION ALL SELECT 'ESI',       'Employee State Insurance', 'DEDUCTION', 'PERCENTAGE_OF_GROSS', NULL, 0.75, FALSE, 6
    UNION ALL SELECT 'PT',        'Professional Tax',      'DEDUCTION',  'FIXED',               200,   NULL, FALSE, 7
) comp ON 1 = 1
WHERE c.company_code = 'CLI0001';

INSERT INTO salary_structures (client_company_id, structure_code, structure_name, description, effective_from, status)
SELECT c.id, 'SS0001', 'Housekeeping Staff Grade A', 'Standard structure for housekeeping staff', '2025-01-01', 'ACTIVE'
FROM client_companies c WHERE c.company_code = 'CLI0001';

INSERT INTO salary_structure_components (salary_structure_id, salary_component_id, calculation_type, amount, percentage, is_active, display_order)
SELECT ss.id, sc.id, sc.calculation_type, sc.value, sc.percentage, TRUE, sc.display_order
FROM salary_structures ss
JOIN client_companies c ON c.id = ss.client_company_id AND c.company_code = 'CLI0001'
JOIN salary_components sc ON sc.client_company_id = c.id
WHERE ss.structure_code = 'SS0001';

-- Demonstrates the full employee-assignment flow: EMP0002 (Amit Sharma) gets
-- this structure effective from their joining date.
INSERT INTO employee_salary_structures (client_company_id, employee_id, salary_structure_id, effective_from, status)
SELECT c.id, e.id, ss.id, e.joining_date, 'ACTIVE'
FROM client_companies c
JOIN employees e ON e.client_company_id = c.id AND e.employee_code = 'EMP0002'
JOIN salary_structures ss ON ss.client_company_id = c.id AND ss.structure_code = 'SS0001'
WHERE c.company_code = 'CLI0001';
