import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EmployeeService } from '../../services/employee.service';
import { EmployeeResponse } from '../../models/employee.model';
import { RoleService } from '../../../role_module/services/role.service';
import { RoleOption } from '../../../role_module/models/role.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';
import { SiteService } from '../../../site_module/services/site.service';
import { SiteResponse } from '../../../site_module/models/site.model';
import { EmployeeAssignmentService } from '../../../employee_assignment_module/services/employee-assignment.service';
import { EmployeeAssignmentResponse } from '../../../employee_assignment_module/models/employee-assignment.model';
import { EmployeeSalaryStructureService } from '../../../salary_structure_module/services/employee-salary-structure.service';
import { SalaryStructureService } from '../../../salary_structure_module/services/salary-structure.service';
import { EmployeeSalaryStructureResponse } from '../../../salary_structure_module/models/employee-salary-structure.model';
import { SalaryStructureResponse } from '../../../salary_structure_module/models/salary-structure.model';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { EmployeePaidLeaveComponent } from '../../../leave_module/components/employee-paid-leave/employee-paid-leave.component';
import { EmployeeAdvancesComponent } from '../../../advance_module/components/employee-advances/employee-advances.component';
import { PayrollService } from '../../../payroll_module/services/payroll.service';
import { extractBlobErrorMessage } from '../../../shared/utils/blob-error.util';

@Component({
  selector: 'app-employee-details',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink, StatusBadgeComponent, HasPermissionDirective, EmployeePaidLeaveComponent, EmployeeAdvancesComponent],
  templateUrl: './employee-details.component.html',
  styleUrl: './employee-details.component.css'
})
export class EmployeeDetailsComponent {
  private readonly employeeService = inject(EmployeeService);
  private readonly roleService = inject(RoleService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly siteService = inject(SiteService);
  private readonly assignmentService = inject(EmployeeAssignmentService);
  private readonly employeeSalaryStructureService = inject(EmployeeSalaryStructureService);
  private readonly salaryStructureService = inject(SalaryStructureService);
  private readonly payrollService = inject(PayrollService);
  readonly authState = inject(AuthStateService);

  // ---- Payslip download (admin picks any month/year for this employee) ----
  readonly payslipMonths = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ].map((label, index) => ({ label, value: index + 1 }));
  readonly payslipYears: number[] = (() => {
    const current = new Date().getFullYear();
    return [current, current - 1, current - 2];
  })();
  payslipMonth = new Date().getMonth() + 1;
  payslipYear = new Date().getFullYear();
  readonly downloadingPayslip = signal(false);

