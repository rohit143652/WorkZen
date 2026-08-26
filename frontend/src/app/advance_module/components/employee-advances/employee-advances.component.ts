import { CommonModule } from '@angular/common';
import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { EmployeeAdvanceService } from '../../services/employee-advance.service';
import { AdvanceRecoveryTransactionResponse, EmployeeAdvanceResponse } from '../../models/advance.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

/**
 * Embedded in Employee Details behind *appHasPermission="'ADVANCE_READ'" -
 * CLIENT_ADMIN only, no employee self-view (unlike Paid Leave), since
 * advances are purely an admin/finance concern.
 */
@Component({
  selector: 'app-employee-advances',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, StatusBadgeComponent, HasPermissionDirective],
  templateUrl: './employee-advances.component.html'
})
export class EmployeeAdvancesComponent {
  readonly employeeId = input.required<number>();

  private readonly advanceService = inject(EmployeeAdvanceService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly advances = signal<EmployeeAdvanceResponse[]>([]);

  readonly months = MONTH_NAMES.map((label, index) => ({ label, value: index + 1 }));
  readonly years: number[] = (() => {
    const current = new Date().getFullYear();
    return [current, current + 1];
  })();

  readonly totalOutstanding = () => this.advances()
    .filter(a => a.status === 'ACTIVE')
    .reduce((sum, a) => sum + a.outstandingAmount, 0);

  /** Rough estimate only ("~X more") - actual future installments can vary if the monthly recovery amount changes or payroll capacity is limited some month. */
  pendingInstallments(a: EmployeeAdvanceResponse): number {
    if (a.status !== 'ACTIVE' || a.monthlyRecoveryAmount <= 0 || a.outstandingAmount <= 0) return 0;
    return Math.ceil(a.outstandingAmount / a.monthlyRecoveryAmount);
  }

  readonly showGrantForm = signal(false);
  readonly granting = signal(false);
  readonly editingRecoveryId = signal<number | null>(null);
  readonly savingId = signal<number | null>(null);
  editRecoveryAmount = 0;

  readonly grantForm = this.fb.nonNullable.group({
    advanceDate: [new Date().toISOString().slice(0, 10), Validators.required],
    amount: [500, [Validators.required, Validators.min(0.01)]],
    reason: [''],
    paymentMode: ['CASH'],
    isLoan: [false],
    monthlyRecoveryAmount: [500, [Validators.required, Validators.min(0)]],
    recoveryStartYear: [new Date().getFullYear(), Validators.required],
    recoveryStartMonth: [new Date().getMonth() + 1, Validators.required],
    remarks: ['']
  });

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.advanceService.list(this.employeeId()).subscribe({
      next: list => { this.advances.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openGrantForm(): void {
    this.grantForm.reset({
      advanceDate: new Date().toISOString().slice(0, 10), amount: 500, reason: '', paymentMode: 'CASH', isLoan: false, monthlyRecoveryAmount: 500,
      recoveryStartYear: new Date().getFullYear(), recoveryStartMonth: new Date().getMonth() + 1, remarks: ''
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
    this.granting.set(true);
    const { isLoan, ...raw } = this.grantForm.getRawValue();
    const [dateYear, dateMonth] = raw.advanceDate.split('-').map(Number);
    const request = isLoan
      ? raw
      // Not a loan - recover the full amount from the current month's payroll (or as much as
      // safely possible; any remainder automatically stays outstanding for next month, same
      // safety cap the payroll engine already applies to every advance).
      : { ...raw, monthlyRecoveryAmount: raw.amount, recoveryStartYear: dateYear, recoveryStartMonth: dateMonth };
    this.advanceService.grant(this.employeeId(), request).subscribe({
      next: () => {
        this.granting.set(false);
        this.showGrantForm.set(false);
        this.toast.success('Advance granted.');
        this.load();
      },
      error: err => {
        this.granting.set(false);
        this.toast.error(err.error?.message ?? 'Unable to grant this advance.');
      }
    });
  }

  startEditRecovery(advance: EmployeeAdvanceResponse): void {
    this.editingRecoveryId.set(advance.id);
    this.editRecoveryAmount = advance.monthlyRecoveryAmount;
  }

  cancelEditRecovery(): void {
    this.editingRecoveryId.set(null);
  }

  saveRecoveryAmount(advance: EmployeeAdvanceResponse): void {
    if (this.editRecoveryAmount < 0) {
      this.toast.warning('Monthly recovery amount must be 0 or more.');
      return;
    }
    this.savingId.set(advance.id);
    this.advanceService.updateRecoveryAmount(this.employeeId(), advance.id, this.editRecoveryAmount).subscribe({
      next: () => {
        this.savingId.set(null);
        this.editingRecoveryId.set(null);
        this.toast.success('Recovery amount updated - applies from next month onward.');
        this.load();
      },
      error: err => {
        this.savingId.set(null);
        this.toast.error(err.error?.message ?? 'Unable to update the recovery amount.');
      }
    });
  }

  async settle(advance: EmployeeAdvanceResponse): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Settle this advance?',
      message: `This marks the remaining ₹${advance.outstandingAmount} as paid outside payroll. No further amount will be recovered from salary for this advance.`,
      confirmLabel: 'Settle',
      danger: true
    });
    if (!ok) return;
    this.advanceService.settle(this.employeeId(), advance.id).subscribe({
      next: () => { this.toast.success('Advance settled.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to settle this advance.')
    });
  }

  /** "Should this month's (and future) payroll cut this advance?" - manual settlement is unaffected either way. */
  toggleRecoverViaPayroll(advance: EmployeeAdvanceResponse, checked: boolean): void {
    this.advanceService.updateRecoverViaPayroll(this.employeeId(), advance.id, checked).subscribe({
      next: () => {
        this.toast.success(checked
          ? 'Payroll will recover this advance from the next calculation onward.'
          : 'Payroll will skip this advance until turned back on - already-paid manual settlements are unaffected.');
        this.load();
      },
      error: err => { this.toast.error(err.error?.message ?? 'Unable to update this setting.'); this.load(); }
    });
  }

  // ---- Partial settlement (employee paid part of the outstanding amount directly, outside payroll) - shown in a modal ----
  readonly settlingAdvance = signal<EmployeeAdvanceResponse | null>(null);
  partialSettlementAmount = 0;
  partialSettlementRemark = '';

  openPartialSettleForm(advance: EmployeeAdvanceResponse): void {
    this.settlingAdvance.set(advance);
    this.partialSettlementAmount = 0;
    this.partialSettlementRemark = '';
  }

  cancelPartialSettleForm(): void {
    this.settlingAdvance.set(null);
  }

  readonly settlingPartial = signal(false);

  submitPartialSettlement(): void {
    const advance = this.settlingAdvance();
    if (!advance) return;
    if (this.partialSettlementAmount <= 0) {
      this.toast.warning('Settlement amount must be greater than 0.');
      return;
    }
    if (this.partialSettlementAmount > advance.outstandingAmount) {
      this.toast.warning(`Settlement amount cannot exceed the outstanding amount (₹${advance.outstandingAmount}).`);
      return;
    }
    this.settlingPartial.set(true);
    this.advanceService.settlePartial(this.employeeId(), advance.id,
      { amount: this.partialSettlementAmount, remark: this.partialSettlementRemark || undefined }).subscribe({
      next: () => {
        this.settlingPartial.set(false);
        this.settlingAdvance.set(null);
        this.toast.success('Partial settlement recorded. Future payroll recovers from the reduced outstanding only.');
        this.load();
      },
      error: err => { this.settlingPartial.set(false); this.toast.error(err.error?.message ?? 'Unable to record this settlement.'); }
    });
  }

  // ---- Recovery history (which payroll, or which manual payment, recovered each amount) - shown in a modal ----
  readonly historyAdvance = signal<EmployeeAdvanceResponse | null>(null);
  readonly historyLoading = signal(false);
  readonly recoveryHistory = signal<AdvanceRecoveryTransactionResponse[]>([]);

  openRecoveryHistory(advance: EmployeeAdvanceResponse): void {
    this.historyAdvance.set(advance);
    this.historyLoading.set(true);
    this.advanceService.getRecoveryHistory(this.employeeId(), advance.id).subscribe({
      next: history => { this.recoveryHistory.set(history); this.historyLoading.set(false); },
      error: () => { this.historyLoading.set(false); this.toast.error('Unable to load recovery history.'); }
    });
  }

  closeRecoveryHistory(): void {
    this.historyAdvance.set(null);
  }
}
