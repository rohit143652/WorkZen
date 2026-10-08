import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EmployeeService } from '../../services/employee.service';
import { EmployeeAssignmentService } from '../../../employee_assignment_module/services/employee-assignment.service';
import { ProfileCompletion } from '../../models/employee.model';
import { ToastService } from '../../../shared/services/toast.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { ChangePasswordCardComponent } from '../../../login_module/components/change-password-card/change-password-card.component';

/** Human-readable labels for the section codes the backend returns (see EmployeeProfileCompletionService). */
const SECTION_LABELS: Record<string, string> = {
  PERSONAL_INFORMATION: 'Personal Information',
  CONTACT_INFORMATION: 'Contact Information',
  ADDRESS: 'Address',
  EMERGENCY_CONTACT: 'Emergency Contact',
  BANK_DETAILS: 'Bank Details',
  STATUTORY_INFORMATION: 'Statutory Information',
  PROFILE_PHOTO: 'Profile Photo'
};

/**
 * "My Profile" - open to EVERY signed-in account. Everyone sees their account details and can change their
 * password; a working employee additionally sees their own profile completion % and the fields they're allowed
 * to edit about themselves (spec sections 23-25: EMPLOYEE_EDITABLE only - there is no field here for
 * department/designation/salary/PF-ESI-PT/joining date, all admin-only). An account with no employee record
 * (Super Admin, Client Admin, ...) simply doesn't load or show the employee sections.
 */
@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, ChangePasswordCardComponent],
  templateUrl: './my-profile.component.html'
})
export class MyProfileComponent {
  private readonly employeeService = inject(EmployeeService);
  private readonly assignmentService = inject(EmployeeAssignmentService);
  private readonly toast = inject(ToastService);
  readonly authState = inject(AuthStateService);

  /** A working employee (has an employee record and is not a Client Admin) - the only accounts the personal-details sections apply to. */
  readonly isEmployee = !!this.authState.currentUser()?.employeeCode && !this.authState.hasRole('CLIENT_ADMIN');

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly completion = signal<ProfileCompletion | null>(null);
  readonly currentSiteName = signal<string | null>(null);

  // Editable fields - plain component state, submitted as a whole on Save (spec section 31:
  // partial saves are fine, the employee doesn't have to fill every field before saving once).
  mobileNumber = '';
  alternateMobileNumber = '';
  address = '';
  city = '';
  state = '';
  country = '';
  pincode = '';
  emergencyContactName = '';
  emergencyContactRelationship = '';
  emergencyContactMobile = '';
  bankAccountHolderName = '';
  bankAccountNumber = '';
  bankIfscCode = '';
  bankName = '';
  bankBranch = '';

  get displayName(): string {
    const user = this.authState.currentUser();
    const name = [user?.firstName, user?.lastName].filter(Boolean).join(' ').trim();
    return name || user?.username || '';
  }

  constructor() {
    if (!this.isEmployee) {
      // No employee record behind this login, so there is nothing to load (the calls would only fail with errors).
      this.loading.set(false);
      return;
    }
    this.loadCompletion();
    this.employeeService.getMyProfile().subscribe({
      next: emp => {
        this.mobileNumber = emp.mobileNumber ?? '';
        this.alternateMobileNumber = emp.alternateMobileNumber ?? '';
        this.address = emp.address ?? '';
        this.city = emp.city ?? '';
        this.state = emp.state ?? '';
        this.country = emp.country ?? '';
        this.pincode = emp.pincode ?? '';
        this.emergencyContactName = emp.emergencyContactName ?? '';
        this.emergencyContactRelationship = emp.emergencyContactRelationship ?? '';
        this.emergencyContactMobile = emp.emergencyContactMobile ?? '';
        this.bankAccountHolderName = emp.bankAccountHolderName ?? '';
        this.bankAccountNumber = emp.bankAccountNumber ?? '';
        this.bankIfscCode = emp.bankIfscCode ?? '';
        this.bankName = emp.bankName ?? '';
        this.bankBranch = emp.bankBranch ?? '';
      },
      error: () => this.toast.error('Unable to load your current profile details.')
    });
    this.assignmentService.myCurrentSite().subscribe({
      next: site => this.currentSiteName.set(site?.siteName ?? null),
      error: () => { /* Non-fatal - the site line just doesn't show if this fails. */ }
    });
  }

  sectionLabel(code: string): string {
    return SECTION_LABELS[code] ?? code;
  }

  private loadCompletion(): void {
    this.loading.set(true);
    this.employeeService.getMyProfileCompletion().subscribe({
      next: c => { this.completion.set(c); this.loading.set(false); },
      error: () => { this.toast.error('Unable to load your profile completion.'); this.loading.set(false); }
    });
  }

  save(): void {
    this.saving.set(true);
    this.employeeService.updateMyProfile({
      mobileNumber: this.mobileNumber || undefined,
      alternateMobileNumber: this.alternateMobileNumber || undefined,
      address: this.address || undefined,
      city: this.city || undefined,
      state: this.state || undefined,
      country: this.country || undefined,
      pincode: this.pincode || undefined,
      emergencyContactName: this.emergencyContactName || undefined,
      emergencyContactRelationship: this.emergencyContactRelationship || undefined,
      emergencyContactMobile: this.emergencyContactMobile || undefined,
      bankAccountHolderName: this.bankAccountHolderName || undefined,
      bankAccountNumber: this.bankAccountNumber || undefined,
      bankIfscCode: this.bankIfscCode || undefined,
      bankName: this.bankName || undefined,
      bankBranch: this.bankBranch || undefined
    }).subscribe({
      next: completion => {
        this.completion.set(completion);
        this.saving.set(false);
        this.toast.success('Profile saved successfully.');
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to save your profile.');
      }
    });
  }
}
