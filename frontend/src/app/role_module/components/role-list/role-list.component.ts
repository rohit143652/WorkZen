import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RoleService } from '../../services/role.service';
import { RoleOption } from '../../models/role.model';
import { PermissionService } from '../../../permission_module/services/permission.service';
import { PermissionOption } from '../../../permission_module/models/permission.model';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-role-list',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent, HasPermissionDirective],
  templateUrl: './role-list.component.html'
})
export class RoleListComponent {
  private readonly roleService = inject(RoleService);
  private readonly permissionService = inject(PermissionService);
  private readonly toast = inject(ToastService);
  readonly authState = inject(AuthStateService);

  readonly roles = signal<RoleOption[]>([]);
  readonly loading = signal(true);
  readonly showAddForm = signal(false);
  readonly saving = signal(false);

  /** The full permission catalog, needed to submit permissionIds. */
  readonly allPermissions = signal<PermissionOption[]>([]);
  /**
   * Only permissions the CURRENT user themselves holds are offered as
   * selectable checkboxes - matches the backend's "can't grant what you
   * don't have" ceiling (see RoleService.resolvePermissionsWithCeiling)
   * exactly, so the form never lets you attempt something that will 400.
   */
  readonly selectablePermissions = signal<PermissionOption[]>([]);

  newRoleName = '';
  newRoleDescription = '';
  selectedPermissionIds = new Set<number>();

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.roleService.list().subscribe({
      next: roles => { this.roles.set(roles); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  openAddForm(): void {
    this.showAddForm.set(true);
    this.newRoleName = '';
    this.newRoleDescription = '';
    this.selectedPermissionIds = new Set();

    if (this.allPermissions().length === 0) {
      this.permissionService.list().subscribe(list => {
        this.allPermissions.set(list);
        const myPermissionNames = new Set(this.authState.permissions());
        this.selectablePermissions.set(list.filter(p => myPermissionNames.has(p.name)));
      });
    }
  }

  togglePermission(id: number): void {
    const set = new Set(this.selectedPermissionIds);
    if (set.has(id)) set.delete(id); else set.add(id);
    this.selectedPermissionIds = set;
  }

  submitNewRole(): void {
    const name = this.newRoleName.trim();
    if (!name) return;
    this.saving.set(true);
    this.roleService.create({
      name,
      description: this.newRoleDescription.trim() || undefined,
      permissionIds: Array.from(this.selectedPermissionIds)
    }).subscribe({
      next: () => {
        this.toast.success(`Role "${name}" added successfully.`);
        this.saving.set(false);
        this.showAddForm.set(false);
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to add role.');
      }
    });
  }
}
