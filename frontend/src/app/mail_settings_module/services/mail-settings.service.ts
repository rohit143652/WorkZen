import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MailOverview, MailSettings, MailSettingsRequest, MailTestResult } from '../models/mail-settings.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

/**
 * Super Admin only (the backend requires MAIL_SETTINGS_MANAGE on every one of these). Each call
 * takes the client company it is about; null means the platform default sender.
 */
@Injectable({ providedIn: 'root' })
export class MailSettingsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admin/mail-settings`;

  private scope(clientCompanyId: number | null): HttpParams {
    return clientCompanyId == null ? new HttpParams() : new HttpParams().set('clientCompanyId', clientCompanyId);
  }

  overview(): Observable<MailOverview> {
    return this.http.get<ApiEnvelope<MailOverview>>(`${this.baseUrl}/overview`).pipe(map(e => e.data));
  }

  get(clientCompanyId: number | null): Observable<MailSettings> {
    return this.http.get<ApiEnvelope<MailSettings>>(this.baseUrl, { params: this.scope(clientCompanyId) }).pipe(map(e => e.data));
  }

  save(clientCompanyId: number | null, request: MailSettingsRequest): Observable<MailSettings> {
    return this.http.put<ApiEnvelope<MailSettings>>(this.baseUrl, request, { params: this.scope(clientCompanyId) }).pipe(map(e => e.data));
  }

  /** Deletes that scope's saved sender - a company then uses the platform default; the platform default falls back to the MAIL_* variables. */
  remove(clientCompanyId: number | null): Observable<MailSettings> {
    return this.http.delete<ApiEnvelope<MailSettings>>(this.baseUrl, { params: this.scope(clientCompanyId) }).pipe(map(e => e.data));
  }

  /** Sends a real message exactly the way that company's real emails go out. */
  sendTest(clientCompanyId: number | null, toAddress: string): Observable<MailTestResult> {
    return this.http.post<ApiEnvelope<MailTestResult>>(`${this.baseUrl}/test`, { toAddress }, { params: this.scope(clientCompanyId) }).pipe(map(e => e.data));
  }
}
