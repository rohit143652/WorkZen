export interface SiteRequest {
  siteCode: string;
  siteName: string;
  description?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  pincode?: string;
  siteContactPerson?: string;
  siteContactNumber?: string;
  requiredEmployeeCount: number;
  allowOverAllocation: boolean;
}

export interface SiteResponse {
  id: number;
  siteCode: string;
  siteName: string;
  description?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  pincode?: string;
  siteContactPerson?: string;
  siteContactNumber?: string;
  requiredEmployeeCount: number;
  assignedEmployeeCount: number;
  allowOverAllocation: boolean;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
}
