import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PayrollRunService } from '../../services/payroll-run.service';
import { PayrollRun } from '../../models/payroll-run.model';
import { SiteService } from '../../../site_module/services/site.service';
import { SiteResponse } from '../../../site_module/models/site.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

/**
 * Persisted Payroll Runs (architecture refactor Phase 2) - one run per
 * client+month. Creating a run only makes an empty DRAFT; "Calculate" (on
 * the Details page) is the one explicit action that ever writes payroll
 * numbers - viewing this list, or any run's details, never recalculates
 * anything.
 */
@Component({
  selector: 'app-payroll-processing',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StatusBadgeComponent, HasPermissionDirective],
  templateUrl: './payroll-processing.component.html'
})
export class PayrollProcessingComponent {
  private readonly payrollRunService = inject(PayrollRunService);
  private readonly siteService = inject(SiteService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly sites = signal<SiteResponse[]>([]);
  /** 'ALL' | 'SINGLE' | 'MULTIPLE' (Phase 4: All Sites / Single Site / Multiple Sites). */
  createScope: 'ALL' | 'SINGLE' | 'MULTIPLE' = 'ALL';
  createSelectedSiteIds: number[] = [];

  readonly months = MONTH_NAMES.map((label, index) => ({ label, value: index + 1 }));
  readonly years: number[] = (() => {
    const current = new Date().getFullYear();
    return [current + 1, current, current - 1, current - 2];
  })();
  readonly statuses = ['DRAFT', 'CALCULATED', 'APPROVED', 'PAID', 'CANCELLED'];

  filterYear: number | null = null;
  filterMonth: number | null = null;
  filterStatus = '';

  readonly loading = signal(true);
  readonly runs = signal<PayrollRun[]>([]);

  readonly showCreateForm = signal(false);
  readonly creating = signal(false);
  createYear = new Date().getFullYear();
  createMonth = new Date().getMonth() + 1;

  constructor() {
    this.load();
    this.siteService.list().subscribe(res => this.sites.set(res.content));
  }

  load(): void {
    this.loading.set(true);
    this.payrollRunService.list(this.filterYear ?? undefined, this.filterMonth ?? undefined, this.filterStatus || undefined, 0, 50)
      .subscribe({
        next: page => { this.runs.set(page.content); this.loading.set(false); },
        error: () => { this.loading.set(false); this.toast.error('Unable to load payroll runs.'); }
      });
  }

  openCreateForm(): void {
    this.createYear = new Date().getFullYear();
    this.createMonth = new Date().getMonth() + 1;
    this.createScope = 'ALL';
    this.createSelectedSiteIds = [];
    this.showCreateForm.set(true);
  }

  cancelCreateForm(): void {
    this.showCreateForm.set(false);
  }

  selectSingleSite(value: string): void {
    this.createSelectedSiteIds = value ? [+value] : [];
  }

  toggleSite(siteId: number, checked: boolean): void {
    if (checked) {
      this.createSelectedSiteIds = [...this.createSelectedSiteIds, siteId];
    } else {
      this.createSelectedSiteIds = this.createSelectedSiteIds.filter(id => id !== siteId);
    }
  }

  createRun(): void {
    if (this.createScope !== 'ALL' && this.createSelectedSiteIds.length === 0) {
      this.toast.error('Select at least one site.');
      return;
    }
    this.creating.set(true);
    this.payrollRunService.create({
      year: this.createYear,
      month: this.createMonth,
      siteIds: this.createScope === 'ALL' ? undefined : this.createSelectedSiteIds
    }).subscribe({
      next: run => {
        this.creating.set(false);
        this.showCreateForm.set(false);
        this.toast.success(`Payroll run created for ${run.monthLabel}.`);
        this.router.navigate(['/payroll/runs', run.id]);
      },
      error: err => {
        this.creating.set(false);
        this.toast.error(err.error?.message ?? 'Unable to create this payroll run.');
      }
    });
  }
}
