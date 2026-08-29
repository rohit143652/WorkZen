export interface HolidayResponse {
  id: number;
  startDate: string;
  endDate: string;
  name: string;
  description?: string;
  employeesMarkedPresent: number;
}

export interface HolidayRequest {
  startDate: string;
  endDate: string;
  name: string;
  description?: string;
}
