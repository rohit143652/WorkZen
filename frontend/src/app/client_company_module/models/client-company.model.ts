import { ClientSubscriptionRequest } from '../../subscription_module/models/subscription.model';

export interface ClientAdminLoginRequest {
  username: string;
  password: string;
}

export interface FeatureCategory {
  label: string;
  codes: string[];
}

export interface FeatureEffectiveStatus {
  featureCode: string;
  category: string;
  planDefault: boolean;
  clientOverride: boolean | null;
  effective: boolean;
}

export interface CompanyFeatureResponse {
  features: Record<string, boolean>;
  categories: FeatureCategory[];
  enforcedCodes: string[];
  /** Plan default / client override (null = no override) / effective, per feature code - spec section 16. */
  details: FeatureEffectiveStatus[];
}

export interface UpdateCompanyFeaturesRequest {
  features: Record<string, boolean>;
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
  /** Required when CREATING a client - omitted entirely when updating (the backend never reads this field on update). */
  subscription?: ClientSubscriptionRequest;
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
