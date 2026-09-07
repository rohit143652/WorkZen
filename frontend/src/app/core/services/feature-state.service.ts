import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { environment } from '../../../environments/environment';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

/**
 * Company Feature Configuration, from the CURRENT user's own perspective - the frontend half of
 * the "Company Feature Enabled AND User Role Has Permission" rule (see FeatureAccessService on
 * the backend for the authoritative version of this same check). This is a UX convenience only:
 * hiding a Check-In button when a feature is off avoids a pointless round-trip to an API call
 * that would just get rejected anyway, but the backend enforces the real restriction regardless
 * of whatever this service says - never trust this alone for anything security-sensitive.
 *
 * Absence of a code in the map means enabled, matching the backend's own default exactly (see
 * that service's javadoc for why).
 */
@Injectable({ providedIn: 'root' })
export class FeatureStateService {
  private readonly http = inject(HttpClient);
  private readonly features = signal<Record<string, boolean>>({});
  private readonly loaded = signal(false);

  readonly isLoaded = computed(() => this.loaded());

  /** Called once after login/session-restore succeeds (see app.config.ts's initializeAuth() and login.component.ts). Safe to call again any time features might have changed (e.g. after a Super Admin edits them) - just re-fetches. */
  load(): void {
    this.http.get<ApiEnvelope<Record<string, boolean>>>(`${environment.apiUrl}/features/mine`).subscribe({
      next: res => { this.features.set(res.data); this.loaded.set(true); },
      error: () => { this.features.set({}); this.loaded.set(true); }
    });
  }

  clear(): void {
    this.features.set({});
    this.loaded.set(false);
  }

  isEnabled(featureCode: string): boolean {
    const map = this.features();
    return map[featureCode] !== false;
  }
}
