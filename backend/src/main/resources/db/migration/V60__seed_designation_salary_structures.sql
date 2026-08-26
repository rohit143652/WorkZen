-- ============================================================
-- V60: Salary Structure per Designation, assigned to every sample employee
--
-- V42 created one structure (SS0001 "Housekeeping Staff Grade A") and
-- assigned it only to EMP0002. This adds one structure per remaining
-- designation used by the 10 sample employees, and assigns every
-- employee the structure matching their own Designation - so the
-- Monthly Attendance & Payment Report has real salary data for
-- everyone, not just one employee.
--
-- Housekeeping Staff (EMP0002/0004/0006/0010) all reuse the existing
-- SS0001 - it already matches that designation, so it's left untouched
-- rather than duplicated. EMP0002's existing assignment (from V42) is
-- also left untouched; only the other 3 Housekeeping Staff employees get
-- a new assignment row here.
-- ============================================================

-- A Dearness Allowance component didn't exist yet (V42 only created
-- BASIC/HRA/CONVEYANCE/SPECIAL_ALLOWANCE/PF/ESI/PT).
INSERT INTO salary_components (client_company_id, component_code, component_name, component_type, calculation_type,
                                value, percentage, is_taxable, is_active, display_order)
SELECT c.id, 'DA', 'Dearness Allowance', 'EARNING', 'FIXED', 0, NULL, TRUE, TRUE, 8
FROM client_companies c
WHERE c.company_code = 'CLI0001'
  AND NOT EXISTS (
      SELECT 1 FROM salary_components sc WHERE sc.client_company_id = c.id AND sc.component_code = 'DA'
  );

-- One structure per remaining designation (Housekeeping Staff already has SS0001 from V42).
INSERT INTO salary_structures (client_company_id, structure_code, structure_name, description, effective_from, status)
SELECT c.id, def.structure_code, def.structure_name, def.description, '2025-01-01', 'ACTIVE'
FROM client_companies c
JOIN (
    SELECT 'SS0002' AS structure_code, 'Supervisor Grade'         AS structure_name, 'Standard structure for Supervisors'         AS description
    UNION ALL SELECT 'SS0003', 'Security Guard Grade',   'Standard structure for Security Guards'
    UNION ALL SELECT 'SS0004', 'Technician Grade',        'Standard structure for Technicians'
    UNION ALL SELECT 'SS0005', 'Team Lead Grade',         'Standard structure for Team Leads'
    UNION ALL SELECT 'SS0006', 'Electrician Grade',       'Standard structure for Electricians'
) def ON 1 = 1
WHERE c.company_code = 'CLI0001'
  AND NOT EXISTS (
      SELECT 1 FROM salary_structures ss WHERE ss.client_company_id = c.id AND ss.structure_code = def.structure_code
  );

