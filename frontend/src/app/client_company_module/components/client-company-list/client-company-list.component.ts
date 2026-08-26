import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ClientCompanyService } from '../../services/client-company.service';
import { ClientCompanyResponse } from '../../models/client-company.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

@Component({
  selector: 'app-client-company-list',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent, HasPermissionDirective],
  templateUrl: './client-company-list.component.html'
})
export class ClientCompanyListComponent {
  private readonly clientCompanyService = inject(ClientCompanyService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly companies = signal<ClientCompanyResponse[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.clientCompanyService.list().subscribe({
      next: res => { this.companies.set(res.content); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); }
    });
  }

  async toggleStatus(company: ClientCompanyResponse): Promise<void> {
    const activating = company.status !== 'ACTIVE';
    const ok = await this.confirmDialog.ask({
      title: activating ? 'Activate client company?' : 'Deactivate client company?',
      message: activating
        ? `Reactivate ${company.companyName}?`
        : `This will also disable every login account under ${company.companyName}. Continue?`,
      confirmLabel: activating ? 'Activate' : 'Deactivate',
      danger: !activating
    });
    if (!ok) return;
    const action$ = activating ? this.clientCompanyService.activate(company.id) : this.clientCompanyService.deactivate(company.id);
    action$.subscribe({
      next: () => { this.toast.success(activating ? 'Client company activated.' : 'Client company deactivated.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to update client company.')
    });
  }
}
