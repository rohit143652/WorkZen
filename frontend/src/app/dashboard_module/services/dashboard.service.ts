import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardSummary } from '../models/dashboard-summary.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  getSummary(): Observable<DashboardSummary> {
    return this.http
      .get<ApiEnvelope<DashboardSummary>>(`${environment.apiUrl}/dashboard/summary`)
      .pipe(map(e => e.data));
  }
}
