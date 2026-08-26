import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EmployeeService } from '../../services/employee.service';
import { RoleService } from '../../../role_module/services/role.service';
import { RoleOption } from '../../../role_module/models/role.model';
import { DepartmentService } from '../../../department_module/services/department.service';
import { DepartmentResponse } from '../../../department_module/models/department.model';
import { DesignationService } from '../../../designation_module/services/designation.service';
import { DesignationResponse } from '../../../designation_module/models/designation.model';
import { SalaryStructureService } from '../../../salary_structure_module/services/salary-structure.service';
import { SalaryStructureResponse } from '../../../salary_structure_module/models/salary-structure.model';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { ToastService } from '../../../shared/services/toast.service';
import { EmployeeResponse } from '../../models/employee.model';

function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirm = control.get('confirmPassword')?.value;
  if (!password || !confirm) return null;
  return password === confirm ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './employee-form.component.html',
  styleUrl: './employee-form.component.css'
})
export class EmployeeFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly employeeService = inject(EmployeeService);
  private readonly roleService = inject(RoleService);
  private readonly departmentService = inject(DepartmentService);
  private readonly designationService = inject(DesignationService);
  private readonly salaryStructureService = inject(SalaryStructureService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly authState = inject(AuthStateService);

  readonly roles = signal<RoleOption[]>([]);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly designations = signal<DesignationResponse[]>([]);
  readonly salaryStructures = signal<SalaryStructureResponse[]>([]);
  readonly saving = signal(false);
  readonly loading = signal(false);
  readonly isEditMode = signal(false);
  readonly employeeId = signal<number | null>(null);
  readonly existingLoginEnabled = signal(false);

  // Only used in edit mode, when the employee has no login yet - see openEnableLoginForm().
  readonly showEnableLoginForm = signal(false);
  readonly savingLogin = signal(false);
  readonly enableLoginForm = this.fb.nonNullable.group(
    {
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
      roleId: [null as number | null, Validators.required]
    },
    { validators: passwordsMatchValidator }
  );

  readonly form = this.fb.nonNullable.group({
    employeeCode: [''],
    firstName: ['', Validators.required],
    middleName: [''],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    mobileNumber: [''],
    alternateMobileNumber: [''],
    dateOfBirth: [''],
    gender: [''],
    joiningDate: ['', Validators.required],
    department: ['', Validators.required],
    designation: ['', Validators.required],
    employmentType: [''],
    address: [''],
    city: [''],
    state: [''],
    country: [''],
    pincode: [''],
    pfApplicable: [false],
    esiApplicable: [false],
    ptApplicable: [false],
    salaryStructureId: [null as number | null],
    salaryEffectiveFrom: [''],
    enableLogin: [false],
    loginAccess: this.fb.nonNullable.group(
      {
        username: [''],
        password: [''],
        confirmPassword: [''],
        roleId: [null as number | null]
      },
      { validators: passwordsMatchValidator }
    )
  });

  constructor() {
    this.roleService.list().subscribe(roles => this.roles.set(roles));
    this.departmentService.list().subscribe(list => this.departments.set(list));
    this.designationService.list().subscribe(list => this.designations.set(list));
    if (this.authState.hasPermission('EMPLOYEE_SALARY_UPDATE')) {
      this.salaryStructureService.listActive().subscribe(list => this.salaryStructures.set(list));
    }

    this.form.controls.enableLogin.valueChanges.subscribe(enabled => this.applyLoginValidators(enabled));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode.set(true);
      this.employeeId.set(Number(idParam));
      this.loading.set(true);
      this.form.controls.employeeCode.disable();
      this.employeeService.getById(Number(idParam)).subscribe({
        next: emp => { this.patchForm(emp); this.loading.set(false); },
        error: () => { this.toast.error('Unable to load employee.'); this.loading.set(false); }
      });
    } else {
      this.form.controls.employeeCode.disable();
      this.employeeService.nextCode().subscribe({
        next: code => this.form.controls.employeeCode.setValue(code),
        error: () => this.toast.error('Unable to generate the next employee code.')
      });
    }
  }

  private applyLoginValidators(enabled: boolean): void {
    const group = this.form.controls.loginAccess;
    if (enabled) {
      group.controls.username.setValidators([Validators.required, Validators.minLength(3)]);
      group.controls.password.setValidators([Validators.required, Validators.minLength(8)]);
      group.controls.confirmPassword.setValidators([Validators.required]);
      group.controls.roleId.setValidators([Validators.required]);
    } else {
      group.controls.username.clearValidators();
      group.controls.password.clearValidators();
      group.controls.confirmPassword.clearValidators();
      group.controls.roleId.clearValidators();
    }
    Object.values(group.controls).forEach(c => c.updateValueAndValidity());
  }

  private patchForm(emp: EmployeeResponse): void {
    this.existingLoginEnabled.set(!!emp.userId);

    // If this employee's current department/designation was since deactivated (or is a
    // legacy free-text value predating the master lists), it won't be in the active-only
    // dropdown fetched in the constructor. Inject it so the field still shows correctly
    // and isn't silently blanked out or forced to change on an unrelated edit.
    if (emp.department && !this.departments().some(d => d.name === emp.department)) {
      this.departments.update(list => [...list, { id: -1, name: emp.department, status: 'INACTIVE', employeeCount: 0 }]);
    }
    if (emp.designation && !this.designations().some(d => d.name === emp.designation)) {
      this.designations.update(list => [...list, {
        id: -1, name: emp.designation, status: 'INACTIVE', employeeCount: 0
      }]);
    }
    if (emp.currentSalaryStructureId && !this.salaryStructures().some(s => s.id === emp.currentSalaryStructureId)) {
      // The employee's current structure may have since been deactivated - still show it selected.
      this.salaryStructureService.getById(emp.currentSalaryStructureId).subscribe(s => {
        this.salaryStructures.update(list => [...list, s]);
      });
    }

    this.form.patchValue({
      employeeCode: emp.employeeCode,
      firstName: emp.firstName,
      middleName: emp.middleName ?? '',
      lastName: emp.lastName,
      email: emp.email,
      mobileNumber: emp.mobileNumber ?? '',
      alternateMobileNumber: emp.alternateMobileNumber ?? '',
      dateOfBirth: emp.dateOfBirth ?? '',
      gender: emp.gender ?? '',
      joiningDate: emp.joiningDate,
      department: emp.department,
      designation: emp.designation,
      employmentType: emp.employmentType ?? '',
      address: emp.address ?? '',
      city: emp.city ?? '',
      state: emp.state ?? '',
      country: emp.country ?? '',
      pincode: emp.pincode ?? '',
      pfApplicable: emp.pfApplicable,
      esiApplicable: emp.esiApplicable,
      ptApplicable: emp.ptApplicable,
      salaryStructureId: emp.currentSalaryStructureId ?? null,
      salaryEffectiveFrom: emp.currentSalaryEffectiveFrom ?? ''
    });
  }

  /** Whichever salary structure is currently selected in the form - looked up client-side from the already-loaded active list. */
  get selectedSalaryStructure(): SalaryStructureResponse | null {
    const id = this.form.controls.salaryStructureId.value;
    return this.salaryStructures().find(s => s.id === id) ?? null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const raw = this.form.getRawValue();

    if (this.isEditMode()) {
      const { employeeCode, enableLogin, loginAccess, ...updatePayload } = raw;
      this.employeeService.update(this.employeeId()!, updatePayload).subscribe({
        next: () => { this.toast.success('Employee updated successfully.'); this.saving.set(false); this.router.navigateByUrl('/employees'); },
        error: (err: HttpErrorResponse) => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Unable to save employee.'); }
      });
    } else {
      const payload = {
        ...raw,
        loginAccess: raw.enableLogin
          ? { username: raw.loginAccess.username, password: raw.loginAccess.password,
              confirmPassword: raw.loginAccess.confirmPassword, roleId: raw.loginAccess.roleId }
          : undefined
      };
      this.employeeService.create(payload).subscribe({
        next: () => { this.toast.success('Employee created successfully.'); this.saving.set(false); this.router.navigateByUrl('/employees'); },
        error: (err: HttpErrorResponse) => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Unable to save employee.'); }
      });
    }
  }

  cancel(): void {
    this.router.navigateByUrl('/employees');
  }

  resetForm(): void {
    this.form.reset({ enableLogin: false });
  }

  openEnableLoginForm(): void {
    this.showEnableLoginForm.set(true);
  }

  onLoginToggle(checked: boolean): void {
    if (checked) {
      this.openEnableLoginForm();
    } else {
      this.showEnableLoginForm.set(false);
      this.enableLoginForm.reset({ roleId: null });
    }
  }

  submitEnableLogin(): void {
    if (this.enableLoginForm.invalid) {
      this.enableLoginForm.markAllAsTouched();
      return;
    }
    const id = this.employeeId();
    if (!id) return;

    const { confirmPassword, ...payload } = this.enableLoginForm.getRawValue();

    this.savingLogin.set(true);
    this.employeeService.enableLogin(id, payload).subscribe({
      next: () => {
        this.toast.success('Login access enabled successfully.');
        this.existingLoginEnabled.set(true);
        this.showEnableLoginForm.set(false);
        this.savingLogin.set(false);
      },
      error: err => {
        this.savingLogin.set(false);
        this.toast.error(err.error?.message ?? 'Unable to enable login access.');
      }
    });
  }
}