-- Components for each new structure - BASIC/DA/HRA/CONVEYANCE amounts scaled by designation
-- seniority, plus the same PF (12% of Basic)/ESI (0.75% of Gross)/PT (flat 200) deductions
-- every structure in this tenant already uses.
INSERT INTO salary_structure_components (salary_structure_id, salary_component_id, calculation_type, amount, percentage, is_active, display_order)
SELECT ss.id, sc.id, sc.calculation_type, vals.amount, sc.percentage, TRUE, sc.display_order
FROM salary_structures ss
JOIN client_companies c ON c.id = ss.client_company_id AND c.company_code = 'CLI0001'
JOIN salary_components sc ON sc.client_company_id = c.id
JOIN (
    -- One row per (structure_code, component_code) with the amount that applies.
    -- PF/ESI/PT aren't listed here since they're handled by the next INSERT below,
    -- which reuses each component's own default value/percentage instead.
    SELECT 'SS0002' AS structure_code, 'BASIC' AS component_code, 22000 AS amount
    UNION ALL SELECT 'SS0002', 'DA', 2000
    UNION ALL SELECT 'SS0002', 'HRA', 4000
    UNION ALL SELECT 'SS0002', 'CONVEYANCE', 1500
    UNION ALL SELECT 'SS0003', 'BASIC', 16000
    UNION ALL SELECT 'SS0003', 'DA', 1500
    UNION ALL SELECT 'SS0003', 'HRA', 2500
    UNION ALL SELECT 'SS0003', 'CONVEYANCE', 1000
    UNION ALL SELECT 'SS0004', 'BASIC', 18000
    UNION ALL SELECT 'SS0004', 'DA', 1800
    UNION ALL SELECT 'SS0004', 'HRA', 3000
    UNION ALL SELECT 'SS0004', 'CONVEYANCE', 1500
    UNION ALL SELECT 'SS0005', 'BASIC', 20000
    UNION ALL SELECT 'SS0005', 'DA', 2000
    UNION ALL SELECT 'SS0005', 'HRA', 3500
    UNION ALL SELECT 'SS0005', 'CONVEYANCE', 1500
    UNION ALL SELECT 'SS0006', 'BASIC', 19000
    UNION ALL SELECT 'SS0006', 'DA', 1900
    UNION ALL SELECT 'SS0006', 'HRA', 3200
    UNION ALL SELECT 'SS0006', 'CONVEYANCE', 1500
) vals ON vals.structure_code = ss.structure_code AND vals.component_code = sc.component_code
WHERE ss.structure_code IN ('SS0002', 'SS0003', 'SS0004', 'SS0005', 'SS0006')
  AND NOT EXISTS (
      SELECT 1 FROM salary_structure_components existing WHERE existing.salary_structure_id = ss.id AND existing.salary_component_id = sc.id
  );

-- PF/ESI/PT deduction rows for each new structure - same rates as SS0001, using the
-- component's own default value/percentage rather than a designation-specific amount.
INSERT INTO salary_structure_components (salary_structure_id, salary_component_id, calculation_type, amount, percentage, is_active, display_order)
SELECT ss.id, sc.id, sc.calculation_type, sc.value, sc.percentage, TRUE, sc.display_order
FROM salary_structures ss
JOIN client_companies c ON c.id = ss.client_company_id AND c.company_code = 'CLI0001'
JOIN salary_components sc ON sc.client_company_id = c.id AND sc.component_code IN ('PF', 'ESI', 'PT')
WHERE ss.structure_code IN ('SS0002', 'SS0003', 'SS0004', 'SS0005', 'SS0006')
  AND NOT EXISTS (
      SELECT 1 FROM salary_structure_components existing WHERE existing.salary_structure_id = ss.id AND existing.salary_component_id = sc.id
  );

-- Assign every sample employee the structure matching their own Designation. Employees who
-- already have an assignment (EMP0002, from V42) are skipped to avoid an overlapping
-- assignment; every other employee gets exactly one.
INSERT INTO employee_salary_structures (client_company_id, employee_id, salary_structure_id, effective_from, status)
SELECT c.id, e.id, ss.id, e.joining_date, 'ACTIVE'
FROM client_companies c
JOIN employees e ON e.client_company_id = c.id
JOIN (
    SELECT 'EMP0001' AS employee_code, 'SS0002' AS structure_code
    UNION ALL SELECT 'EMP0003', 'SS0003'
    UNION ALL SELECT 'EMP0004', 'SS0001'
    UNION ALL SELECT 'EMP0005', 'SS0004'
    UNION ALL SELECT 'EMP0006', 'SS0001'
    UNION ALL SELECT 'EMP0007', 'SS0005'
    UNION ALL SELECT 'EMP0008', 'SS0003'
    UNION ALL SELECT 'EMP0009', 'SS0006'
    UNION ALL SELECT 'EMP0010', 'SS0001'
) map ON map.employee_code = e.employee_code
JOIN salary_structures ss ON ss.client_company_id = c.id AND ss.structure_code = map.structure_code
WHERE c.company_code = 'CLI0001'
  AND NOT EXISTS (
      SELECT 1 FROM employee_salary_structures existing WHERE existing.employee_id = e.id
  );
