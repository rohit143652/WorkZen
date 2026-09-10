import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SubscriptionPlan, SubscriptionPlanRequest } from '../models/subscription.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

export interface FeatureCategory { label: string; codes: string[]; }

@Injectable({ providedIn: 'root' })
export class SubscriptionPlanService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/subscription-plans`;

  /** The feature catalog structure (category -> codes) - independent of any one company, for the Plan Form's checkboxes. */
  getFeatureCatalog(): Observable<FeatureCategory[]> {
    return this.http.get<ApiEnvelope<FeatureCategory[]>>(`${environment.apiUrl}/features/catalog`).pipe(map(e => e.data));
  }

  list(activeOnly = false): Observable<SubscriptionPlan[]> {
    return this.http.get<ApiEnvelope<SubscriptionPlan[]>>(this.baseUrl, { params: { activeOnly } }).pipe(map(e => e.data));
  }

  getById(id: number): Observable<SubscriptionPlan> {
    return this.http.get<ApiEnvelope<SubscriptionPlan>>(`${this.baseUrl}/${id}`).pipe(map(e => e.data));
  }

  create(request: SubscriptionPlanRequest): Observable<SubscriptionPlan> {
    return this.http.post<ApiEnvelope<SubscriptionPlan>>(this.baseUrl, request).pipe(map(e => e.data));
  }

  update(id: number, request: SubscriptionPlanRequest): Observable<SubscriptionPlan> {
    return this.http.put<ApiEnvelope<SubscriptionPlan>>(`${this.baseUrl}/${id}`, request).pipe(map(e => e.data));
  }

  activate(id: number): Observable<SubscriptionPlan> {
    return this.http.put<ApiEnvelope<SubscriptionPlan>>(`${this.baseUrl}/${id}/activate`, {}).pipe(map(e => e.data));
  }

  deactivate(id: number): Observable<SubscriptionPlan> {
    return this.http.put<ApiEnvelope<SubscriptionPlan>>(`${this.baseUrl}/${id}/deactivate`, {}).pipe(map(e => e.data));
  }
}
