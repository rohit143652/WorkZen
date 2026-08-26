export interface AssignSalaryStructureRequest {
  salaryStructureId: number;
  effectiveFrom: string;
}

export interface EmployeeSalaryStructureResponse {
  id: number;
  salaryStructureId: number;
  structureCode: string;
  structureName: string;
  salaryType: string;
  effectiveFrom: string;
  effectiveTo?: string | null;
  status: 'ACTIVE' | 'ENDED';
  grossEarnings: number;
  createdAt: string;
}
