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
  latitude?: number | null;
  longitude?: number | null;
  geofenceRadiusMeters?: number | null;
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
  latitude?: number | null;
  longitude?: number | null;
  geofenceRadiusMeters?: number | null;
  requiredEmployeeCount: number;
  assignedEmployeeCount: number;
  allowOverAllocation: boolean;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
}
