import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PermissionService } from '../../services/permission.service';
import { PermissionOption } from '../../models/permission.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-permission-list',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent],
  templateUrl: './permission-list.component.html'
})
export class PermissionListComponent {
  private readonly permissionService = inject(PermissionService);

  readonly permissions = signal<PermissionOption[]>([]);
  readonly loading = signal(true);
  searchTerm = '';

  constructor() {
    this.permissionService.list().subscribe({
      next: list => { this.permissions.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  /** Every permission ever seeded (across every phase's migrations) shows here - the backend endpoint has no filtering, this is genuinely the complete catalog. */
  filteredPermissions(): PermissionOption[] {
    const term = this.searchTerm.trim().toLowerCase();
    const list = !term
      ? this.permissions()
      : this.permissions().filter(p =>
          p.name.toLowerCase().includes(term) || (p.description ?? '').toLowerCase().includes(term));
    return [...list].sort((a, b) => a.name.localeCompare(b.name));
  }
}
