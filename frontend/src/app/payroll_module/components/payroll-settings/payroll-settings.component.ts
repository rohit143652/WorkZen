import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PayrollService } from '../../services/payroll.service';
import { PayrollSettings, PayrollSettingsCreateRequest } from '../../models/payroll.model';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

/**
 * CLIENT_ADMIN only - see PAYROLL_REGISTER_EXPORT permission and the route
 * guard. Effective-dated (architecture refactor Phase 8): "Save" never
 * edits the current configuration in place - it always schedules a NEW
 * configuration effective from a chosen date, so historical/approved
 * payroll can never be affected by a settings change. The current
 * configuration is shown read-only; only a not-yet-effective (future)
 * configuration can be edited or cancelled.
 */
@Component({
  selector: 'app-payroll-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payroll-settings.component.html'
})
export class PayrollSettingsComponent {
  private readonly payrollService = inject(PayrollService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly current = signal<PayrollSettings | null>(null);
  readonly history = signal<PayrollSettings[]>([]);

  readonly showScheduleForm = signal(false);
  readonly today = new Date().toISOString().slice(0, 10);

  effectiveFrom = '';
  epfEnabled = true;
  epfEmployeePercent = 12;
  epfEmployerPercent = 13;

  esiEnabled = true;
  esiEmployeePercent = 0.75;
  esiEmployerPercent = 3.25;
  hasEsiCeiling = true;
  esiWageCeiling = 21000;

  ptEnabled = true;
  professionalTax = 200;

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.payrollService.getSettings().subscribe({
      next: s => { this.current.set(s); this.loading.set(false); this.loadHistory(); },
      error: () => { this.loading.set(false); this.toast.error('Unable to load payroll settings.'); }
    });
  }

  private loadHistory(): void {
    this.payrollService.getHistory().subscribe({
      next: rows => this.history.set(rows),
      error: () => this.toast.error('Unable to load configuration history.')
    });
  }

  get nextScheduled(): PayrollSettings | undefined {
    const today = new Date().toISOString().slice(0, 10);
    return this.history()
      .filter(h => h.status === 'ACTIVE' && (h.effectiveFrom ?? '') > today)
      .sort((a, b) => (a.effectiveFrom ?? '').localeCompare(b.effectiveFrom ?? ''))[0];
  }

  openScheduleForm(): void {
    const c = this.current();
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    this.effectiveFrom = tomorrow.toISOString().slice(0, 10);
    if (c) {
      this.epfEnabled = c.epfEnabled;
      this.epfEmployeePercent = c.epfEmployeePercent;
      this.epfEmployerPercent = c.epfEmployerPercent;
      this.esiEnabled = c.esiEnabled;
      this.esiEmployeePercent = c.esiEmployeePercent;
      this.esiEmployerPercent = c.esiEmployerPercent;
      this.hasEsiCeiling = c.esiWageCeiling != null;
      this.esiWageCeiling = c.esiWageCeiling ?? 21000;
      this.ptEnabled = c.ptEnabled;
      this.professionalTax = c.professionalTax;
    }
    this.showScheduleForm.set(true);
  }

  cancelScheduleForm(): void {
    this.showScheduleForm.set(false);
  }

  save(): void {
    if (!this.effectiveFrom) {
      this.toast.warning('An effective date is required.');
      return;
    }
    const request: PayrollSettingsCreateRequest = {
      effectiveFrom: this.effectiveFrom,
      epfEnabled: this.epfEnabled,
      epfEmployeePercent: this.epfEmployeePercent,
      epfEmployerPercent: this.epfEmployerPercent,
      esiEnabled: this.esiEnabled,
      esiEmployeePercent: this.esiEmployeePercent,
      esiEmployerPercent: this.esiEmployerPercent,
      esiWageCeiling: this.hasEsiCeiling ? this.esiWageCeiling : null,
      ptEnabled: this.ptEnabled,
      professionalTax: this.professionalTax
    };
    this.saving.set(true);
    this.payrollService.createConfig(request).subscribe({
      next: () => {
        this.saving.set(false);
        this.showScheduleForm.set(false);
        this.toast.success(`Payroll configuration scheduled from ${this.effectiveFrom}.`);
        this.load();
      },
      error: err => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Unable to schedule this configuration.'); }
    });
  }

  async cancelFutureConfig(row: PayrollSettings): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Cancel this scheduled configuration?',
      message: `The configuration scheduled for ${row.effectiveFrom} will be cancelled, and whichever configuration it was going to replace stays in effect instead.`,
      confirmLabel: 'Cancel Configuration',
      danger: true
    });
    if (!ok || row.id == null) return;
    this.payrollService.cancelConfig(row.id).subscribe({
      next: () => { this.toast.success('Scheduled configuration cancelled.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to cancel this configuration.')
    });
  }
}
