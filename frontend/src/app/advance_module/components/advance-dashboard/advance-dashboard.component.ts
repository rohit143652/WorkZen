import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EmployeeAdvanceService } from '../../services/employee-advance.service';
import { AdvanceDashboardSummary, AdvanceRecoveryTransactionResponse, EmployeeAdvanceResponse } from '../../models/advance.model';
import { EmployeeService } from '../../../employee_module/services/employee.service';
import { EmployeeResponse } from '../../../employee_module/models/employee.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

/**
 * Company-wide Advance Dashboard (architecture completion audit, spec
 * section 12) - CLIENT_ADMIN only, gated by ADVANCE_READ at the route
 * level. Every advance across every employee, in one place, with totals -
 * previously the only way to see an advance was to open that specific
 * employee's own Details page one at a time.
 */
@Component({
  selector: 'app-advance-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink, StatusBadgeComponent, HasPermissionDirective],
  templateUrl: './advance-dashboard.component.html'
})
export class AdvanceDashboardComponent {
  private readonly advanceService = inject(EmployeeAdvanceService);
  private readonly employeeService = inject(EmployeeService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly summary = signal<AdvanceDashboardSummary | null>(null);
  readonly advances = signal<EmployeeAdvanceResponse[]>([]);

  searchTerm = '';
  statusFilter = '';
  readonly statuses = ['ACTIVE', 'SETTLED', 'CANCELLED'];

  readonly filteredAdvances = computed(() => {
    const term = this.searchTerm.trim().toLowerCase();
    return this.advances().filter(a => {
      const matchesTerm = !term
        || a.employeeCode.toLowerCase().includes(term)
        || a.employeeName.toLowerCase().includes(term)
        || (a.reason ?? '').toLowerCase().includes(term);
      const matchesStatus = !this.statusFilter || a.status === this.statusFilter;
      return matchesTerm && matchesStatus;
    });
  });

  /** Employee-wise rollup (spec section 12) - one row per employee, summing across all their advances. */
  readonly employeeRows = computed(() => {
    const byEmployee = new Map<number, { employeeCode: string; employeeName: string; given: number; recovered: number; outstanding: number; monthlyRecovery: number; hasActive: boolean }>();
    for (const a of this.filteredAdvances()) {
      const row = byEmployee.get(a.employeeId) ?? {
        employeeCode: a.employeeCode, employeeName: a.employeeName,
        given: 0, recovered: 0, outstanding: 0, monthlyRecovery: 0, hasActive: false
      };
      row.given += a.amount;
      row.recovered += a.recoveredAmount;
      row.outstanding += a.outstandingAmount;
      if (a.status === 'ACTIVE') {
        row.monthlyRecovery += a.monthlyRecoveryAmount;
        row.hasActive = true;
      }
      byEmployee.set(a.employeeId, row);
    }
    return Array.from(byEmployee.values()).sort((a, b) => b.outstanding - a.outstanding);
  });

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.advanceService.getDashboardSummary().subscribe({
      next: s => this.summary.set(s),
      error: () => this.toast.error('Unable to load the advance summary.')
    });
    this.advanceService.listAll().subscribe({
      next: list => { this.advances.set(list); this.loading.set(false); },
      error: () => { this.loading.set(false); this.toast.error('Unable to load advances.'); }
    });
  }

  // ---- Give Advance directly from the dashboard (employee picker + the same grant form) ----
  readonly showGiveForm = signal(false);
  readonly giving = signal(false);
  readonly employees = signal<EmployeeResponse[]>([]);
  readonly employeesLoading = signal(false);

  readonly months = MONTH_NAMES.map((label, index) => ({ label, value: index + 1 }));
  readonly years: number[] = (() => {
    const current = new Date().getFullYear();
    return [current, current + 1];
  })();

