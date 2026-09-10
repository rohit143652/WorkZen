import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PayrollRunService } from '../../services/payroll-run.service';
import { extractBlobErrorMessage } from '../../../shared/utils/blob-error.util';
import { PayrollRun, PayrollRunEmployeeResult } from '../../models/payroll-run.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';
import { FeatureStateService } from '../../../core/services/feature-state.service';
import { EmployeeAdvanceService } from '../../../advance_module/services/employee-advance.service';

/**
 * Read-only view of a persisted Payroll Run + its employee results, plus
 * the explicit workflow actions (Calculate/Approve/Mark Paid/Cancel) and
 * the manual Advance/Uniform + Allowance adjustment editor (architecture
 * refactor Phase 4: this used to live on the Monthly Attendance Report,
 * moved here since it's a Payroll concern, not an attendance one).
 * Loading this page never recalculates anything - "Calculate" is the only
 * button that writes payroll numbers, and it's disabled once the run is
 * APPROVED/PAID/CANCELLED. Editing an adjustment only takes effect the
 * next time "Calculate"/"Recalculate" is pressed - it never silently
 * changes already-displayed numbers.
 */
@Component({
  selector: 'app-payroll-run-details',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StatusBadgeComponent, HasPermissionDirective],
  templateUrl: './payroll-run-details.component.html'
})
export class PayrollRunDetailsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly payrollRunService = inject(PayrollRunService);
  private readonly toast = inject(ToastService);
  private readonly featureState = inject(FeatureStateService);
  private readonly employeeAdvanceService = inject(EmployeeAdvanceService);
  readonly overtimeFeatureEnabled = () => this.featureState.isEnabled('OVERTIME_MANAGEMENT');
  readonly togglingAdvanceForEmployeeId = signal<number | null>(null);

  /** "Skip this month" - only offered when the employee has EXACTLY ONE advance eligible for
      payroll recovery (see PayrollRunEmployeeResult.singleEligibleAdvanceId / backend
      EmployeeAdvanceService.getSingleEligibleAdvanceId()). Pauses that advance's payroll
      recovery via the SAME "Cut from Payroll" mechanism the Employee Advances page uses -
      correctly reflected in the real ledger (outstanding stays unchanged this month), not a
      cosmetic override - then recalculates so the run's figures update immediately. Re-enabling
      it afterward (for next month) is done from the Employee Advances page, same as always. */
  async skipAdvanceThisMonth(row: PayrollRunEmployeeResult): Promise<void> {
    if (!row.singleEligibleAdvanceId) return;
    const ok = await this.confirmDialog.ask({
      title: "Skip this employee's advance recovery this month?",
      message: `No amount will be cut from ${row.employeeName}'s pay this month for this advance - the outstanding balance stays the same and recovery resumes automatically next month (or whenever you turn "Cut from Payroll" back on from Employee Advances).`,
      confirmLabel: 'Skip This Month'
    });
    if (!ok) return;
    this.togglingAdvanceForEmployeeId.set(row.employeeId);
    this.employeeAdvanceService.updateRecoverViaPayroll(row.employeeId, row.singleEligibleAdvanceId, false).subscribe({
      next: () => {
        this.payrollRunService.calculate(this.runId).subscribe({
          next: () => {
            this.togglingAdvanceForEmployeeId.set(null);
            this.toast.success(`Advance recovery skipped for ${row.employeeName} this month - run recalculated.`);
            this.load();
          },
          error: err => {
            this.togglingAdvanceForEmployeeId.set(null);
            this.toast.error(err.error?.message ?? 'Skipped, but the run could not be recalculated automatically - use Recalculate above.');
            this.load();
          }
        });
      },
      error: err => {
        this.togglingAdvanceForEmployeeId.set(null);
        this.toast.error(err.error?.message ?? 'Unable to update this advance.');
      }
    });
  }
  private readonly confirmDialog = inject(ConfirmDialogService);

  private readonly runId = Number(this.route.snapshot.paramMap.get('id'));

  readonly loading = signal(true);
  readonly run = signal<PayrollRun | null>(null);
  readonly employees = signal<PayrollRunEmployeeResult[]>([]);
  readonly actionInProgress = signal(false);

  readonly editingEmployeeId = signal<number | null>(null);
  readonly savingEmployeeId = signal<number | null>(null);
  /** Sent as-is to preserve the request contract (backend still requires this field) - there's
      no UI to edit it any more (see "advance deduction madhun manual kadhun tak" request: Manual
      was removed from this screen entirely, only automatic Advance Recovery is shown now). */
  private currentOtherManualDeduction = 0;
  editAllowance = 0;

  readonly showCancelForm = signal(false);
  cancelReasonText = '';
  readonly showReopenForm = signal(false);
  reopenReasonText = '';

  readonly recalculable = () => {
    const r = this.run();
    return r != null && (r.status === 'DRAFT' || r.status === 'CALCULATED');
  };

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.payrollRunService.getById(this.runId).subscribe({
      next: run => {
        this.run.set(run);
        this.loading.set(false);
        if (run.status !== 'DRAFT') {
          this.loadEmployees();
        }
      },
      error: () => { this.loading.set(false); this.toast.error('Unable to load this payroll run.'); }
    });
  }

  private loadEmployees(): void {
    this.payrollRunService.getEmployees(this.runId).subscribe({
      next: page => this.employees.set(page.content),
      error: () => this.toast.error('Unable to load employee payroll results.')
    });
  }

  calculate(): void {
    this.actionInProgress.set(true);
    this.payrollRunService.calculate(this.runId).subscribe({
      next: () => {
        this.actionInProgress.set(false);
        this.toast.success('Payroll calculated.');
        this.load();
      },
      error: err => {
        this.actionInProgress.set(false);
        this.toast.error(err.error?.message ?? 'Unable to calculate this payroll run.');
      }
    });
  }

  async approve(): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Approve this payroll run?',
      message: 'Once approved, it can no longer be recalculated - only an authorized reopen action can change it after this.',
      confirmLabel: 'Approve'
    });
    if (!ok) return;
    this.actionInProgress.set(true);
    this.payrollRunService.approve(this.runId).subscribe({
      next: () => { this.actionInProgress.set(false); this.toast.success('Payroll run approved.'); this.load(); },
      error: err => { this.actionInProgress.set(false); this.toast.error(err.error?.message ?? 'Unable to approve this payroll run.'); }
    });
  }

  async markPaid(): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Mark this payroll run as paid?',
      message: 'This records that salaries for this month have actually been disbursed. Paid payroll cannot be normally modified afterward.',
      confirmLabel: 'Mark Paid'
    });
    if (!ok) return;
    this.actionInProgress.set(true);
    this.payrollRunService.markPaid(this.runId).subscribe({
      next: () => { this.actionInProgress.set(false); this.toast.success('Payroll run marked as paid.'); this.load(); },
      error: err => { this.actionInProgress.set(false); this.toast.error(err.error?.message ?? 'Unable to mark this payroll run as paid.'); }
    });
  }

  openCancelForm(): void {
    this.cancelReasonText = '';
    this.showCancelForm.set(true);
  }

  cancelCancelForm(): void {
    this.showCancelForm.set(false);
  }

  confirmCancel(): void {
    if (!this.cancelReasonText.trim()) {
      this.toast.warning('A cancellation reason is required.');
      return;
    }
    this.actionInProgress.set(true);
    this.payrollRunService.cancel(this.runId, this.cancelReasonText.trim()).subscribe({
      next: () => {
        this.actionInProgress.set(false);
        this.showCancelForm.set(false);
        this.toast.success('Payroll run cancelled.');
        this.load();
      },
      error: err => { this.actionInProgress.set(false); this.toast.error(err.error?.message ?? 'Unable to cancel this payroll run.'); }
    });
  }

  openReopenForm(): void {
    this.reopenReasonText = '';
    this.showReopenForm.set(true);
  }

  cancelReopenForm(): void {
    this.showReopenForm.set(false);
  }

  confirmReopen(): void {
    if (!this.reopenReasonText.trim()) {
      this.toast.warning('A reopen reason is required.');
      return;
    }
    this.actionInProgress.set(true);
    this.payrollRunService.reopen(this.runId, this.reopenReasonText.trim()).subscribe({
      next: () => {
        this.actionInProgress.set(false);
        this.showReopenForm.set(false);
        this.toast.success('Payroll run reopened - it is now CALCULATED and can be reviewed/recalculated.');
        this.load();
      },
      error: err => { this.actionInProgress.set(false); this.toast.error(err.error?.message ?? 'Unable to reopen this payroll run.'); }
    });
  }

  startEditAdjustment(row: PayrollRunEmployeeResult): void {
    this.editingEmployeeId.set(row.employeeId);
    this.currentOtherManualDeduction = row.otherManualDeduction;
    this.editAllowance = row.allowance;
  }

  cancelEditAdjustment(): void {
    this.editingEmployeeId.set(null);
  }

  saveAdjustment(row: PayrollRunEmployeeResult): void {
    if (this.editAllowance < 0) {
      this.toast.warning('Allowance must be 0 or more.');
      return;
    }
    this.savingEmployeeId.set(row.employeeId);
    this.payrollRunService.setEmployeeAdjustment(this.runId, row.employeeId, this.currentOtherManualDeduction, this.editAllowance).subscribe({
      next: () => {
        this.editingEmployeeId.set(null);
        // Recalculate the whole run immediately, so this employee's (and everyone's) totals
        // refresh right away instead of the admin having to scroll up and click Recalculate
        // separately. Net Pay/deductions are always computed for the full run together, never
        // per employee, so a full recalculation is the correct - and only - way to apply this.
        this.payrollRunService.calculate(this.runId).subscribe({
          next: () => {
            this.savingEmployeeId.set(null);
            this.toast.success(`Adjustment saved for ${row.employeeName} - run recalculated.`);
            this.load();
          },
          error: err => {
            this.savingEmployeeId.set(null);
            this.toast.error(err.error?.message ?? 'Adjustment saved, but the run could not be recalculated automatically - use Recalculate above.');
            this.load();
          }
        });
      },
      error: err => {
        this.savingEmployeeId.set(null);
        this.toast.error(err.error?.message ?? 'Unable to save this adjustment.');
      }
    });
  }

  readonly downloadingRegister = signal(false);

  downloadSalaryRegister(): void {
    this.downloadingRegister.set(true);
    this.payrollRunService.downloadSalaryRegister(this.runId).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `Salary-Register-${this.runId}.xlsx`;
        anchor.click();
        window.URL.revokeObjectURL(url);
        this.downloadingRegister.set(false);
        this.toast.success('Salary register downloaded.');
      },
      error: err => {
        this.downloadingRegister.set(false);
        extractBlobErrorMessage(err, 'Unable to generate the salary register.').then(message => this.toast.error(message));
      }
    });
  }
}
