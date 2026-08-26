import { Injectable } from '@angular/core';

/**
 * Access token is kept in memory only (a plain class field), never in
 * localStorage/sessionStorage, to minimize the XSS attack surface.
 * The refresh token never touches JavaScript at all - it lives in an
 * HttpOnly cookie set by the backend (see AuthController on the server).
 *
 * Because the access token lives in memory, it is lost on a full page
 * reload. AppInitializer / AuthStateService calls /api/auth/refresh on
 * startup (relying on the HttpOnly cookie) to silently re-establish it.
 */
@Injectable({ providedIn: 'root' })
export class TokenService {
  private accessToken: string | null = null;
  private accessTokenExpiresAt: number | null = null; // epoch millis

  getAccessToken(): string | null {
    return this.accessToken;
  }

  setAccessToken(token: string, expiresInSeconds: number): void {
    this.accessToken = token;
    this.accessTokenExpiresAt = Date.now() + expiresInSeconds * 1000;
  }

  clearTokens(): void {
    this.accessToken = null;
    this.accessTokenExpiresAt = null;
  }

  isTokenExpired(): boolean {
    if (!this.accessToken || !this.accessTokenExpiresAt) return true;
    // Treat token as expired 10s before actual expiry to avoid race conditions.
    return Date.now() >= this.accessTokenExpiresAt - 10_000;
  }

  hasToken(): boolean {
    return !!this.accessToken;
  }
}
