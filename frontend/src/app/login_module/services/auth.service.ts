import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthStateService } from '../../core/services/auth-state.service';
import { AuthenticatedUser } from '../../core/models/user.model';
import { TokenService } from '../../core/services/token.service';
import { LoginRequest } from '../models/login-request.model';
import { LoginResponse } from '../models/login-response.model';

interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
}

interface RefreshData {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenService = inject(TokenService);
  private readonly authState = inject(AuthStateService);

  private readonly baseUrl = `${environment.apiUrl}/auth`;

  /**
   * Logs in and stores the access token (in memory) + current user
   * (in reactive state). The refresh token is set by the server as an
   * HttpOnly cookie - withCredentials must stay true so the browser
   * accepts and later sends it automatically.
   */
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<ApiEnvelope<LoginResponse>>(`${this.baseUrl}/login`, request, { withCredentials: true })
      .pipe(
        map(envelope => envelope.data),
        tap(data => {
          this.tokenService.setAccessToken(data.accessToken, data.expiresIn);
          this.authState.setUser(data.user);
        })
      );
  }

  logout(): Observable<void> {
    return this.http
      .post<ApiEnvelope<void>>(`${this.baseUrl}/logout`, {}, { withCredentials: true })
      .pipe(
        map(() => void 0),
        tap(() => this.clearLocalSession()),
        // Even if the server call fails, clear local state so the user is
        // never stuck "logged in" on the client with a dead session.
        catchError(() => {
          this.clearLocalSession();
          return of(void 0);
        })
      );
  }

  /**
   * Silently refreshes the access token using the HttpOnly refresh-token
   * cookie. Used on app bootstrap to restore a session after a full page
   * reload, and by the HTTP interceptor when a request gets a 401.
   */
  refresh(): Observable<RefreshData> {
    return this.http
      .post<ApiEnvelope<RefreshData>>(`${this.baseUrl}/refresh`, {}, { withCredentials: true })
      .pipe(
        map(envelope => envelope.data),
        tap(data => this.tokenService.setAccessToken(data.accessToken, data.expiresIn))
      );
  }

  fetchCurrentUser(): Observable<AuthenticatedUser> {
    return this.http.get<ApiEnvelope<AuthenticatedUser>>(`${this.baseUrl}/me`).pipe(
      map(envelope => envelope.data),
      tap(user => this.authState.setUser(user))
    );
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http
      .post<ApiEnvelope<void>>(`${this.baseUrl}/change-password`, { currentPassword, newPassword })
      .pipe(
        map(() => void 0),
        // Backend revokes all refresh tokens on password change, so the
        // frontend must also drop its local session and force re-login.
        tap(() => this.clearLocalSession())
      );
  }

  isAuthenticated(): boolean {
    return this.authState.isAuthenticated();
  }

  hasRole(role: string): boolean {
    return this.authState.hasRole(role);
  }

  hasPermission(permission: string): boolean {
    return this.authState.hasPermission(permission);
  }

  getCurrentUser(): AuthenticatedUser | null {
    return this.authState.currentUser();
  }

  clearLocalSession(): void {
    this.tokenService.clearTokens();
    this.authState.clearUser();
  }
}
