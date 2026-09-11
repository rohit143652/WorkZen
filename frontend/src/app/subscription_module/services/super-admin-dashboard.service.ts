import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SuperAdminDashboardSummary } from '../models/subscription.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class SuperAdminDashboardService {
  private readonly http = inject(HttpClient);

  getSummary(): Observable<SuperAdminDashboardSummary> {
    return this.http.get<ApiEnvelope<SuperAdminDashboardSummary>>(`${environment.apiUrl}/super-admin-dashboard`).pipe(map(e => e.data));
  }
}
