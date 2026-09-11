import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { EmployeeService } from '../../services/employee.service';
import { PhotoCaptureComponent } from '../photo-capture/photo-capture.component';
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

type EmployeeFormTab = 'personal' | 'employment' | 'statutory' | 'login';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirm = control.get('confirmPassword')?.value;
  if (!password || !confirm) return null;
  return password === confirm ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, PhotoCaptureComponent],
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
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly authState = inject(AuthStateService);

  readonly roles = signal<RoleOption[]>([]);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly designations = signal<DesignationResponse[]>([]);
  readonly salaryStructures = signal<SalaryStructureResponse[]>([]);
  readonly saving = signal(false);
  readonly loading = signal(false);

  /** Sectioned form (Personal / Employment / Statutory & Bank / Login Access) - purely a
      presentation split, the underlying FormGroup is one single group same as before, so
      switching tabs never loses anything typed on another tab (Angular reactive form controls
      keep their value whether or not they're currently rendered). */
  readonly activeTab = signal<EmployeeFormTab>('personal');

  readonly tabs: { id: EmployeeFormTab; label: string }[] = [
    { id: 'personal', label: 'Personal Info' },
    { id: 'employment', label: 'Employment Details' },
    { id: 'statutory', label: 'PF, ESIC & Bank Details' },
    { id: 'login', label: 'Login Access' }
  ];

  /** Which tab each required control lives on - used only to jump the user to the right tab if they submit with an error hidden on a tab they're not currently viewing. */
  private readonly CONTROL_TAB: Record<string, EmployeeFormTab> = {
    firstName: 'personal', lastName: 'personal', email: 'personal',
    aadharNumber: 'personal', panNumber: 'personal',
    joiningDate: 'employment', department: 'employment', designation: 'employment'
  };
  readonly isEditMode = signal(false);
  readonly employeeId = signal<number | null>(null);
  /** True whenever a User row exists for this employee, active or not (backend sets userId
      whenever hasLogin() is true, regardless of the active flag - see EmployeeService.toResponse()).
      Kept separate from loginIsActive below because "has an account" and "that account is
      currently switched on" are genuinely different questions - conflating them into one
      boolean previously hid the ability to re-enable a disabled login from this page entirely. */
  readonly hasLoginAccount = signal(false);
  readonly loginIsActive = signal(false);
  readonly existingUsername = signal<string | null>(null);
  readonly togglingLogin = signal(false);

  // Only used in edit mode, when the employee has no login yet - see openEnableLoginForm().
  readonly showEnableLoginForm = signal(false);
  readonly savingLogin = signal(false);
  readonly enableLoginForm = this.fb.nonNullable.group(
    {
      username: ['', [Validators.required, Validators.minLength(3)]],
      sendInvitation: [true],
      password: [''],
      confirmPassword: [''],
      roleId: [null as number | null, Validators.required]
    },
    { validators: passwordsMatchValidator }
  );

  /** Not part of the reactive form group - managed directly via app-photo-capture's
      [(photoData)] two-way binding, then merged into the payload manually in submit(). */
  photoData: string | null = null;

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
    panNumber: ['', [Validators.required, Validators.pattern(/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/)]],
    uanNumber: [''],
    pfMemberId: [''],
    esicNumber: [''],
    bankAccountHolderName: [''],
    bankAccountNumber: [''],
    bankIfscCode: [''],
    bankName: [''],
    bankBranch: [''],
    pfApplicable: [false],
    esiApplicable: [false],
    ptApplicable: [false],
    salaryStructureId: [null as number | null],
    salaryEffectiveFrom: [''],
    enableLogin: [false],
    loginAccess: this.fb.nonNullable.group(
      {
        username: [''],
        // Defaults to true - the new self-onboarding flow (employee sets their OWN password via
        // emailed invitation) is the recommended path; admin-set-password stays available by
        // switching this off, for any workflow that genuinely needs it immediately.
        sendInvitation: [true],
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
    this.form.controls.loginAccess.controls.sendInvitation.valueChanges.subscribe(sendInvitation => {
      if (this.form.controls.enableLogin.value) this.applyPasswordValidators(!sendInvitation);
    });
    this.enableLoginForm.controls.sendInvitation.valueChanges.subscribe(sendInvitation => {
      if (sendInvitation) {
        this.enableLoginForm.controls.password.clearValidators();
        this.enableLoginForm.controls.confirmPassword.clearValidators();
        this.enableLoginForm.controls.password.setValue('');
        this.enableLoginForm.controls.confirmPassword.setValue('');
      } else {
        this.enableLoginForm.controls.password.setValidators([Validators.required, Validators.minLength(8)]);
        this.enableLoginForm.controls.confirmPassword.setValidators([Validators.required]);
      }
      this.enableLoginForm.controls.password.updateValueAndValidity();
      this.enableLoginForm.controls.confirmPassword.updateValueAndValidity();
    });

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
      group.controls.roleId.setValidators([Validators.required]);
      this.applyPasswordValidators(!group.controls.sendInvitation.value);
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

  /** password/confirmPassword are only REQUIRED when the admin is setting one directly (sendInvitation off) - when sendInvitation is on, the employee sets their own via the emailed invitation, so both stay optional and blank. */
  private applyPasswordValidators(adminSetsPassword: boolean): void {
    const group = this.form.controls.loginAccess;
    if (adminSetsPassword) {
      group.controls.password.setValidators([Validators.required, Validators.minLength(8)]);
      group.controls.confirmPassword.setValidators([Validators.required]);
    } else {
      group.controls.password.clearValidators();
      group.controls.confirmPassword.clearValidators();
      group.controls.password.setValue('');
      group.controls.confirmPassword.setValue('');
    }
    group.controls.password.updateValueAndValidity();
    group.controls.confirmPassword.updateValueAndValidity();
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
    this.hasLoginAccount.set(!!emp.userId);
    this.loginIsActive.set(emp.loginEnabled);
    this.existingUsername.set(emp.username ?? null);

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

    this.photoData = emp.photoData ?? null;
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
      uanNumber: emp.uanNumber ?? '',
      pfMemberId: emp.pfMemberId ?? '',
      esicNumber: emp.esicNumber ?? '',
      bankAccountHolderName: emp.bankAccountHolderName ?? '',
      bankAccountNumber: emp.bankAccountNumber ?? '',
      bankIfscCode: emp.bankIfscCode ?? '',
      bankName: emp.bankName ?? '',
      bankBranch: emp.bankBranch ?? '',
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

  /** The input previously only *displayed* uppercase via CSS (text-transform), while the
      control's real value - and what the pattern validator checked - could still be lowercase.
      That's harmless data-wise (the backend uppercases it before saving anyway), but it meant a
      user could type "abcde1234f", see it rendered as "ABCDE1234F", yet have the validator
      silently pass or fail based on the untouched lowercase value underneath. Uppercasing the
      actual control value here (and tightening the pattern to A-Z only) keeps what's stored,
      what's shown, and what's validated all in sync. */
  onPanInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const upper = input.value.toUpperCase();
    this.form.controls.panNumber.setValue(upper, { emitEvent: false });
    input.value = upper;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.jumpToFirstInvalidTab();
      return;
    }

    this.saving.set(true);
    const raw = this.form.getRawValue();
    // The form control shows the Aadhar number formatted with spaces (see onAadharInput()) purely
    // for readability while typing - the backend only ever wants the plain 12 digits, so this is
    // the one place that strips them back out again, right before either payload is built below.
    raw.aadharNumber = (raw.aadharNumber || '').replace(/\s+/g, '');
    const rawWithPhoto = { ...raw, photoData: this.photoData };

    if (this.isEditMode()) {
      const { employeeCode, enableLogin, loginAccess, ...updatePayload } = rawWithPhoto;
      this.employeeService.update(this.employeeId()!, updatePayload).subscribe({
        next: () => { this.toast.success('Employee updated successfully.'); this.saving.set(false); this.router.navigateByUrl('/employees'); },
        error: (err: HttpErrorResponse) => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Unable to save employee.'); }
      });
    } else {
      const payload = {
        ...rawWithPhoto,
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

  /** If a required field is invalid but sits on a tab the user isn't currently looking at, an
      error message rendered next to it would be completely invisible - jump there instead of
      leaving them stuck wondering why Save did nothing. Only checks the fields actually listed
      in CONTROL_TAB (the required ones) - optional fields never block submission so they never
      need this. */
  private jumpToFirstInvalidTab(): void {
    for (const [controlName, tab] of Object.entries(this.CONTROL_TAB)) {
      if (this.form.get(controlName)?.invalid) {
        this.activeTab.set(tab);
        this.toast.error(`Please fix the highlighted field(s) in the "${this.tabs.find(t => t.id === tab)?.label}" tab.`);
        return;
      }
    }
    // Login Access fields (username/password/role) aren't in CONTROL_TAB since they're only
    // required conditionally (enableLogin on, or the reactivation form open) - checked
    // separately since "invalid" alone can't distinguish "required and empty" from "not
    // applicable right now".
    if ((this.form.controls.enableLogin.value && this.form.controls.loginAccess.invalid)
        || (this.showEnableLoginForm() && this.enableLoginForm.invalid)) {
      this.activeTab.set('login');
      this.toast.error('Please fix the highlighted field(s) in the "Login Access" tab.');
    }
  }

  /** Drives a small error dot on a tab button so a mistake on a tab the user isn't viewing is at least visible, not just silently there. */
  tabHasError(tab: EmployeeFormTab): boolean {
    return Object.entries(this.CONTROL_TAB).some(
      ([controlName, t]) => t === tab && !!this.form.get(controlName)?.invalid && !!this.form.get(controlName)?.touched
    );
  }

  resetForm(): void {
    this.form.reset({ enableLogin: false });
  }

  openEnableLoginForm(): void {
    this.showEnableLoginForm.set(true);
    this.autoGenerateUsername(this.enableLoginForm.controls.username);
  }

  /**
   * The one toggle switch now has to cover three real states instead of two:
   *  - never had a login at all -> flipping on opens the full new-account form (username/
   *    password/confirm/role, same as before).
   *  - has one, currently disabled -> flipping on reactivates it (see reactivateLogin()) -
   *    exactly the same "reuse the old username" fix just made on the Employee Details page,
   *    now also reachable from here instead of only from there.
   *  - has one, currently active -> flipping off disables it (with a confirmation, since this
   *    immediately signs the employee out).
   */
  async onLoginToggle(checked: boolean): Promise<void> {
    if (!this.hasLoginAccount()) {
      if (checked) {
        this.openEnableLoginForm();
      } else {
        this.showEnableLoginForm.set(false);
        this.enableLoginForm.reset({ roleId: null });
      }
      return;
    }

    if (checked && !this.loginIsActive()) {
      await this.reactivateLogin();
      return;
    }

    if (!checked && this.loginIsActive()) {
      const id = this.employeeId();
      if (!id) return;
      const ok = await this.confirmDialog.ask({
        title: 'Disable login access?',
        message: 'This will immediately sign the employee out and prevent further logins. Continue?',
        confirmLabel: 'Disable',
        danger: true
      });
      if (!ok) return;
      this.togglingLogin.set(true);
      this.employeeService.disableLogin(id).subscribe({
        next: () => {
          this.loginIsActive.set(false);
          this.togglingLogin.set(false);
          this.toast.success('Login access disabled successfully.');
        },
        error: err => {
          this.togglingLogin.set(false);
          this.toast.error(err.error?.message ?? 'Unable to disable login access.');
        }
      });
    }
  }

  /** Reuses the old username, reactivates the existing account, and immediately issues a fresh
      temporary password - same behavior as Employee Details' "Enable Login (same username)". */
  private async reactivateLogin(): Promise<void> {
    const id = this.employeeId();
    if (!id) return;
    this.togglingLogin.set(true);
    this.employeeService.enableLogin(id, {}).subscribe({
      next: () => {
        this.loginIsActive.set(true);
        this.employeeService.resetPassword(id).subscribe({
          next: temp => {
            this.togglingLogin.set(false);
            this.toast.success(`Login re-enabled for ${this.existingUsername()}. Temporary password: ${temp} (shown once - share it securely).`);
          },
          error: () => {
            this.togglingLogin.set(false);
            this.toast.warning('Login re-enabled, but the automatic password reset failed - use the Employee Details page to reset it.');
          }
        });
      },
      error: err => {
        this.togglingLogin.set(false);
        this.toast.error(err.error?.message ?? 'Unable to enable login access.');
      }
    });
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
      next: emp => {
        this.toast.success('Login access enabled successfully.');
        this.hasLoginAccount.set(true);
        this.loginIsActive.set(true);
        this.existingUsername.set(emp.username ?? payload.username ?? null);
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