  readonly giveForm = this.fb.nonNullable.group({
    employeeId: [null as number | null, Validators.required],
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

  openGiveForm(): void {
    this.giveForm.reset({
      employeeId: null, advanceDate: new Date().toISOString().slice(0, 10), amount: 500, reason: '', paymentMode: 'CASH', isLoan: false,
      monthlyRecoveryAmount: 500, recoveryStartYear: new Date().getFullYear(), recoveryStartMonth: new Date().getMonth() + 1, remarks: ''
    });
    this.showGiveForm.set(true);
    if (this.employees().length === 0) {
      this.employeesLoading.set(true);
      this.employeeService.search({ status: 'ACTIVE', size: 500, sort: 'employeeCode,asc' }).subscribe({
        next: page => { this.employees.set(page.content); this.employeesLoading.set(false); },
        error: () => { this.employeesLoading.set(false); this.toast.error('Unable to load the employee list.'); }
      });
    }
  }

  cancelGiveForm(): void {
    this.showGiveForm.set(false);
  }

  submitGive(): void {
    if (this.giveForm.invalid) {
      this.giveForm.markAllAsTouched();
      this.toast.warning('Please select an employee and fill in the required fields.');
      return;
    }
    const { employeeId, isLoan, ...raw } = this.giveForm.getRawValue();
    const [dateYear, dateMonth] = raw.advanceDate.split('-').map(Number);
    const request = isLoan
      ? raw
      // Not a loan - recover the full amount from the current month's payroll (or as much as
      // safely possible; any remainder automatically stays outstanding for next month).
      : { ...raw, monthlyRecoveryAmount: raw.amount, recoveryStartYear: dateYear, recoveryStartMonth: dateMonth };
    this.giving.set(true);
    this.advanceService.grant(employeeId!, request).subscribe({
      next: () => {
        this.giving.set(false);
        this.showGiveForm.set(false);
        this.toast.success('Advance given.');
        this.load();
      },
      error: err => {
        this.giving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to give this advance.');
      }
    });
  }

  /** Rough estimate only ("~X more") - actual future installments can vary if the monthly recovery amount changes or payroll capacity is limited some month. */
  pendingInstallments(a: EmployeeAdvanceResponse): number {
    if (a.status !== 'ACTIVE' || a.monthlyRecoveryAmount <= 0 || a.outstandingAmount <= 0) return 0;
    return Math.ceil(a.outstandingAmount / a.monthlyRecoveryAmount);
  }

  // ---- Settle / recovery history, directly from the dashboard table - shown in modals ----
  readonly settlingAdvance = signal<EmployeeAdvanceResponse | null>(null);
  partialSettlementAmount = 0;
  partialSettlementRemark = '';
  readonly settlingPartial = signal(false);

  openPartialSettleForm(advance: EmployeeAdvanceResponse): void {
    this.settlingAdvance.set(advance);
    this.partialSettlementAmount = 0;
    this.partialSettlementRemark = '';
  }

  cancelPartialSettleForm(): void {
    this.settlingAdvance.set(null);
  }

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
    this.advanceService.settlePartial(advance.employeeId, advance.id,
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

  async settleFull(advance: EmployeeAdvanceResponse): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Settle this advance in full?',
      message: `This marks the remaining ₹${advance.outstandingAmount} as paid outside payroll. No further amount will be recovered from salary for this advance.`,
      confirmLabel: 'Settle Full',
      danger: true
    });
    if (!ok) return;
    this.advanceService.settle(advance.employeeId, advance.id).subscribe({
      next: () => { this.toast.success('Advance settled.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to settle this advance.')
    });
  }

  /** "Should this month's (and future) payroll cut this advance?" - manual settlement is unaffected either way. */
  toggleRecoverViaPayroll(advance: EmployeeAdvanceResponse, checked: boolean): void {
    this.advanceService.updateRecoverViaPayroll(advance.employeeId, advance.id, checked).subscribe({
      next: () => {
        this.toast.success(checked
          ? 'Payroll will recover this advance from the next calculation onward.'
          : 'Payroll will skip this advance until turned back on - already-paid manual settlements are unaffected.');
        this.load();
      },
      error: err => { this.toast.error(err.error?.message ?? 'Unable to update this setting.'); this.load(); }
    });
  }

  readonly historyAdvance = signal<EmployeeAdvanceResponse | null>(null);
  readonly historyLoading = signal(false);
  readonly recoveryHistory = signal<AdvanceRecoveryTransactionResponse[]>([]);

  openRecoveryHistory(advance: EmployeeAdvanceResponse): void {
    this.historyAdvance.set(advance);
    this.historyLoading.set(true);
    this.advanceService.getRecoveryHistory(advance.employeeId, advance.id).subscribe({
      next: history => { this.recoveryHistory.set(history); this.historyLoading.set(false); },
      error: () => { this.historyLoading.set(false); this.toast.error('Unable to load recovery history.'); }
    });
  }

  closeRecoveryHistory(): void {
    this.historyAdvance.set(null);
  }
}
