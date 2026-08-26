import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SalaryStructureService } from '../../services/salary-structure.service';
import { SalaryStructureResponse } from '../../models/salary-structure.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

@Component({
  selector: 'app-salary-structure-list',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent, HasPermissionDirective],
  templateUrl: './salary-structure-list.component.html'
})
export class SalaryStructureListComponent {
  private readonly structureService = inject(SalaryStructureService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly structures = signal<SalaryStructureResponse[]>([]);
  readonly loading = signal(true);
  readonly totalElements = signal(0);
  readonly page = signal(0);
  readonly pageSize = 10;

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.structureService.list(this.page(), this.pageSize).subscribe({
      next: res => { this.structures.set(res.content); this.totalElements.set(res.totalElements); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  totalPages(): number {
    return Math.max(1, Math.ceil(this.totalElements() / this.pageSize));
  }

  goToPage(next: number): void {
    this.page.set(next);
    this.load();
  }

  async toggleStatus(s: SalaryStructureResponse): Promise<void> {
    const activating = s.status !== 'ACTIVE';
    const ok = await this.confirmDialog.ask({
      title: activating ? 'Activate structure?' : 'Deactivate structure?',
      message: activating
        ? `"${s.structureName}" will become assignable to employees again.`
        : `"${s.structureName}" will no longer be assignable to new employees. Employees already assigned keep their current pay.`,
      confirmLabel: activating ? 'Activate' : 'Deactivate',
      danger: !activating
    });
    if (!ok) return;
    const action$ = activating ? this.structureService.activate(s.id) : this.structureService.deactivate(s.id);
    action$.subscribe({
      next: () => { this.toast.success('Updated successfully.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to update structure.')
    });
  }

  async duplicate(s: SalaryStructureResponse): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Duplicate structure?',
      message: `Create a copy of "${s.structureName}" with a new code that you can then edit independently?`,
      confirmLabel: 'Duplicate'
    });
    if (!ok) return;
    this.structureService.duplicate(s.id).subscribe({
      next: () => { this.toast.success('Structure duplicated successfully.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to duplicate structure.')
    });
  }

  async remove(s: SalaryStructureResponse): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Delete structure?',
      message: `Delete "${s.structureName}" permanently? This only works if it has never been assigned to an employee.`,
      confirmLabel: 'Delete',
      danger: true
    });
    if (!ok) return;
    this.structureService.delete(s.id).subscribe({
      next: () => { this.toast.success('Structure deleted successfully.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to delete structure.')
    });
  }
}
