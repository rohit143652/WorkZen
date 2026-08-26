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
}
