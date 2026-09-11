export type ExitType = 'RESIGNATION' | 'TERMINATION' | 'RETIREMENT' | 'END_OF_CONTRACT' | 'OTHER';

export interface EmployeeExitResponse {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  exitType: ExitType;
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
  exitType: ExitType;
  resignationDate: string;
  lastWorkingDay: string;
  reason?: string;
}
