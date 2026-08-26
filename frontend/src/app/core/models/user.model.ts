export interface AuthenticatedUser {
  id: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  roles: string[];
  permissions: string[];
  mustChangePassword?: boolean;
}
