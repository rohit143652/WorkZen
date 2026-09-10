import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ClientCompanyService } from '../../services/client-company.service';
import { ClientCompanyResponse, CompanyFeatureResponse } from '../../models/client-company.model';
import { ToastService } from '../../../shared/services/toast.service';

/**
 * Super Admin "Manage Features" screen - company-wise module/feature access (see
 * FeatureAccessService on the backend for how this is actually enforced, not just displayed).
 * Categorized toggles matching FeatureCode.CATALOG exactly, so adding a new feature code on the
 * backend automatically shows up here with zero frontend changes needed.
 */
@Component({
  selector: 'app-manage-features',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './manage-features.component.html',
  styleUrl: './manage-features.component.css'
})
export class ManageFeaturesComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly clientCompanyService = inject(ClientCompanyService);
  private readonly toast = inject(ToastService);

  private readonly companyId = Number(this.route.snapshot.paramMap.get('id'));

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly company = signal<ClientCompanyResponse | null>(null);
  readonly data = signal<CompanyFeatureResponse | null>(null);
  /** Local editable copy - only sent on Save, so navigating away without saving discards changes cleanly. */
  readonly pending = signal<Record<string, boolean>>({});

  readonly featureLabels: Record<string, string> = {
    EMPLOYEE_MANAGEMENT: 'Employee Management',
    LEAVE_MANAGEMENT: 'Leave Management',
    RECRUITMENT: 'Recruitment',
    ATTENDANCE_MANAGEMENT: 'Attendance Management',
    EMPLOYEE_SELF_ATTENDANCE: 'Employee Self Attendance (Check-In/Check-Out)',
    ADMIN_MANUAL_ATTENDANCE: 'Admin Manual Attendance',
    SUPERVISOR_MANUAL_ATTENDANCE: 'Supervisor Manual Attendance',
    HR_MANUAL_ATTENDANCE: 'HR Manual Attendance',
    SHIFT_MANAGEMENT: 'Shift Management',
    HOLIDAY_CALENDAR: 'Holiday Calendar',
    SALARY_MANAGEMENT: 'Salary Management',
    PAYROLL: 'Payroll',
    EXPENSE_MANAGEMENT: 'Expense Management',
    REPORTS: 'Reports',
    ASSET_MANAGEMENT: 'Asset Management'
  };

  constructor() {
    this.clientCompanyService.getById(this.companyId).subscribe(c => this.company.set(c));
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.clientCompanyService.getFeatures(this.companyId).subscribe({
      next: res => {
        this.data.set(res);
        this.pending.set({ ...res.features });
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  label(code: string): string {
    return this.featureLabels[code] ?? code;
  }

  isEnforced(code: string): boolean {
    return this.data()?.enforcedCodes.includes(code) ?? false;
  }

  isChecked(code: string): boolean {
    return this.pending()[code] ?? true;
  }

  /** The plan-default/override/effective breakdown for one code, for the "Plan: Enabled, Override: No Override, Effective: Enabled" display. */
  detailFor(code: string) {
    return this.data()?.details.find(d => d.featureCode === code) ?? null;
  }

  toggle(code: string): void {
    this.pending.update(p => ({ ...p, [code]: !(p[code] ?? true) }));
  }

  save(): void {
    this.saving.set(true);
    this.clientCompanyService.updateFeatures(this.companyId, { features: this.pending() }).subscribe({
      next: features => {
        this.saving.set(false);
        this.pending.set({ ...features });
        this.toast.success('Company features updated successfully.');
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to update company features.');
      }
    });
  }
}
