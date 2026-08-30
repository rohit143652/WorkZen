import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RoleService } from '../../services/role.service';
import { RoleOption } from '../../models/role.model';
import { PermissionService } from '../../../permission_module/services/permission.service';
import { PermissionOption } from '../../../permission_module/models/permission.model';
import { groupPermissionsByCategory } from '../../../permission_module/utils/permission-category.util';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';

/** Mirrors RoleService.UNRESTRICTED_PERMISSIONS on the backend exactly - permissions any ROLE_UPDATE holder can freely grant/revoke on ANY role, including their own, regardless of holding it themselves. */
const UNRESTRICTED_PERMISSIONS = new Set<string>(['PAYSLIP_SELF_VIEW']);

@Component({
  selector: 'app-role-list',
  standalone: true,
  imports: [CommonModule, FormsModule, HasPermissionDirective],
  templateUrl: './role-list.component.html',
  styleUrl: './role-list.component.css'
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

  get groupedSelectablePermissions() {
    return groupPermissionsByCategory(this.selectablePermissions());
  }

  newRoleName = '';
  newRoleDescription = '';
  selectedPermissionIds = new Set<number>();

  // ---- Edit an existing role (name/description/permissions together) ----
  readonly editingRoleId = signal<number | null>(null);
  editRoleName = '';
  editRoleDescription = '';
  editSelectedPermissionIds = new Set<number>();
  /** Permission IDs the current user personally holds - anything else shown while editing is a permission the role already has that the editor can't themselves grant/revoke (backend enforces this ceiling too), so those checkboxes are shown checked but disabled rather than silently dropped on save. */
  private myPermissionIds = new Set<number>();

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

  /** SUPER_ADMIN may edit any role, including their own. Everyone else may never edit a role that is currently one of their OWN roles - not even to fix its name/description, let alone its permissions (matches RoleService.rejectIfEditingOwnRole() on the backend exactly). */
  canEditRole(role: RoleOption): boolean {
    if (this.authState.hasRole('SUPER_ADMIN')) return true;
    return !this.authState.roles().includes(role.name);
  }

  openAddForm(): void {
    this.showAddForm.set(true);
    this.editingRoleId.set(null);
    this.newRoleName = '';
    this.newRoleDescription = '';
    this.selectedPermissionIds = new Set();

    if (this.allPermissions().length === 0) {
      this.permissionService.list().subscribe(list => {
        this.allPermissions.set(list);
        // SUPER_ADMIN bypasses the "can only grant what you have" ceiling entirely on the
        // backend - offer every permission as selectable for them, not just ones they personally hold.
        if (this.authState.hasRole('SUPER_ADMIN')) {
          this.selectablePermissions.set(list);
        } else {
          const myPermissionNames = new Set(this.authState.permissions());
          this.selectablePermissions.set(list.filter(p => myPermissionNames.has(p.name) || UNRESTRICTED_PERMISSIONS.has(p.name)));
        }
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

  /**
   * Opens the edit form for an existing role, pre-filled with its current name/description/
   * permissions. The selectable list here is the UNION of the editor's own permissions and
   * whatever the role already has - a permission the role holds that the editor themselves
   * lacks still shows up (checked, but disabled - see isPermissionLocked()) so editing something
   * else about the role can never silently strip a permission nobody here has authority over.
   */
  openEditForm(role: RoleOption): void {
    this.showAddForm.set(false);
    this.editingRoleId.set(role.id);
    this.editRoleName = role.name;
    this.editRoleDescription = role.description ?? '';

    const loadSelectable = () => {
      const myPermissionNames = new Set(this.authState.permissions());
      this.myPermissionIds = new Set(this.allPermissions().filter(p => myPermissionNames.has(p.name)).map(p => p.id));
      const rolePermissionNames = new Set(role.permissions ?? []);
      const rolePermissionIds = this.allPermissions().filter(p => rolePermissionNames.has(p.name)).map(p => p.id);
      this.editSelectedPermissionIds = new Set(rolePermissionIds);

      // SUPER_ADMIN bypasses the ceiling entirely on the backend - every permission is offered,
      // not just the union of what they personally hold and what this role already has.
      if (this.authState.hasRole('SUPER_ADMIN')) {
        this.selectablePermissions.set(this.allPermissions());
      } else {
        const grantableToOthersIds = this.allPermissions().filter(p => UNRESTRICTED_PERMISSIONS.has(p.name)).map(p => p.id);
        const unionIds = new Set([...this.myPermissionIds, ...rolePermissionIds, ...grantableToOthersIds]);
        this.selectablePermissions.set(this.allPermissions().filter(p => unionIds.has(p.id)));
      }
    };

    if (this.allPermissions().length === 0) {
      this.permissionService.list().subscribe(list => { this.allPermissions.set(list); loadSelectable(); });
    } else {
      loadSelectable();
    }
  }

  cancelEditForm(): void {
    this.editingRoleId.set(null);
  }

  /** True if this permission is on the role being edited but NOT held by the current editor - shown checked, but not something they can toggle off (matches the backend's own "can't revoke what you don't have" ceiling). */
  /** SUPER_ADMIN bypasses the "can only grant what you have" ceiling entirely on the backend (see RoleService.resolvePermissionsWithCeiling) - the frontend lock must mirror that, or a super admin would see permissions as locked that the backend would actually let them freely grant/revoke. */
  isPermissionLocked(permissionId: number): boolean {
    if (this.authState.hasRole('SUPER_ADMIN')) return false;
    const permission = this.allPermissions().find(p => p.id === permissionId);
    // UNRESTRICTED_PERMISSIONS permissions are never locked at all, on any role including the
    // editor's own - matches the backend, which exempts these from the ceiling check entirely.
    if (permission && UNRESTRICTED_PERMISSIONS.has(permission.name)) return false;
    return this.editSelectedPermissionIds.has(permissionId) && !this.myPermissionIds.has(permissionId);
  }

  /** Explains WHY a permission is locked, for the tooltip. */
  permissionLockReason(): string {
    return "You don't hold this permission yourself, so you can't change it here.";
  }

  toggleEditPermission(id: number): void {
    if (this.isPermissionLocked(id)) return;
    const set = new Set(this.editSelectedPermissionIds);
    if (set.has(id)) set.delete(id); else set.add(id);
    this.editSelectedPermissionIds = set;
  }

  submitEditRole(): void {
    const id = this.editingRoleId();
    const name = this.editRoleName.trim();
    if (!id || !name) return;
    this.saving.set(true);
    this.roleService.update(id, {
      name,
      description: this.editRoleDescription.trim() || undefined,
      permissionIds: Array.from(this.editSelectedPermissionIds)
    }).subscribe({
      next: () => {
        this.toast.success(`Role "${name}" updated successfully.`);
        this.saving.set(false);
        this.editingRoleId.set(null);
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err.error?.message ?? 'Unable to update role.');
      }
    });
  }
}
