/**
 * The Payroll Register itself lives on the Payroll Processing screen now
 * (see payroll_module's PayrollRun/PayrollRunEmployee) - this module owns
 * the EPF/ESI/PT configuration those figures are computed from.
 *
 * Effective-dated (architecture refactor Phase 8): a tenant can have any
 * number of these over time, each covering a date range. id/effectiveFrom/
 * effectiveTo/status are only present on rows read from the API (history,
 * current, for-month) - omit them when submitting a new configuration.
 */
export type PfCalculationBase = 'GROSS' | 'BASIC' | 'BASIC_PLUS_DA';

export interface OvertimeRecord {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  overtimeDate: string;
  hours: number;
  amount: number;
  remarks?: string;
  markedByUsername?: string;
  markedByRole?: string;
  createdAt: string;
  updatedAt: string;
}

export interface OvertimeRecordRequest {
  employeeId: number;
  overtimeDate: string;
  hours: number;
  amount: number;
  remarks?: string;
}

export const PF_CALCULATION_BASES: { value: PfCalculationBase; label: string }[] = [
  { value: 'GROSS', label: 'Gross Salary' },
  { value: 'BASIC', label: 'Basic Salary' },
  { value: 'BASIC_PLUS_DA', label: 'Basic Salary + Dearness Allowance (DA)' }
];

export interface PayrollSettings {
  id?: number;
  effectiveFrom?: string;
  effectiveTo?: string | null;
  status?: 'ACTIVE' | 'CANCELLED';
  epfEnabled: boolean;
  epfEmployeePercent: number;
  epfEmployerPercent: number;
  esiEnabled: boolean;
  esiEmployeePercent: number;
  esiEmployerPercent: number;
  esiWageCeiling: number | null;
  ptEnabled: boolean;
  professionalTax: number;
  /** What PF is calculated as a percentage OF - client-configured, see PayrollCalculationService.resolveEpfBase() on the backend. */
  pfCalculationBase: PfCalculationBase;
}

/** What's submitted to schedule a new configuration - effectiveFrom is required here even though it's optional on the read-side PayrollSettings shape above. */
export interface PayrollSettingsCreateRequest {
  effectiveFrom: string;
  epfEnabled: boolean;
  epfEmployeePercent: number;
  epfEmployerPercent: number;
  esiEnabled: boolean;
  esiEmployeePercent: number;
  esiEmployerPercent: number;
  esiWageCeiling: number | null;
  ptEnabled: boolean;
  professionalTax: number;
  pfCalculationBase: PfCalculationBase;
}
