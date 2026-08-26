export interface DepartmentRequest {
  name: string;
}

export interface DepartmentResponse {
  id: number;
  name: string;
  status: 'ACTIVE' | 'INACTIVE';
  employeeCount: number;
}
