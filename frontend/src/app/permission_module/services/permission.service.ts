import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PermissionOption } from '../models/permission.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class PermissionService {
  private readonly http = inject(HttpClient);

  list(): Observable<PermissionOption[]> {
    return this.http.get<ApiEnvelope<PermissionOption[]>>(`${environment.apiUrl}/permissions`).pipe(map(e => e.data));
  }
}
