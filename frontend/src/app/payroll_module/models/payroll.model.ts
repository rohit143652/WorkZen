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

/** What daysInMonth actually means for proration (Basic/DA/Gross divided by this, then multiplied by payable days) - see backend PayrollWorkingDaysResolver. */
export type WorkingDaysBasis = 'CALENDAR_DAYS' | 'WORKING_DAYS' | 'FIXED';

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

export const WORKING_DAYS_BASES: { value: WorkingDaysBasis; label: string }[] = [
  { value: 'CALENDAR_DAYS', label: 'Calendar Days (28/29/30/31 - varies by month)' },
  { value: 'WORKING_DAYS', label: 'Working Days (calendar days minus configured weekly offs)' },
  { value: 'FIXED', label: 'Fixed Number of Days (e.g. 26 or 30, every month)' }
];

/** FLAT = one company-wide amount (PayrollSettings.professionalTax). SLAB = looked up from ProfessionalTaxSlab by that month's Gross salary - see ProfessionalTaxSlabService on the backend. */
export type PtCalculationMode = 'FLAT' | 'SLAB';

export const PT_CALCULATION_MODES: { value: PtCalculationMode; label: string }[] = [
  { value: 'FLAT', label: 'Flat Amount (same for everyone)' },
  { value: 'SLAB', label: 'Slab-Based (varies by gross salary band)' }
];

export interface ProfessionalTaxSlab {
  id: number;
  /** Null = applies regardless of state. */
  state: string | null;
  minSalary: number;
  /** Null = unbounded (the top slab). */
  maxSalary: number | null;
  ptAmount: number;
  effectiveFrom: string;
  effectiveTo: string | null;
  status: 'ACTIVE' | 'CANCELLED';
}

export interface ProfessionalTaxSlabRequest {
  state: string | null;
  minSalary: number;
  maxSalary: number | null;
  ptAmount: number;
  effectiveFrom: string;
  effectiveTo: string | null;
}

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
  /** FLAT or SLAB - see PT_CALCULATION_MODES. */
  ptCalculationMode: PtCalculationMode;
  /** What PF is calculated as a percentage OF - client-configured, see PayrollCalculationService.resolveEpfBase() on the backend. */
  pfCalculationBase: PfCalculationBase;
  /** What daysInMonth means for proration - see PayrollWorkingDaysResolver on the backend. */
  workingDaysBasis: WorkingDaysBasis;
  /** Only meaningful when workingDaysBasis is FIXED. */
  fixedWorkingDays: number | null;
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
  ptCalculationMode: PtCalculationMode;
  pfCalculationBase: PfCalculationBase;
  workingDaysBasis: WorkingDaysBasis;
  fixedWorkingDays: number | null;
}
