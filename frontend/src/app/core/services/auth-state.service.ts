import { Injectable, computed, signal } from '@angular/core';
import { AuthenticatedUser } from '../models/user.model';

/**
 * Central reactive authentication state, exposed as Angular signals.
 * Components/guards read from here; only AuthService writes to it.
 */
@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly _currentUser = signal<AuthenticatedUser | null>(null);
  private readonly _initializing = signal<boolean>(true);

  readonly currentUser = this._currentUser.asReadonly();
  readonly initializing = this._initializing.asReadonly();

  readonly isAuthenticated = computed(() => this._currentUser() !== null);
  readonly roles = computed(() => this._currentUser()?.roles ?? []);
  readonly permissions = computed(() => this._currentUser()?.permissions ?? []);

  setUser(user: AuthenticatedUser): void {
    this._currentUser.set(user);
  }

  clearUser(): void {
    this._currentUser.set(null);
  }

  setInitializing(value: boolean): void {
    this._initializing.set(value);
  }

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  hasAnyRole(roles: string[]): boolean {
    const mine = this.roles();
    return roles.some(r => mine.includes(r));
  }

  hasPermission(permission: string): boolean {
    return this.permissions().includes(permission);
  }
}
