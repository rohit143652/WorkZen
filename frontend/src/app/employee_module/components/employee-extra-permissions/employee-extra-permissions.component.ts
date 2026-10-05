import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { ExtraPermissionOption, ExtraPermissions } from '../../models/employee.model';
import { EmployeeService } from '../../services/employee.service';
import { groupPermissionsByCategory } from '../../../permission_module/utils/permission-category.util';
import { isPermissionFeatureAvailable } from '../../../permission_module/utils/permission-feature.util';
import { FeatureStateService } from '../../../core/services/feature-state.service';
import { ToastService } from '../../../shared/services/toast.service';

/**
 * "Same role, but this one person needs a bit more": additional permissions for ONE employee, on
 * top of their role. Strictly additive - it can never take away what the role gives. You can only
 * offer what you hold yourself (the server enforces that; the list here simply never shows the rest).
 */
@Component({
  selector: 'app-employee-extra-permissions',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './employee-extra-permissions.component.html'
})
export class EmployeeExtraPermissionsComponent implements OnInit {
  private readonly employeeService = inject(EmployeeService);
  private readonly featureState = inject(FeatureStateService);
  private readonly toast = inject(ToastService);

  @Input({ required: true }) employeeId!: number;

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly data = signal<ExtraPermissions | null>(null);
  /** The permission ids currently ticked - including locked ones, which the user can't untick. */
  readonly selected = signal<Set<number>>(new Set());
  private saved = new Set<number>();

  /** Hidden while the company feature behind them is off (same rule as the role editor) - but a permission that is already granted stays ticked/kept even if hidden. */
  readonly groups = computed(() => {
    const d = this.data();
    if (!d) return [];
    const visible = d.grantable.filter(p => isPermissionFeatureAvailable(p.name, code => this.featureState.isEnabled(code)));
    return groupPermissionsByCategory<ExtraPermissionOption>(visible);
  });

  readonly dirty = computed(() => {
    const now = this.selected();
    if (now.size !== this.saved.size) return true;
    for (const id of now) if (!this.saved.has(id)) return true;
    return false;
  });

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.employeeService.getExtraPermissions(this.employeeId).subscribe({
      next: d => { this.apply(d); this.loading.set(false); },
      error: err => { this.loading.set(false); this.error.set(err.error?.message ?? 'Unable to load the additional permissions.'); }
    });
  }

  private apply(d: ExtraPermissions): void {
    this.data.set(d);
    this.saved = new Set(d.extraPermissions.map(p => p.id));
    this.selected.set(new Set(this.saved));
  }

  isChecked(id: number): boolean {
    return this.selected().has(id);
  }

  toggle(p: ExtraPermissionOption): void {
    if (p.locked) return;
    const next = new Set(this.selected());
    if (next.has(p.id)) next.delete(p.id); else next.add(p.id);
    this.selected.set(next);
  }

  reset(): void {
    this.selected.set(new Set(this.saved));
  }

  save(): void {
    this.saving.set(true);
    this.employeeService.saveExtraPermissions(this.employeeId, Array.from(this.selected())).subscribe({
      next: d => {
        this.saving.set(false);
        this.apply(d);
        this.toast.success('Additional permissions saved. The change applies to this employee immediately; their menu updates the next time they reload the page.');
      },
      error: err => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Unable to save the additional permissions.'); }
    });
  }
}
