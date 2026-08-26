export interface ClientAdminLoginRequest {
  username: string;
  password: string;
}

export interface ClientCompanyRequest {
  companyCode: string;
  companyName: string;
  legalName?: string;
  email?: string;
  phone?: string;
  alternatePhone?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  pincode?: string;
  contactPersonName?: string;
  contactPersonEmail?: string;
  contactPersonPhone?: string;
  createClientAdminLogin: boolean;
  clientAdminLogin?: ClientAdminLoginRequest;
}

export interface ClientCompanyResponse {
  id: number;
  companyCode: string;
  companyName: string;
  legalName?: string;
  email?: string;
  phone?: string;
  alternatePhone?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  pincode?: string;
  contactPersonName?: string;
  contactPersonEmail?: string;
  contactPersonPhone?: string;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
  totalEmployees: number;
  totalSites: number;
  hasClientAdminLogin: boolean;
}
