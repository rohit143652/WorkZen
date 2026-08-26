import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { environment } from '../../../../environments/environment';

interface AuditLogRow {
  id: number;
  userId: number | null;
  action: string;
  ipAddress: string;
  description: string;
  createdAt: string;
}

@Component({
  selector: 'app-audit-log-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './audit-log-list.component.html'
})
export class AuditLogListComponent {
  private readonly http = inject(HttpClient);

  readonly logs = signal<AuditLogRow[]>([]);
  readonly loading = signal(true);

  constructor() {
    this.http.get<{ data: { content: AuditLogRow[] } }>(`${environment.apiUrl}/audit-logs`).subscribe({
      next: res => { this.logs.set(res.data.content); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}
