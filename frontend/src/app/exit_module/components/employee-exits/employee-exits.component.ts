import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ExitService } from '../../services/exit.service';
import { EmployeeExitResponse } from '../../models/employee-exit.model';
import { EmployeeService } from '../../../employee_module/services/employee.service';
import { EmployeeResponse } from '../../../employee_module/models/employee.model';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { EmployeeSearchSelectComponent } from '../../../employee_module/components/employee-search-select/employee-search-select.component';

@Component({
  selector: 'app-employee-exits',
  standalone: true,
  imports: [CommonModule, FormsModule, HasPermissionDirective, StatusBadgeComponent, EmployeeSearchSelectComponent],
  templateUrl: './employee-exits.component.html'
})
export class EmployeeExitsComponent {
  private readonly exitService = inject(ExitService);
  private readonly employeeService = inject(EmployeeService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly exits = signal<EmployeeExitResponse[]>([]);
  readonly activeEmployees = signal<EmployeeResponse[]>([]);
  readonly loading = signal(true);
  search = '';

  get filteredExits(): EmployeeExitResponse[] {
    const term = this.search.trim().toLowerCase();
    if (!term) return this.exits();
    return this.exits().filter(e =>
      e.employeeCode.toLowerCase().includes(term) ||
      e.employeeName.toLowerCase().includes(term)
    );
  }

  // ---- Record resignation form ----
  readonly showAddForm = signal(false);
  readonly saving = signal(false);
  newEmployeeId: number | null = null;
  newResignationDate = '';
  newLastWorkingDay = '';
  newReason = '';

  // ---- Settlement preview/confirm modal ----
  readonly showSettleModal = signal(false);
  readonly loadingPreview = signal(false);
  readonly settling = signal(false);
  previewingExit: EmployeeExitResponse | null = null;

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.exitService.list().subscribe({
      next: list => { this.exits.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openAddForm(): void {
    this.showAddForm.set(true);
    this.newEmployeeId = null;
    this.newResignationDate = '';
    this.newLastWorkingDay = '';
    this.newReason = '';
    if (this.activeEmployees().length === 0) {
      this.employeeService.search({ status: 'ACTIVE', size: 500, sort: 'employeeCode,asc' }).subscribe(page => {
        this.activeEmployees.set(page.content);
      });
    }
  }

  closeAddForm(): void {
    this.showAddForm.set(false);
  }

  submitResignation(): void {
    if (!this.newEmployeeId || !this.newResignationDate || !this.newLastWorkingDay) return;
    if (this.newLastWorkingDay < this.newResignationDate) {
      this.toast.error('Last working day cannot be before the resignation date.');
      return;
    }
    this.saving.set(true);
    this.exitService.initiate({
      employeeId: this.newEmployeeId,
      resignationDate: this.newResignationDate,
      lastWorkingDay: this.newLastWorkingDay,
      reason: this.newReason.trim() || undefined
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.showAddForm.set(false);
        this.toast.success('Resignation recorded.');
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to record this resignation.');
      }
    });
  }

  openSettleModal(exit: EmployeeExitResponse): void {
    this.previewingExit = exit;
    this.showSettleModal.set(true);
    this.loadingPreview.set(true);
    this.exitService.previewSettlement(exit.id).subscribe({
      next: preview => {
        this.previewingExit = preview;
        this.loadingPreview.set(false);
      },
      error: err => {
        this.loadingPreview.set(false);
        this.toast.error(err.error?.message ?? 'Unable to compute the settlement preview.');
        this.showSettleModal.set(false);
      }
    });
  }

  closeSettleModal(): void {
    this.showSettleModal.set(false);
    this.previewingExit = null;
  }

  async confirmSettle(): Promise<void> {
    if (!this.previewingExit) return;
    const exit = this.previewingExit;

    const ok = await this.confirmDialog.ask({
      title: 'Process Full & Final Settlement?',
      message: `This will pay out a net settlement of ₹${exit.netSettlementAmount} to ${exit.employeeName} and permanently deactivate their account. This cannot be undone.`,
      confirmLabel: 'Confirm & Settle'
    });
    if (!ok) return;

    this.settling.set(true);
    this.exitService.settle(exit.id).subscribe({
      next: () => {
        this.settling.set(false);
        this.closeSettleModal();
        this.toast.success(`Settlement processed for ${exit.employeeName} - employee deactivated.`);
        this.load();
      },
      error: err => {
        this.settling.set(false);
        this.toast.error(err.error?.message ?? 'Unable to process this settlement.');
      }
    });
  }

  /** Only offered here, for an employee whose Full & Final Settlement is done (status SETTLED) -
      not on the plain Employees list, where "deactivated" could mean many things besides a
      formal, settled exit. Reuses the SAME underlying record (full history stays attached) with
      a freshly generated employee code for the new employment period. */
  async rejoinEmployee(exit: EmployeeExitResponse): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Rejoin this employee?',
      message: `${exit.employeeName} will be reactivated under a NEW employee code (their current code ${exit.employeeCode} stays as historical record) - all their past history (attendance, salary, advances, this settlement) stays linked to this same record.`,
      confirmLabel: 'Rejoin with New Code'
    });
    if (!ok) return;

    this.employeeService.rejoin(exit.employeeId).subscribe({
      next: updated => this.toast.success(`Rejoined - new employee code: ${updated.employeeCode}.`),
      error: err => this.toast.error(err.error?.message ?? 'Unable to rejoin this employee.')
    });
  }
}
