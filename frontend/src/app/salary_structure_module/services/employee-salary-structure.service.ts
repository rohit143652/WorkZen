import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AssignSalaryStructureRequest, EmployeeSalaryStructureResponse } from '../models/employee-salary-structure.model';

interface ApiEnvelope<T> { success: boolean; message: string; data: T; }

@Injectable({ providedIn: 'root' })
export class EmployeeSalaryStructureService {
  private readonly http = inject(HttpClient);

  current(employeeId: number): Observable<EmployeeSalaryStructureResponse | null> {
    return this.http
      .get<ApiEnvelope<EmployeeSalaryStructureResponse | null>>(`${environment.apiUrl}/employees/${employeeId}/salary-structure`)
      .pipe(map(e => e.data));
  }

  assign(employeeId: number, request: AssignSalaryStructureRequest): Observable<EmployeeSalaryStructureResponse> {
    return this.http
      .post<ApiEnvelope<EmployeeSalaryStructureResponse>>(`${environment.apiUrl}/employees/${employeeId}/salary-structure`, request)
      .pipe(map(e => e.data));
  }

  history(employeeId: number): Observable<EmployeeSalaryStructureResponse[]> {
    return this.http
      .get<ApiEnvelope<EmployeeSalaryStructureResponse[]>>(`${environment.apiUrl}/employees/${employeeId}/salary-history`)
      .pipe(map(e => e.data));
  }
}
