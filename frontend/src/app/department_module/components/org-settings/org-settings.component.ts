import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DepartmentService } from '../../services/department.service';
import { DepartmentResponse } from '../../models/department.model';
import { DesignationService } from '../../../designation_module/services/designation.service';
import { DesignationResponse } from '../../../designation_module/models/designation.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

@Component({
  selector: 'app-org-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StatusBadgeComponent, HasPermissionDirective],
  templateUrl: './org-settings.component.html'
})
export class OrgSettingsComponent {
  private readonly departmentService = inject(DepartmentService);
  private readonly designationService = inject(DesignationService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly departments = signal<DepartmentResponse[]>([]);
  readonly designations = signal<DesignationResponse[]>([]);
  readonly loadingDepartments = signal(true);
  readonly loadingDesignations = signal(true);
  readonly savingDepartment = signal(false);
  readonly savingDesignation = signal(false);

  newDepartmentName = '';
  newDesignationName = '';

  /** Inline "rename" state - at most one designation row being renamed at a time. */
  readonly editingNameId = signal<number | null>(null);
  editingName = '';
  readonly savingName = signal(false);

  constructor() {
    this.loadDepartments();
    this.loadDesignations();
  }

  private loadDepartments(): void {
    this.loadingDepartments.set(true);
    this.departmentService.list(true).subscribe({
      next: list => { this.departments.set(list); this.loadingDepartments.set(false); },
      error: () => this.loadingDepartments.set(false)
    });
  }

  private loadDesignations(): void {
    this.loadingDesignations.set(true);
    this.designationService.list(true).subscribe({
      next: list => { this.designations.set(list); this.loadingDesignations.set(false); },
      error: () => this.loadingDesignations.set(false)
    });
  }

  addDepartment(): void {
    const name = this.newDepartmentName.trim();
    if (!name) return;
    this.savingDepartment.set(true);
    this.departmentService.create({ name }).subscribe({
      next: () => {
        this.toast.success(`Department "${name}" added.`);
        this.newDepartmentName = '';
        this.savingDepartment.set(false);
        this.loadDepartments();
      },
      error: err => { this.savingDepartment.set(false); this.toast.error(err.error?.message ?? 'Unable to add department.'); }
    });
  }

  addDesignation(): void {
    const name = this.newDesignationName.trim();
    if (!name) return;
    this.savingDesignation.set(true);
    this.designationService.create({ name }).subscribe({
      next: () => {
        this.toast.success(`Designation "${name}" added.`);
        this.newDesignationName = '';
        this.savingDesignation.set(false);
        this.loadDesignations();
      },
      error: err => { this.savingDesignation.set(false); this.toast.error(err.error?.message ?? 'Unable to add designation.'); }
    });
  }

  startEditName(d: DesignationResponse): void {
    this.editingNameId.set(d.id);
    this.editingName = d.name;
  }

  cancelEditName(): void {
    this.editingNameId.set(null);
  }

  saveEditName(d: DesignationResponse): void {
    const name = this.editingName.trim();
    if (!name) {
      this.toast.warning('Designation name cannot be empty.');
      return;
    }
    this.savingName.set(true);
    this.designationService.rename(d.id, { name }).subscribe({
      next: () => {
        this.toast.success(`Designation renamed to "${name}".`);
        this.savingName.set(false);
        this.editingNameId.set(null);
        this.loadDesignations();
      },
      error: err => { this.savingName.set(false); this.toast.error(err.error?.message ?? 'Unable to rename designation.'); }
    });
  }

  async toggleDepartment(d: DepartmentResponse): Promise<void> {
    const activating = d.status !== 'ACTIVE';
    const ok = await this.confirmDialog.ask({
      title: activating ? 'Activate department?' : 'Deactivate department?',
      message: activating
        ? `"${d.name}" will become selectable in the Employee form again.`
        : `"${d.name}" will no longer appear as an option for new/edited employees. ${d.employeeCount} employee(s) currently use it and are unaffected.`,
      confirmLabel: activating ? 'Activate' : 'Deactivate',
      danger: !activating
    });
    if (!ok) return;
    const action$ = activating ? this.departmentService.activate(d.id) : this.departmentService.deactivate(d.id);
    action$.subscribe({
      next: () => { this.toast.success('Updated successfully.'); this.loadDepartments(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to update department.')
    });
  }

  async toggleDesignation(d: DesignationResponse): Promise<void> {
    const activating = d.status !== 'ACTIVE';
    const ok = await this.confirmDialog.ask({
      title: activating ? 'Activate designation?' : 'Deactivate designation?',
      message: activating
        ? `"${d.name}" will become selectable in the Employee form again.`
        : `"${d.name}" will no longer appear as an option for new/edited employees. ${d.employeeCount} employee(s) currently use it and are unaffected.`,
      confirmLabel: activating ? 'Activate' : 'Deactivate',
      danger: !activating
    });
    if (!ok) return;
    const action$ = activating ? this.designationService.activate(d.id) : this.designationService.deactivate(d.id);
    action$.subscribe({
      next: () => { this.toast.success('Updated successfully.'); this.loadDesignations(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to update designation.')
    });
  }
}
