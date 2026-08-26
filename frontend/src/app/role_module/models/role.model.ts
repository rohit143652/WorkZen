export interface RoleOption {
  id: number;
  name: string;
  description?: string;
  permissions?: string[];
  /** True if this is a tenant-created custom role rather than a global/house system role. */
  custom?: boolean;
}
