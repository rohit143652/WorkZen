import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { environment } from '../../../../environments/environment';

interface PermissionRow {
  id: number;
  name: string;
  description: string;
}

@Component({
  selector: 'app-permission-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './permission-list.component.html'
})
export class PermissionListComponent {
  private readonly http = inject(HttpClient);

  readonly permissions = signal<PermissionRow[]>([]);
  readonly loading = signal(true);

  constructor() {
    this.http.get<{ data: PermissionRow[] }>(`${environment.apiUrl}/permissions`).subscribe({
      next: res => { this.permissions.set(res.data); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}
