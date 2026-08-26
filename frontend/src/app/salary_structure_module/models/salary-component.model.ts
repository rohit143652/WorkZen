export type ComponentType = 'EARNING' | 'DEDUCTION' | 'EMPLOYER_CONTRIBUTION' | 'REIMBURSEMENT';
export type CalculationType = 'FIXED' | 'PERCENTAGE_OF_BASIC' | 'PERCENTAGE_OF_GROSS' | 'PER_DAY' | 'PER_HOUR' | 'MANUAL';

export const COMPONENT_TYPES: { value: ComponentType; label: string }[] = [
  { value: 'EARNING', label: 'Earning' },
  { value: 'DEDUCTION', label: 'Deduction' },
  { value: 'EMPLOYER_CONTRIBUTION', label: 'Employer Contribution' },
  { value: 'REIMBURSEMENT', label: 'Reimbursement' }
];

/**
 * Architecture refactor Phase 3: Salary Structure represents Gross
 * Earnings only - PF/ESI/PT/Tax and other monthly deductions are
 * configured in Payroll Settings and per-employee applicability instead,
 * never as a Salary Structure component. New components can therefore
 * only be Earnings or Reimbursements; the backend (SalaryComponentService
 * .create()) enforces this too. Existing Deduction/Employer Contribution
 * components (e.g. seeded PF/ESI/PT) are left in place as historical data
 * and still show correctly in read-only views via COMPONENT_TYPES above -
 * this restricted list is only for the "Add Component" form.
 */
export const CREATABLE_COMPONENT_TYPES: { value: ComponentType; label: string }[] = [
  { value: 'EARNING', label: 'Earning' },
  { value: 'REIMBURSEMENT', label: 'Reimbursement' }
];

export const CALCULATION_TYPES: { value: CalculationType; label: string }[] = [
  { value: 'FIXED', label: 'Fixed Amount' },
  { value: 'PERCENTAGE_OF_BASIC', label: '% of Basic' },
  { value: 'PERCENTAGE_OF_GROSS', label: '% of Gross' },
  { value: 'PER_DAY', label: 'Per Day' },
  { value: 'PER_HOUR', label: 'Per Hour' },
  { value: 'MANUAL', label: 'Manual' }
];

export interface SalaryComponentRequest {
  componentCode?: string;
  componentName: string;
  componentType: ComponentType;
  calculationType: CalculationType;
  value?: number | null;
  percentage?: number | null;
  taxable: boolean;
  displayOrder: number;
}

export interface SalaryComponentResponse {
  id: number;
  componentCode: string;
  componentName: string;
  componentType: ComponentType;
  calculationType: CalculationType;
  value?: number | null;
  percentage?: number | null;
  taxable: boolean;
  active: boolean;
  displayOrder: number;
  usageCount: number;
}
