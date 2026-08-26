import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SiteService } from '../../services/site.service';
import { SiteResponse } from '../../models/site.model';
import { StatusBadgeComponent, BadgeKind } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

@Component({
  selector: 'app-site-list',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent, HasPermissionDirective],
  templateUrl: './site-list.component.html'
})
export class SiteListComponent {
  private readonly siteService = inject(SiteService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly sites = signal<SiteResponse[]>([]);
  readonly loading = signal(true);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.siteService.list().subscribe({
      next: res => { this.sites.set(res.content); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  allocationLabel(site: SiteResponse): string {
    if (site.requiredEmployeeCount <= 0) return `${site.assignedEmployeeCount} assigned`;
    return `${site.assignedEmployeeCount} / ${site.requiredEmployeeCount}`;
  }

  allocationKind(site: SiteResponse): BadgeKind {
    if (site.requiredEmployeeCount <= 0) return 'muted';
    if (site.assignedEmployeeCount > site.requiredEmployeeCount) return 'warning';
    if (site.assignedEmployeeCount === site.requiredEmployeeCount) return 'success';
    return 'info';
  }

  async toggleStatus(site: SiteResponse): Promise<void> {
    const activating = site.status !== 'ACTIVE';
    const ok = await this.confirmDialog.ask({
      title: activating ? 'Activate site?' : 'Deactivate site?',
      message: activating
        ? `Reactivate ${site.siteName}? It will become selectable for assignments again.`
        : `Deactivating ${site.siteName} will unassign all ${site.assignedEmployeeCount} employee(s) currently working here, and the site will no longer appear when assigning employees. Continue?`,
      confirmLabel: activating ? 'Activate' : 'Deactivate',
      danger: !activating
    });
    if (!ok) return;
    const action$ = activating ? this.siteService.activate(site.id) : this.siteService.deactivate(site.id);
    action$.subscribe({
      next: () => {
        this.toast.success(activating ? 'Site activated successfully.' : 'Site deactivated and its employees unassigned.');
        this.load();
      },
      error: err => this.toast.error(err.error?.message ?? 'Unable to update.')
    });
  }
}
