-- ============================================================
-- V78: Dummy data for the "Salary Register" report layout
--
-- Reuses existing sample tenant CLI0001 and employees EMP0001-EMP0007
-- (already seeded, already have attendance history from V49/V59). Reuses
-- the existing EARNING salary components (BASIC/DA/HRA) from V42/V60 -
-- only their per-structure amount differs here, no new component
-- definitions - and does NOT add any DEDUCTION components, per the
-- architecture refactor Phase 3 rule that Salary Structure represents
-- Gross Earnings only (PF/ESI/PT come from Payroll Settings).
--
-- Basic 10,021 + DA 3,900 + HRA 696 = Gross 14,617/month, matching the
-- sample "Salary Register" layout the client shared: HRA here is a FIXED
-- structure component (696 ~= 5% of Basic+DA), not a live 5% formula -
-- see payroll_module's SalaryRegisterExportService for how the export
-- derives an "HRA" column for display from Gross - Basic - DA, so the
-- displayed figure always matches whatever the structure actually adds
-- up to, even if this fixed amount is edited later.
--
-- structure_code is computed dynamically (next free SSxxxx for the
-- tenant) rather than hardcoded, since a real environment's own normal
-- usage (the auto-code-generation "Add Salary Structure" form) may
-- already have claimed any fixed code by the time this runs. Guarded by
-- structure_name so re-running a fresh copy of this migration is a no-op
-- if it already succeeded once.
-- ============================================================

INSERT INTO salary_structures (client_company_id, structure_code, structure_name, description, effective_from, status)
SELECT c.id,
       CONCAT('SS', LPAD(COALESCE(MAX(CAST(SUBSTRING(existing.structure_code, 3) AS UNSIGNED)), 0) + 1, 4, '0')),
       'Sample Salary Register Structure',
       'Dummy data matching the sample salary register report layout',
       '2026-01-01', 'ACTIVE'
FROM client_companies c
LEFT JOIN salary_structures existing
       ON existing.client_company_id = c.id AND existing.structure_code REGEXP '^SS[0-9]+$'
WHERE c.company_code = 'CLI0001'
  AND NOT EXISTS (
      SELECT 1 FROM salary_structures s2
      WHERE s2.client_company_id = c.id AND s2.structure_name = 'Sample Salary Register Structure'
  )
GROUP BY c.id;

INSERT INTO salary_structure_components (salary_structure_id, salary_component_id, calculation_type, amount, percentage, is_active, display_order)
SELECT ss.id, sc.id, 'FIXED', vals.amt, NULL, TRUE, vals.ord
FROM salary_structures ss
JOIN client_companies c ON c.id = ss.client_company_id AND c.company_code = 'CLI0001'
JOIN (
    SELECT 'BASIC' AS code, 10021 AS amt, 1 AS ord
    UNION ALL SELECT 'DA', 3900, 2
    UNION ALL SELECT 'HRA', 696, 3
) vals ON 1 = 1
JOIN salary_components sc ON sc.client_company_id = c.id AND sc.component_code = vals.code
WHERE ss.structure_name = 'Sample Salary Register Structure'
  AND NOT EXISTS (
      SELECT 1 FROM salary_structure_components existing_ssc
      WHERE existing_ssc.salary_structure_id = ss.id AND existing_ssc.salary_component_id = sc.id
  );

-- End each of these 7 employees' current structure assignment (if any) the day before
-- the new one starts - same "never overwrite, always end and start a new row" pattern
-- EmployeeSalaryStructureService itself uses (see V39's own comment). Guarded so it
-- only touches an assignment that isn't already pointing at our sample structure.
UPDATE employee_salary_structures esa
JOIN employees e ON e.id = esa.employee_id
JOIN client_companies c ON c.id = e.client_company_id AND c.company_code = 'CLI0001'
JOIN salary_structures ss ON ss.client_company_id = c.id AND ss.structure_name = 'Sample Salary Register Structure'
SET esa.status = 'ENDED', esa.effective_to = '2025-12-31'
WHERE e.employee_code IN ('EMP0001', 'EMP0002', 'EMP0003', 'EMP0004', 'EMP0005', 'EMP0006', 'EMP0007')
  AND esa.status = 'ACTIVE'
  AND esa.salary_structure_id <> ss.id;

INSERT INTO employee_salary_structures (client_company_id, employee_id, salary_structure_id, effective_from, status)
SELECT c.id, e.id, ss.id, '2026-01-01', 'ACTIVE'
FROM client_companies c
JOIN employees e ON e.client_company_id = c.id
    AND e.employee_code IN ('EMP0001', 'EMP0002', 'EMP0003', 'EMP0004', 'EMP0005', 'EMP0006', 'EMP0007')
JOIN salary_structures ss ON ss.client_company_id = c.id AND ss.structure_name = 'Sample Salary Register Structure'
WHERE c.company_code = 'CLI0001'
  AND NOT EXISTS (
      SELECT 1 FROM employee_salary_structures existing_esa
      WHERE existing_esa.employee_id = e.id AND existing_esa.salary_structure_id = ss.id AND existing_esa.status = 'ACTIVE'
  );
