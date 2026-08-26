export type PayrollRunStatus = 'DRAFT' | 'CALCULATED' | 'APPROVED' | 'PAID' | 'CANCELLED';

export interface PayrollRunCreateRequest {
  year: number;
  month: number;
  remarks?: string;
}

export interface PayrollRunSummary {
  totalEmployees: number;
  totalGross: number;
  totalEarnings: number;
  totalEpf: number;
  totalEsi: number;
  totalPt: number;
  totalOtherDeduction: number;
  totalAdvanceRecovery: number;
  totalDeductions: number;
  totalNetPay: number;
}

export interface PayrollRun {
  id: number;
  year: number;
  month: number;
  monthLabel: string;
  status: PayrollRunStatus;
  remarks: string | null;
  createdAt: string;
  createdBy: string | null;
  calculatedAt: string | null;
  calculatedBy: string | null;
  approvedAt: string | null;
  approvedBy: string | null;
  paidAt: string | null;
  paidBy: string | null;
  cancelledAt: string | null;
  cancelledBy: string | null;
  cancellationReason: string | null;
  reopenedAt: string | null;
  reopenedBy: string | null;
  reopenReason: string | null;
  summary: PayrollRunSummary;
}

export interface PayrollRunEmployeeResult {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  department: string | null;
  designation: string | null;
  siteName: string | null;
  salaryStructureName: string | null;
  salaryType: string | null;
  totalCalendarDays: number;
  presentDays: number;
  halfDays: number;
  onLeaveDays: number;
  absentDays: number;
  paidLeaveDays: number;
  unpaidLeaveDays: number;
  payableDays: number;
  leaveBalanceClosing: number | null;
  basicSalary: number;
  da: number;
  grossSalary: number;
  allowance: number;
  totalEarnings: number;
  epfEmployee: number;
  epfEmployer: number;
  esiEmployee: number;
  esiEmployer: number;
  professionalTax: number;
  otherManualDeduction: number;
  advanceRecovery: number;
  totalDeductions: number;
  advanceOutstandingBeforeRecovery: number;
  advanceOutstandingAfterRecovery: number;
  totalSalaryCtc: number;
  netPay: number;
  note: string | null;
}
