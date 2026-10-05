import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MailOverview, MailOverviewRow, MailSettings, MailSource, MailTestResult } from '../../models/mail-settings.model';
import { MailSettingsService } from '../../services/mail-settings.service';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

/**
 * Super Admin: choose which email address each client company sends from. Every email for a company
 * (employee invitations, password resets, ...) goes out from that company's own sender; a company
 * without one uses the platform default. The password is write-only - never shown or returned;
 * leaving the field blank when updating keeps the saved one.
 */
@Component({
  selector: 'app-mail-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mail-settings.component.html'
})
export class MailSettingsComponent {
  private readonly service = inject(MailSettingsService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly loading = signal(true);
  readonly overview = signal<MailOverview | null>(null);
  /** The scope being edited: a company, or the platform default (id null). Null = nothing open. */
  readonly selected = signal<{ id: number | null; name: string } | null>(null);
  readonly settings = signal<MailSettings | null>(null);
  readonly saving = signal(false);
  readonly testing = signal(false);
  readonly testResult = signal<MailTestResult | null>(null);

  host = '';
  port: number | null = 587;
  username = '';
  fromAddress = '';
  password = '';
  enabled = true;
  testTo = '';

  constructor() {
    this.loadOverview();
  }

  loadOverview(): void {
    this.service.overview().subscribe({
      next: o => { this.overview.set(o); this.loading.set(false); },
      error: err => { this.loading.set(false); this.toast.error(err.error?.message ?? 'Unable to load the email settings.'); }
    });
  }

  sourceLabel(source: MailSource): string {
    switch (source) {
      case 'COMPANY': return 'Own sender';
      case 'PLATFORM': return 'Platform default';
      default: return 'Server variables (MAIL_*)';
    }
  }

  sourceBadge(source: MailSource): string {
    return source === 'COMPANY' ? 'badge-success' : source === 'PLATFORM' ? 'badge-info' : 'badge-muted';
  }

  edit(row: MailOverviewRow): void {
    this.selected.set({ id: row.clientCompanyId, name: row.companyName });
    this.settings.set(null);
    this.testResult.set(null);
    this.testTo = '';
    this.service.get(row.clientCompanyId).subscribe({
      next: s => { this.settings.set(s); this.fillForm(s); },
      error: err => { this.selected.set(null); this.toast.error(err.error?.message ?? 'Unable to load this sender.'); }
    });
  }

  closeEditor(): void {
    this.selected.set(null);
    this.settings.set(null);
  }

  /**
   * Shows the saved values when there are any. Otherwise only the shared server details (host/port)
   * are pre-filled - never another account's username or From address, so a company's own sender
   * can't be saved by accident as a copy of the platform's.
   */
  private fillForm(s: MailSettings): void {
    if (s.hasStoredSettings) {
      this.host = s.host ?? '';
      this.port = s.port;
      this.username = s.username ?? '';
      this.fromAddress = s.fromAddress ?? '';
      this.enabled = s.enabled ?? true;
    } else {
      this.host = s.effectiveHost ?? 'smtp.gmail.com';
      this.port = s.effectivePort ?? 587;
      const isPlatform = s.clientCompanyId == null;
      this.username = isPlatform ? (s.effectiveUsername ?? '') : '';
      this.fromAddress = isPlatform ? (s.effectiveFromAddress ?? '') : '';
      this.enabled = true;
    }
    this.password = '';
  }

  save(): void {
    const sel = this.selected();
    const s = this.settings();
    if (!sel) return;
    if (!this.host.trim() || !this.username.trim() || !this.fromAddress.trim()) {
      this.toast.warning('Mail server, username and From address are all required.');
      return;
    }
    if (this.port == null || this.port < 1 || this.port > 65535) {
      this.toast.warning('Enter a valid port (587 for STARTTLS, 465 for SSL).');
      return;
    }
    if (!s?.passwordSet && !this.password.trim()) {
      this.toast.warning('Enter the password the first time you save.');
      return;
    }

    this.saving.set(true);
    this.service.save(sel.id, {
      host: this.host.trim(),
      port: this.port,
      username: this.username.trim(),
      fromAddress: this.fromAddress.trim(),
      password: this.password.trim() ? this.password : null,
      enabled: this.enabled
    }).subscribe({
      next: saved => {
        this.saving.set(false);
        this.settings.set(saved);
        this.fillForm(saved);
        this.testResult.set(null);
        this.loadOverview();
        this.toast.success(`Saved the sender for ${sel.name}. Send a test email to confirm it works.`);
      },
      error: err => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Unable to save the email sender.'); }
    });
  }

  async remove(): Promise<void> {
    const sel = this.selected();
    if (!sel) return;
    const fallback = sel.id == null
      ? 'The platform default will be removed and the server will go back to the MAIL_* environment variables.'
      : `${sel.name} will stop using its own sender and use the platform default instead.`;
    const ok = await this.confirmDialog.ask({
      title: `Delete the saved sender for ${sel.name}?`,
      message: `The saved account and its password will be removed. ${fallback}`,
      confirmLabel: 'Delete',
      danger: true
    });
    if (!ok) return;
    this.service.remove(sel.id).subscribe({
      next: s => {
        this.settings.set(s);
        this.fillForm(s);
        this.testResult.set(null);
        this.loadOverview();
        this.toast.success(`Saved sender for ${sel.name} deleted.`);
      },
      error: err => this.toast.error(err.error?.message ?? 'Unable to delete the email sender.')
    });
  }

  sendTest(): void {
    const sel = this.selected();
    if (!sel) return;
    if (!this.testTo.trim()) {
      this.toast.warning('Enter the email address to send the test to.');
      return;
    }
    this.testing.set(true);
    this.testResult.set(null);
    this.service.sendTest(sel.id, this.testTo.trim()).subscribe({
      next: r => { this.testing.set(false); this.testResult.set(r); },
      error: err => { this.testing.set(false); this.toast.error(err.error?.message ?? 'Unable to send the test email.'); }
    });
  }
}
