import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ClientSubscription, ClientSubscriptionRequest, SubscriptionHistoryEntry } from '../models/subscription.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class ClientSubscriptionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = (clientCompanyId: number) => `${environment.apiUrl}/client-companies/${clientCompanyId}/subscription`;

  get(clientCompanyId: number): Observable<ClientSubscription> {
    return this.http.get<ApiEnvelope<ClientSubscription>>(this.baseUrl(clientCompanyId)).pipe(map(e => e.data));
  }

  update(clientCompanyId: number, request: ClientSubscriptionRequest): Observable<ClientSubscription> {
    return this.http.put<ApiEnvelope<ClientSubscription>>(this.baseUrl(clientCompanyId), request).pipe(map(e => e.data));
  }

  /** Manual "deactivate now" - sets status to CANCELLED. Distinct from the automatic daily expiry job. */
  deactivate(clientCompanyId: number, reason?: string): Observable<ClientSubscription> {
    return this.http.put<ApiEnvelope<ClientSubscription>>(`${this.baseUrl(clientCompanyId)}/deactivate`, { reason }).pipe(map(e => e.data));
  }

  history(clientCompanyId: number): Observable<SubscriptionHistoryEntry[]> {
    return this.http.get<ApiEnvelope<SubscriptionHistoryEntry[]>>(`${this.baseUrl(clientCompanyId)}/history`).pipe(map(e => e.data));
  }
}
