import { CommonModule } from '@angular/common';
import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PaidLeaveService } from '../../services/paid-leave.service';
import {
  EmployeePaidLeaveBalance, EXTRA_LEAVE_REASONS, ExtraPaidLeaveResponse
} from '../../models/paid-leave.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

/**
 * Embedded in Employee Details. Self-access is enforced by the backend
 * (employee can view their own; PAID_LEAVE_READ needed for anyone else's) -
 * this component just calls the API and quietly hides itself on a 403,
 * since Employee Details itself has no route-level permission guard.
 */
@Component({
  selector: 'app-employee-paid-leave',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StatusBadgeComponent, HasPermissionDirective],
  templateUrl: './employee-paid-leave.component.html'
})
export class EmployeePaidLeaveComponent {
  readonly employeeId = input.required<number>();

  private readonly paidLeaveService = inject(PaidLeaveService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly fb = inject(FormBuilder);

  readonly reasons = EXTRA_LEAVE_REASONS;
  readonly loading = signal(true);
  readonly visible = signal(true);
  readonly balance = signal<EmployeePaidLeaveBalance | null>(null);
  readonly extraHistory = signal<ExtraPaidLeaveResponse[]>([]);
  readonly showMonthlyHistory = signal(false);
  readonly monthlyHistory = signal<EmployeePaidLeaveBalance[]>([]);

  readonly showGrantForm = signal(false);
  readonly granting = signal(false);
  readonly editingGrantId = signal<number | null>(null);

  readonly grantForm = this.fb.nonNullable.group({
    leaveDays: [1, [Validators.required, Validators.min(0.5)]],
    reason: ['MEDICAL' as 'MEDICAL' | 'SPECIAL' | 'EMERGENCY' | 'OTHER', Validators.required],
    startDate: [new Date().toISOString().slice(0, 10), Validators.required],
    endDate: [''],
    remark: ['']
  });

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.paidLeaveService.getCurrentBalance(this.employeeId()).subscribe({
      next: b => {
        this.balance.set(b);
        this.loading.set(false);
        this.loadExtraHistory();
      },
      error: err => {
        this.loading.set(false);
        if (err.status === 403) { this.visible.set(false); } else { this.toast.error('Unable to load paid leave information.'); }
      }
    });
  }

  private loadExtraHistory(): void {
    this.paidLeaveService.listExtraLeave(this.employeeId()).subscribe({
      next: list => this.extraHistory.set(list),
      error: () => {}
    });
  }

  toggleMonthlyHistory(): void {
    const next = !this.showMonthlyHistory();
    this.showMonthlyHistory.set(next);
    if (next && this.monthlyHistory().length === 0) {
      this.paidLeaveService.getBalanceHistory(this.employeeId()).subscribe({
        next: list => this.monthlyHistory.set(list),
        error: () => this.toast.error('Unable to load monthly leave history.')
      });
    }
  }

  openGrantForm(): void {
    this.editingGrantId.set(null);
    this.grantForm.reset({
      leaveDays: 1, reason: 'MEDICAL', startDate: new Date().toISOString().slice(0, 10), endDate: '', remark: ''
    });
    this.showGrantForm.set(true);
  }

  editGrant(grant: ExtraPaidLeaveResponse): void {
    this.editingGrantId.set(grant.id);
    this.grantForm.reset({
      leaveDays: grant.leaveDays, reason: grant.reason,
      startDate: grant.startDate, endDate: grant.endDate ?? '', remark: grant.remark ?? ''
    });
    this.showGrantForm.set(true);
  }

  cancelGrantForm(): void {
    this.showGrantForm.set(false);
  }

  submitGrant(): void {
    if (this.grantForm.invalid) {
      this.grantForm.markAllAsTouched();
      return;
    }
    const raw = this.grantForm.getRawValue();
    if (raw.endDate && raw.endDate < raw.startDate) {
      this.toast.warning('End date cannot be before start date.');
      return;
    }
    const request = {
      leaveDays: raw.leaveDays,
      reason: raw.reason,
      startDate: raw.startDate,
      endDate: raw.endDate || null,
      remark: raw.remark || undefined
    };
    this.granting.set(true);
    const editingId = this.editingGrantId();
    const action$ = editingId
      ? this.paidLeaveService.updateExtraLeave(this.employeeId(), editingId, request)
      : this.paidLeaveService.grantExtraLeave(this.employeeId(), request);
    action$.subscribe({
      next: () => {
        this.granting.set(false);
        this.showGrantForm.set(false);
        this.toast.success(editingId ? 'Extra paid leave updated.' : 'Extra paid leave granted.');
        this.load();
      },
      error: err => {
        this.granting.set(false);
        this.toast.error(err.error?.message ?? 'Unable to save this extra leave grant.');
      }
    });
  }

  async cancelGrant(grant: ExtraPaidLeaveResponse): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Cancel this extra leave grant?',
      message: `This will cancel the ${grant.leaveDays}-day ${grant.reason} grant. The record stays in history but no longer counts toward the balance.`,
      danger: true
    });
    if (!ok) return;
    this.paidLeaveService.cancelExtraLeave(this.employeeId(), grant.id).subscribe({
      next: () => { this.toast.success('Extra leave grant cancelled.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to cancel this grant.')
    });
  }
}
