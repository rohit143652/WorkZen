export interface EmployeeAssignmentRequest {
  employeeId: number;
  siteId: number;
  assignmentType?: string;
  startDate: string;
  primary: boolean;
  remarks?: string;
}

export interface BulkEmployeeAssignmentRequest {
  siteId: number;
  employeeIds: number[];
  startDate: string;
  remarks?: string;
}

export interface TransferEmployeeRequest {
  toSiteId: number | null;
  effectiveDate: string;
  reason?: string;
}

export interface EmployeeAssignmentResponse {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  siteId: number;
  siteName?: string;
  assignmentType: string;
  startDate: string;
  endDate?: string;
  primary: boolean;
  status: 'ACTIVE' | 'ENDED';
  remarks?: string;
  createdAt: string;
}

export interface BulkAssignmentResult {
  requested: number;
  assigned: number;
  rejected: string[];
}

export interface BulkEndAssignmentRequest {
  assignmentIds: number[];
}

export interface BulkEndResult {
  requested: number;
  ended: number;
  failed: string[];
}