  downloadPayslip(employeeId: number): void {
    this.downloadingPayslip.set(true);
    this.payrollService.downloadEmployeePayslip(employeeId, this.payslipYear, this.payslipMonth).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `Payslip-${employeeId}-${this.payslipMonth}-${this.payslipYear}.pdf`;
        anchor.click();
        window.URL.revokeObjectURL(url);
        this.downloadingPayslip.set(false);
        this.toast.success('Payslip downloaded.');
      },
      error: err => {
        this.downloadingPayslip.set(false);
        extractBlobErrorMessage(err, 'Unable to generate this payslip - that month may not have been approved yet.')
          .then(message => this.toast.error(message));
      }
    });
  }

  readonly assignmentHistory = signal<EmployeeAssignmentResponse[]>([]);
  readonly sites = signal<SiteResponse[]>([]);
  readonly showTransferForm = signal(false);
  readonly transferring = signal(false);

  readonly currentSalaryStructure = signal<EmployeeSalaryStructureResponse | null>(null);
  readonly salaryHistory = signal<EmployeeSalaryStructureResponse[]>([]);
  readonly availableSalaryStructures = signal<SalaryStructureResponse[]>([]);
  readonly showAssignSalaryForm = signal(false);
  readonly showSalaryHistory = signal(false);
  readonly assigningSalary = signal(false);
  readonly assignSalaryForm = this.fb.nonNullable.group({
    salaryStructureId: [null as number | null, Validators.required],
    effectiveFrom: [new Date().toISOString().slice(0, 10), Validators.required]
  });

  readonly transferForm = this.fb.nonNullable.group({
    toSiteId: [null as number | null, Validators.required],
    effectiveDate: [new Date().toISOString().slice(0, 10), Validators.required],
    reason: ['']
  });

  readonly employee = signal<EmployeeResponse | null>(null);
  readonly loading = signal(true);
  readonly roles = signal<RoleOption[]>([]);
  readonly showEnableLoginForm = signal(false);
  readonly savingLogin = signal(false);
  readonly lastTempPassword = signal<string | null>(null);

  readonly enableLoginForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    roleId: [null as number | null, Validators.required]
  });

  private id!: number;

  constructor() {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.roleService.list().subscribe(roles => this.roles.set(roles));
    this.siteService.list().subscribe(res => this.sites.set(res.content));
    this.loadAssignmentHistory();
    this.load();
    if (this.authState.hasPermission('SALARY_STRUCTURE_READ')) {
      this.loadCurrentSalaryStructure();
    }
  }

  private loadCurrentSalaryStructure(): void {
    this.employeeSalaryStructureService.current(this.id).subscribe({
      next: current => this.currentSalaryStructure.set(current),
      error: () => { /* non-fatal: salary panel is secondary */ }
    });
  }

  openAssignSalaryForm(): void {
    this.showAssignSalaryForm.set(true);
    if (this.availableSalaryStructures().length === 0) {
      this.salaryStructureService.list(0, 100).subscribe(res =>
        this.availableSalaryStructures.set(res.content.filter(s => s.status === 'ACTIVE')));
    }
  }

  submitAssignSalary(): void {
    if (this.assignSalaryForm.invalid) {
      this.assignSalaryForm.markAllAsTouched();
      return;
    }
    this.assigningSalary.set(true);
    const raw = this.assignSalaryForm.getRawValue();
    this.employeeSalaryStructureService.assign(this.id, {
      salaryStructureId: raw.salaryStructureId!,
      effectiveFrom: raw.effectiveFrom
    }).subscribe({
      next: current => {
        this.currentSalaryStructure.set(current);
        this.showAssignSalaryForm.set(false);
        this.assigningSalary.set(false);
        this.toast.success('Salary structure assigned successfully.');
        if (this.showSalaryHistory()) this.loadSalaryHistory();
      },
      error: err => {
        this.assigningSalary.set(false);
        this.toast.error(err.error?.message ?? 'Unable to assign salary structure.');
      }
    });
  }

  toggleSalaryHistory(): void {
    this.showSalaryHistory.update(v => !v);
    if (this.showSalaryHistory() && this.salaryHistory().length === 0) {
      this.loadSalaryHistory();
    }
  }

  private loadSalaryHistory(): void {
    this.employeeSalaryStructureService.history(this.id).subscribe(history => this.salaryHistory.set(history));
  }

  loadAssignmentHistory(): void {
    this.assignmentService.historyForEmployee(this.id).subscribe({
      next: history => this.assignmentHistory.set(history),
      error: () => { /* non-fatal: assignment history is a secondary panel */ }
    });
  }

  currentAssignment(): EmployeeAssignmentResponse | undefined {
    return this.assignmentHistory().find(a => a.status === 'ACTIVE');
  }

  submitTransfer(): void {
    if (this.transferForm.invalid) {
      this.transferForm.markAllAsTouched();
      return;
    }
    this.transferring.set(true);
    this.assignmentService.transfer(this.id, this.transferForm.getRawValue()).subscribe({
      next: () => {
        this.transferring.set(false);
        this.showTransferForm.set(false);
        this.toast.success('Employee transferred successfully.');
        this.loadAssignmentHistory();
      },
      error: err => {
        this.transferring.set(false);
        this.toast.error(err.error?.message ?? 'Unable to transfer employee.');
      }
    });
  }

  load(): void {
    this.loading.set(true);
    this.employeeService.getById(this.id).subscribe({
      next: emp => { this.employee.set(emp); this.loading.set(false); },
      error: () => { this.toast.error('Unable to load employee.'); this.loading.set(false); }
    });
  }

  openEnableLoginForm(): void {
    this.showEnableLoginForm.set(true);
  }

  submitEnableLogin(): void {
    if (this.enableLoginForm.invalid) {
      this.enableLoginForm.markAllAsTouched();
      return;
    }
    this.savingLogin.set(true);
    this.employeeService.enableLogin(this.id, this.enableLoginForm.getRawValue()).subscribe({
      next: emp => {
        this.employee.set(emp);
        this.showEnableLoginForm.set(false);
        this.savingLogin.set(false);
        this.toast.success('Login access enabled successfully.');
      },
      error: err => {
        this.savingLogin.set(false);
        this.toast.error(err.error?.message ?? 'Unable to enable login access.');
      }
    });
  }

  async disableLogin(): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Disable login access?',
      message: 'This will immediately sign the employee out and prevent further logins. Continue?',
      confirmLabel: 'Disable',
      danger: true
    });
    if (!ok) return;
    this.employeeService.disableLogin(this.id).subscribe({
      next: emp => { this.employee.set(emp); this.toast.success('Login access disabled successfully.'); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to disable login access.')
    });
  }

  changeRole(roleId: string): void {
    const id = Number(roleId);
    if (!id) return;
    this.employeeService.assignRole(this.id, { roleId: id }).subscribe({
      next: emp => { this.employee.set(emp); this.toast.success('Role updated successfully.'); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to update role.')
    });
  }

  async resetPassword(): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Reset password?',
      message: 'A new temporary password will be generated. The employee will be required to change it on next login.',
      confirmLabel: 'Reset Password'
    });
    if (!ok) return;
    this.employeeService.resetPassword(this.id).subscribe({
      next: temp => { this.lastTempPassword.set(temp); this.toast.success('Temporary password issued.'); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to reset password.')
    });
  }

  /** Purely a display formatter - spaces every 4 digits ("123456789012" -> "1234 5678 9012") for readability; the stored value in the database is always the plain 12 digits. */
  formatAadhar(value: string | undefined): string {
    if (!value) return '-';
    return value.replace(/(\d{4})(?=\d)/g, '$1 ');
  }
}
