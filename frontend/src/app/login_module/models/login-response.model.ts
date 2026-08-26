import { AuthenticatedUser } from '../../core/models/user.model';

export interface LoginResponse {
  success: boolean;
  message: string;
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthenticatedUser;
}
