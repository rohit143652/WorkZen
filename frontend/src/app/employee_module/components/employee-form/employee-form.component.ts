import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
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
import { UserManagementService } from '../../../user_module/services/user-management.service';

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
  private readonly userManagementService = inject(UserManagementService);
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
    aadharNumber: ['', [Validators.required, Validators.pattern(/^\d{4} ?\d{4} ?\d{4}$/)]],
    panNumber: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{5}[0-9]{4}[A-Za-z]{1}$/)]],
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

    // Keep the auto-generated username in sync if the admin edits the name AFTER already
    // switching Login Enabled on (e.g. fixed a typo in the last name) - a no-op while login is
    // off, since autoGenerateUsername() only touches the (currently hidden) username field.
    // Debounced so it doesn't fire (and hit the backend) on every single keystroke.
    this.form.controls.firstName.valueChanges.pipe(debounceTime(500)).subscribe(() => {
      if (this.form.controls.enableLogin.value) this.autoGenerateUsername();
    });
    this.form.controls.lastName.valueChanges.pipe(debounceTime(500)).subscribe(() => {
      if (this.form.controls.enableLogin.value) this.autoGenerateUsername();
    });

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
      this.autoGenerateUsername();
    } else {
      group.controls.username.clearValidators();
      group.controls.password.clearValidators();
      group.controls.confirmPassword.clearValidators();
      group.controls.roleId.clearValidators();
      group.controls.username.enable({ emitEvent: false });
      group.controls.username.setValue('');
    }
    Object.values(group.controls).forEach(c => c.updateValueAndValidity());
  }

  /**
   * Auto-fills and locks the username the moment "Login Enabled" is switched on (spec: admin
   * never types a username by hand) - "first initial.lastname" first (e.g. "m.kumari" for Meena
   * Kumari), falling back through fuller/numbered variants server-side if that's already taken.
   * See UserService.generateUsername() for the exact fallback order. Disabled while generating so
   * a second toggle-off/on cycle can't race a stale request against a fresh one. Shared by both
   * the create-mode nested loginAccess group and the separate edit-mode enableLoginForm - both
   * read the SAME employee's firstName/lastName off the main form either way.
   */
  private autoGenerateUsername(usernameControl = this.form.controls.loginAccess.controls.username): void {
    const firstName = this.form.controls.firstName.value?.trim();
    const lastName = this.form.controls.lastName.value?.trim();
    if (!firstName || !lastName) {
      // Nothing to generate from yet - leave the field editable and empty until both names are
      // filled in (e.g. mid-way through the Personal Details section, above this one).
      usernameControl.enable({ emitEvent: false });
      usernameControl.setValue('');
      return;
    }
    usernameControl.disable({ emitEvent: false });
    usernameControl.setValue('Generating…');
    this.userManagementService.generateUsername(firstName, lastName).subscribe({
      next: username => usernameControl.setValue(username),
      error: () => {
        this.toast.error('Unable to auto-generate a username - please enter one manually.');
        usernameControl.setValue('');
        usernameControl.enable({ emitEvent: false });
      }
    });
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
      aadharNumber: this.formatAadhar(emp.aadharNumber ?? ''),
      panNumber: emp.panNumber ?? '',
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

  /** Inserts a space every 4 digits ("123456789012" -> "1234 5678 9012") - purely for readability, never what actually gets sent to or stored by the backend (see submit(), which strips spaces back out). */
  private formatAadhar(value: string): string {
    const digitsOnly = value.replace(/\D/g, '').slice(0, 12);
    return digitsOnly.replace(/(\d{4})(?=\d)/g, '$1 ');
  }

  /** Live-reformats the Aadhar field as the user types, so the spacing stays correct even after they delete/retype a digit in the middle. */
  onAadharInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const formatted = this.formatAadhar(input.value);
    this.form.controls.aadharNumber.setValue(formatted, { emitEvent: false });
    input.value = formatted;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const raw = this.form.getRawValue();
    // The form control shows the Aadhar number formatted with spaces (see onAadharInput()) purely
    // for readability while typing - the backend only ever wants the plain 12 digits, so this is
    // the one place that strips them back out again, right before either payload is built below.
    raw.aadharNumber = (raw.aadharNumber || '').replace(/\s+/g, '');

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
    this.autoGenerateUsername(this.enableLoginForm.controls.username);
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
