import { CalculationType } from './salary-component.model';

export interface SalaryStructureComponentRequest {
  salaryComponentId: number;
  calculationType: CalculationType;
  amount?: number | null;
  percentage?: number | null;
  displayOrder: number;
}

export interface SalaryStructureComponentResponse {
  id: number;
  salaryComponentId: number;
  componentCode: string;
  componentName: string;
  componentType: string;
  calculationType: CalculationType;
  amount?: number | null;
  percentage?: number | null;
  /** The actual computed amount for this line item within this structure. */
  resolvedAmount: number;
  displayOrder: number;
}

export type SalaryType = 'MONTHLY' | 'DAILY' | 'HOURLY' | 'CONTRACT';

export interface SalaryStructureRequest {
  structureCode?: string;
  structureName: string;
  salaryType: SalaryType;
  description?: string;
  dailyRate?: number | null;
  hourlyRate?: number | null;
  effectiveFrom: string;
  effectiveTo?: string | null;
  components: SalaryStructureComponentRequest[];
}

export interface SalaryStructureResponse {
  id: number;
  structureCode: string;
  structureName: string;
  salaryType: SalaryType;
  description?: string;
  dailyRate?: number | null;
  hourlyRate?: number | null;
  effectiveFrom: string;
  effectiveTo?: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  components: SalaryStructureComponentResponse[];
  grossEarnings: number;
  employeeCount: number;
  createdAt: string;
  updatedAt: string;
}
