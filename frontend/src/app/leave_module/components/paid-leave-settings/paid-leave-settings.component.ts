import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PaidLeaveService } from '../../services/paid-leave.service';
import { EmployeeLeaveSummary, PaidLeaveConfig, PaidLeaveConfigCreateRequest } from '../../models/paid-leave.model';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';

/**
 * CLIENT_ADMIN only - see PAID_LEAVE_CONFIG_READ/UPDATE and PAID_LEAVE_READ
 * permissions and the route guard. Effective-dated (architecture refactor
 * Phase 9): "Save" never edits the current policy in place - it always
 * schedules a NEW policy effective from a chosen date, so historical
 * leave balances can never be affected by a policy change. The current
 * policy is shown read-only; only a not-yet-effective (future) policy can
 * be edited or cancelled.
 */
@Component({
  selector: 'app-paid-leave-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent],
  templateUrl: './paid-leave-settings.component.html'
})
export class PaidLeaveSettingsComponent {
  private readonly paidLeaveService = inject(PaidLeaveService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly current = signal<PaidLeaveConfig | null>(null);
  readonly history = signal<PaidLeaveConfig[]>([]);
  readonly today = new Date().toISOString().slice(0, 10);

  readonly showScheduleForm = signal(false);
  effectiveFrom = '';
  monthlyPaidLeave = 2;
  enabled = true;
  allowCarryForward = true;
  hasMaximumCarryForward = false;
  maximumCarryForward = 30;
  resetAnnually = false;

  // ---- Employee leave balances overview ----
  readonly balancesLoading = signal(true);
  readonly balances = signal<EmployeeLeaveSummary[]>([]);
  employeeSearch = '';
  readonly filteredBalances = computed(() => {
    const term = this.employeeSearch.trim().toLowerCase();
    if (!term) return this.balances();
    return this.balances().filter(b =>
      b.employeeCode.toLowerCase().includes(term) || b.employeeName.toLowerCase().includes(term));
  });

  get nextScheduled(): PaidLeaveConfig | undefined {
    return this.history()
      .filter(h => h.status === 'ACTIVE' && (h.effectiveFrom ?? '') > this.today)
      .sort((a, b) => (a.effectiveFrom ?? '').localeCompare(b.effectiveFrom ?? ''))[0];
  }

  constructor() {
    this.load();

    this.paidLeaveService.listEmployeeBalances().subscribe({
      next: list => { this.balances.set(list); this.balancesLoading.set(false); },
      error: () => this.balancesLoading.set(false)
    });
  }

  private load(): void {
    this.loading.set(true);
    this.paidLeaveService.getConfig().subscribe({
      next: config => { this.current.set(config); this.loading.set(false); this.loadHistory(); },
      error: () => { this.loading.set(false); this.toast.error('Unable to load the leave policy.'); }
    });
  }

  private loadHistory(): void {
    this.paidLeaveService.getConfigHistory().subscribe({
      next: rows => this.history.set(rows),
      error: () => this.toast.error('Unable to load policy history.')
    });
  }

  openScheduleForm(): void {
    const c = this.current();
    this.prefillScheduleForm(c);
    this.showScheduleForm.set(true);
  }

  /**
   * Reuses a cancelled (or any other historical) row's settings as the starting point for a
   * fresh "Schedule New Policy" - this does NOT un-cancel the old row itself (its CANCELLED
   * status and history stay exactly as they are, per the "no destructive history edits" rule),
   * it just saves you re-typing the same values into a brand new policy.
   */
  reactivateConfig(row: PaidLeaveConfig): void {
    this.prefillScheduleForm(row);
    this.showScheduleForm.set(true);
    this.toast.success('Pre-filled from the cancelled policy - review and Schedule to make it active again.');
  }

  private prefillScheduleForm(c: PaidLeaveConfig | null): void {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    this.effectiveFrom = tomorrow.toISOString().slice(0, 10);
    if (c) {
      this.monthlyPaidLeave = c.monthlyPaidLeave;
      this.enabled = c.enabled;
      this.allowCarryForward = c.allowCarryForward;
      this.hasMaximumCarryForward = c.maximumCarryForward != null;
      this.maximumCarryForward = c.maximumCarryForward ?? 30;
      this.resetAnnually = c.resetAnnually;
    } else {
      this.monthlyPaidLeave = 2;
      this.enabled = true;
      this.allowCarryForward = true;
      this.hasMaximumCarryForward = false;
      this.maximumCarryForward = 30;
      this.resetAnnually = false;
    }
  }

  cancelScheduleForm(): void {
    this.showScheduleForm.set(false);
  }

  save(): void {
    if (!this.effectiveFrom) {
      this.toast.warning('An effective date is required.');
      return;
    }
    const request: PaidLeaveConfigCreateRequest = {
      effectiveFrom: this.effectiveFrom,
      monthlyPaidLeave: this.monthlyPaidLeave,
      enabled: this.enabled,
      allowCarryForward: this.allowCarryForward,
      maximumCarryForward: this.hasMaximumCarryForward ? this.maximumCarryForward : null,
      resetAnnually: this.resetAnnually
    };
    this.saving.set(true);
    this.paidLeaveService.createConfig(request).subscribe({
      next: () => {
        this.saving.set(false);
        this.showScheduleForm.set(false);
        this.toast.success(`Leave policy scheduled from ${this.effectiveFrom}. Historical months are unaffected.`);
        this.load();
      },
      error: err => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Unable to schedule this policy.'); }
    });
  }

  async cancelFutureConfig(row: PaidLeaveConfig): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Cancel this scheduled policy?',
      message: `The policy scheduled for ${row.effectiveFrom} will be cancelled, and whichever policy it was going to replace stays in effect instead.`,
      confirmLabel: 'Cancel Policy',
      danger: true
    });
    if (!ok || row.id == null) return;
    this.paidLeaveService.cancelConfig(row.id).subscribe({
      next: () => { this.toast.success('Scheduled policy cancelled.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to cancel this policy.')
    });
  }
}
