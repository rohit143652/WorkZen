import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, from, map, of, switchMap, tap } from 'rxjs';
import { Capacitor } from '@capacitor/core';
import { Preferences } from '@capacitor/preferences';
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
  refreshToken?: string;
}

/** Key under which the native app persists its refresh token (see AuthService javadoc-style
    comments below for why this only applies to the packaged app, never the web version). */
const NATIVE_REFRESH_TOKEN_KEY = 'workzen_refresh_token';

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
   *
   * On the packaged native app ONLY, the response also carries the raw refresh token in the
   * JSON body (see backend LoginResponse.refreshToken javadoc) - a cross-origin HttpOnly cookie
   * over plain HTTP is unreliable inside a Capacitor WebView (SameSite=None needs HTTPS, which
   * this deployment doesn't have), so the native app persists it itself via Capacitor
   * Preferences instead, surviving a full app close/reopen. The web version never reads or
   * stores this field - it keeps relying purely on the cookie, unchanged.
   */
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<ApiEnvelope<LoginResponse>>(`${this.baseUrl}/login`, request, { withCredentials: true })
      .pipe(
        map(envelope => envelope.data),
        tap(data => {
          this.tokenService.setAccessToken(data.accessToken, data.expiresIn);
          this.authState.setUser(data.user);
          this.persistNativeRefreshToken(data.refreshToken);
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
   * Silently refreshes the access token. On the web, this relies purely on the HttpOnly
   * refresh-token cookie (unchanged from before). On the native app, it explicitly sends the
   * refresh token it persisted at login (or from a previous refresh - the backend rotates the
   * refresh token on every use, so the newly-returned one is re-persisted each time too).
   */
  refresh(): Observable<RefreshData> {
    return from(this.getNativeRefreshTokenIfAny()).pipe(
      switchMap(nativeToken => {
        const body = nativeToken ? { refreshToken: nativeToken } : {};
        return this.http.post<ApiEnvelope<RefreshData>>(`${this.baseUrl}/refresh`, body, { withCredentials: true });
      }),
      map(envelope => envelope.data),
      tap(data => {
        this.tokenService.setAccessToken(data.accessToken, data.expiresIn);
        this.persistNativeRefreshToken(data.refreshToken);
      })
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
    if (Capacitor.isNativePlatform()) {
      Preferences.remove({ key: NATIVE_REFRESH_TOKEN_KEY });
    }
  }

  /** No-op on the web - only the native app persists a refresh token at all (see login()/refresh() javadoc for why). */
  private persistNativeRefreshToken(refreshToken: string | undefined): void {
    if (!refreshToken || !Capacitor.isNativePlatform()) return;
    Preferences.set({ key: NATIVE_REFRESH_TOKEN_KEY, value: refreshToken });
  }

  /** Resolves to null on the web (and on native, before any login has ever persisted one) - either way, refresh() then falls back to the HttpOnly cookie exactly as before. */
  private async getNativeRefreshTokenIfAny(): Promise<string | null> {
    if (!Capacitor.isNativePlatform()) return null;
    const result = await Preferences.get({ key: NATIVE_REFRESH_TOKEN_KEY });
    return result.value;
  }
}
