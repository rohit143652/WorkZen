import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EmployeeService, EmployeeSearchParams } from '../../../employee_module/services/employee.service';
import { EmployeeResponse } from '../../../employee_module/models/employee.model';
import { EmployeeSearchSelectComponent } from '../../../employee_module/components/employee-search-select/employee-search-select.component';
import { OvertimeService } from '../../services/overtime.service';
import { OvertimeRecord } from '../../models/payroll.model';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';
import { AuthStateService } from '../../../core/services/auth-state.service';

/**
 * The Overtime Register - who did overtime, on which day, how many hours, and how much to pay
 * for it (see backend EmployeeOvertimeRecord/V107-V108 migrations for why this exists and why
 * amount is entered directly here rather than derived from a fixed company-wide rate). A
 * month's payroll Overtime line is always summed from these records' amounts.
 *
 * Deliberately simple: pick an employee, pick a month (not an arbitrary date range - a month is
 * the natural unit here since that's what payroll actually consumes), log entries as they
 * happen with both Hours (informational - how long) and Amount (the actual rupee figure to pay)
 * entered side by side, independently.
 */
@Component({
  selector: 'app-overtime-register',
  standalone: true,
  imports: [CommonModule, FormsModule, EmployeeSearchSelectComponent],
  templateUrl: './overtime-register.component.html'
})
export class OvertimeRegisterComponent {
  private readonly employeeService = inject(EmployeeService);
  private readonly overtimeService = inject(OvertimeService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  readonly authState = inject(AuthStateService);

  readonly canManage = this.authState.hasPermission('OVERTIME_RECORD_MANAGE');

  /** "Log Overtime" and "History" as separate tabs, not both stacked on one page at once -
      much less cluttered, especially for someone who can't even log entries (only READ) and
      would otherwise see a form they can't use sitting above the list they actually want. */
  activeTab: 'log' | 'history' = this.canManage ? 'log' : 'history';

  readonly employees = signal<EmployeeResponse[]>([]);
  selectedEmployeeId: number | null = null;

  private readonly today = new Date();
  readonly todayStr = this.today.toISOString().slice(0, 10);

  /** A single month, not a from/to range - simpler, and matches the unit payroll actually cares about. */
  selectedMonth = `${this.today.getFullYear()}-${String(this.today.getMonth() + 1).padStart(2, '0')}`;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly records = signal<OvertimeRecord[]>([]);

  // New-entry form - Hours and Amount are both entered directly and independently; Amount is
  // what actually feeds payroll, Hours is kept purely as the "how long" record alongside it.
  newDate = this.todayStr;
  newHours: number | null = null;
  newAmount: number | null = null;
  newRemarks = '';

  readonly totalHours = () => this.records().reduce((sum, r) => sum + r.hours, 0);
  readonly totalAmount = () => this.records().reduce((sum, r) => sum + r.amount, 0);

  constructor() {
    this.employeeService.search({ status: 'ACTIVE', page: 0, size: 200 } as EmployeeSearchParams).subscribe(res => this.employees.set(res.content));
  }

  onEmployeeChange(): void {
    if (this.selectedEmployeeId) {
      this.load();
    } else {
      this.records.set([]);
    }
  }

  private monthRange(): { from: string; to: string } {
    const [year, month] = this.selectedMonth.split('-').map(Number);
    const from = `${this.selectedMonth}-01`;
    const lastDay = new Date(year, month, 0).getDate();
    const to = `${this.selectedMonth}-${String(lastDay).padStart(2, '0')}`;
    return { from, to };
  }

  load(): void {
    if (!this.selectedEmployeeId) return;
    this.loading.set(true);
    const { from, to } = this.monthRange();
    this.overtimeService.forEmployee(this.selectedEmployeeId, from, to).subscribe({
      next: records => { this.records.set(records); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  addRecord(): void {
    if (!this.selectedEmployeeId) {
      this.toast.warning('Select an employee first.');
      return;
    }
    if (!this.newHours || this.newHours <= 0) {
      this.toast.warning('Enter a valid number of hours.');
      return;
    }
    if (this.newAmount == null || this.newAmount < 0) {
      this.toast.warning('Enter the amount to pay for this overtime.');
      return;
    }
    this.saving.set(true);
    this.overtimeService.mark({
      employeeId: this.selectedEmployeeId,
      overtimeDate: this.newDate,
      hours: this.newHours,
      amount: this.newAmount,
      remarks: this.newRemarks || undefined
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.newHours = null;
        this.newAmount = null;
        this.newRemarks = '';
        this.toast.success('Overtime recorded successfully.');
        // The entry might land outside the currently-viewed month (e.g. logging yesterday
        // right after midnight into a new month) - jump the view to match what was just
        // logged instead of leaving the admin wondering where their entry went.
        this.selectedMonth = this.newDate.slice(0, 7);
        this.activeTab = 'history';
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to record overtime.');
      }
    });
  }

  async deleteRecord(record: OvertimeRecord): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Delete this overtime entry?',
      message: `Remove ${record.hours} hour(s) / ₹${record.amount} logged for ${record.employeeName} on ${record.overtimeDate}? This cannot be undone.`,
      confirmLabel: 'Delete',
      danger: true
    });
    if (!ok) return;
    this.overtimeService.delete(record.id, record.employeeId).subscribe({
      next: () => { this.toast.success('Overtime entry deleted.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to delete this entry.')
    });
  }
}
