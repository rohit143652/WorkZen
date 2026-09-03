import { AuthenticatedUser } from '../../core/models/user.model';

export interface LoginResponse {
  success: boolean;
  message: string;
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthenticatedUser;
  /** Only present/used by the packaged native app - see AuthService.login() for why. Web ignores this entirely. */
  refreshToken?: string;
}
