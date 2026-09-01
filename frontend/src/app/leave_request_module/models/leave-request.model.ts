export interface LeaveRequestResponse {
  id: number;
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  startDate: string;
  endDate: string;
  dayCount: number;
  reason?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  selfRequested: boolean;
  reviewedByName?: string;
  reviewedAt?: string;
  reviewNote?: string;
  createdAt: string;
}

export interface LeaveRequestCreateRequest {
  startDate: string;
  endDate: string;
  reason?: string;
}

export interface LeaveRequestAdminCreateRequest {
  employeeId: number;
  startDate: string;
  endDate: string;
  reason?: string;
}

export interface LeaveRequestReviewRequest {
  reviewNote?: string;
}
