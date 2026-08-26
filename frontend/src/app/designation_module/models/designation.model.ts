export interface DesignationRequest {
  name: string;
}

export interface DesignationResponse {
  id: number;
  name: string;
  status: 'ACTIVE' | 'INACTIVE';
  employeeCount: number;
}
