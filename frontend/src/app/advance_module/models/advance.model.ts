export interface AdvanceGrantRequest {
  advanceDate: string;
  amount: number;
  reason?: string;
  paymentMode?: string;
  monthlyRecoveryAmount: number;
  recoveryStartYear?: number;
  recoveryStartMonth?: number;
  remarks?: string;
}

export interface EmployeeAdvanceResponse {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  advanceDate: string;
  amount: number;
  reason: string | null;
  paymentMode: string | null;
  monthlyRecoveryAmount: number;
  recoveryStartYear: number;
  recoveryStartMonth: number;
  remarks: string | null;
  recoverViaPayroll: boolean;
  status: 'ACTIVE' | 'SETTLED' | 'CANCELLED';
  installmentsPaid: number;
  recoveredAmount: number;
  outstandingAmount: number;
  createdAt: string;
  createdBy: string;
}

export interface AdvanceDashboardSummary {
  totalAdvancesCount: number;
  totalAdvancesGiven: number;
  totalRecovered: number;
  totalOutstanding: number;
  currentMonthRecovery: number;
}

export interface AdvancePartialSettlementRequest {
  amount: number;
  remark?: string;
}

/** One recovery event - either PAYROLL-sourced (traceable to exactly one Payroll Run) or a MANUAL_SETTLEMENT paid outside payroll. */
export interface AdvanceRecoveryTransactionResponse {
  id: number;
  year: number;
  month: number;
  recoveredAmount: number;
  source: 'PAYROLL' | 'MANUAL_SETTLEMENT';
  payrollRunId: number | null;
  createdAt: string;
  createdBy: string;
}
