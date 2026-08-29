export interface EmployeeExitResponse {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  resignationDate: string;
  lastWorkingDay: string;
  noticePeriodDays: number;
  reason?: string;
  status: 'INITIATED' | 'SETTLED';
  proratedSalary?: number;
  outstandingAdvanceDeduction?: number;
  netSettlementAmount?: number;
  settledAt?: string;
}

export interface EmployeeExitRequest {
  employeeId: number;
  resignationDate: string;
  lastWorkingDay: string;
  reason?: string;
}
