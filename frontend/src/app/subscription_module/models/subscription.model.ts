export type BillingCycle = 'MONTHLY' | 'YEARLY' | 'CUSTOM';
export type SubscriptionStatus = 'TRIAL' | 'ACTIVE' | 'EXPIRING_SOON' | 'EXPIRED' | 'GRACE_PERIOD' | 'SUSPENDED' | 'CANCELLED';

export const SUBSCRIPTION_STATUSES: SubscriptionStatus[] = [
  'TRIAL', 'ACTIVE', 'EXPIRING_SOON', 'EXPIRED', 'GRACE_PERIOD', 'SUSPENDED', 'CANCELLED'
];

export interface SubscriptionPlan {
  id: number;
  planCode: string;
  planName: string;
  description?: string;
  employeeLimit: number | null;
  customEmployeeLimitAllowed: boolean;
  monthlyPrice: number | null;
  yearlyPrice: number | null;
  active: boolean;
  displayOrder: number;
  featureCodes: string[];
  clientCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface SubscriptionPlanRequest {
  planCode: string;
  planName: string;
  description?: string;
  employeeLimit: number | null;
  customEmployeeLimitAllowed: boolean;
  monthlyPrice: number | null;
  yearlyPrice: number | null;
  displayOrder: number;
  featureCodes: string[];
}

export interface ClientSubscription {
  id: number;
  clientCompanyId: number;
  planId: number;
  planCode: string;
  planName: string;
  billingCycle: BillingCycle;
  startDate: string;
  endDate: string | null;
  daysRemaining: number | null;
  status: SubscriptionStatus;
  employeeLimitOverride: number | null;
  effectiveEmployeeLimit: number | null;
  monthlyPriceOverride: number | null;
  effectiveMonthlyPrice: number | null;
  yearlyPriceOverride: number | null;
  effectiveYearlyPrice: number | null;
  activeEmployeeCount: number;
  notes?: string;
  enabledFeatures: string[];
}

export interface ClientSubscriptionRequest {
  planId: number;
  billingCycle: BillingCycle;
  startDate: string;
  endDate?: string | null;
  status: SubscriptionStatus;
  employeeLimitOverride?: number | null;
  monthlyPriceOverride?: number | null;
  yearlyPriceOverride?: number | null;
  notes?: string;
  reason?: string;
}

export interface SubscriptionHistoryEntry {
  id: number;
  previousPlanName?: string;
  newPlanName?: string;
  previousBillingCycle?: string;
  newBillingCycle?: string;
  previousEmployeeLimit?: number;
  newEmployeeLimit?: number;
  previousStatus?: string;
  newStatus?: string;
  changeDate: string;
  reason?: string;
  changedByUsername?: string;
}

/** Plan default / client override (null = no override) / effective, per feature code - matches backend FeatureAccessService.FeatureEffectiveStatus. */
export interface FeatureEffectiveStatus {
  featureCode: string;
  category: string;
  planDefault: boolean;
  clientOverride: boolean | null;
  effective: boolean;
}

export interface ExpiringSubscription {
  clientCompanyId: number;
  companyName: string;
  planName: string;
  endDate: string;
  daysRemaining: number;
  status: SubscriptionStatus;
}

export interface SuperAdminDashboardSummary {
  totalCompanies: number;
  activeCompanies: number;
  expiringSoon: ExpiringSubscription[];
}
