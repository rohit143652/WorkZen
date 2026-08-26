import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SalaryComponentService } from '../../services/salary-component.service';
import {
  CALCULATION_TYPES, CREATABLE_COMPONENT_TYPES, CalculationType, ComponentType, SalaryComponentResponse
} from '../../models/salary-component.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-salary-component-list',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent, HasPermissionDirective],
  templateUrl: './salary-component-list.component.html'
})
export class SalaryComponentListComponent {
  private readonly componentService = inject(SalaryComponentService);
  private readonly toast = inject(ToastService);

  readonly components = signal<SalaryComponentResponse[]>([]);
  readonly loading = signal(true);
  readonly showAddForm = signal(false);
  readonly saving = signal(false);
  readonly componentTypes = CREATABLE_COMPONENT_TYPES;
  readonly calculationTypes = CALCULATION_TYPES;

  newName = '';
  newType: ComponentType = 'EARNING';
  newCalcType: CalculationType = 'FIXED';
  newValue: number | null = null;
  newPercentage: number | null = null;
  newTaxable = true;

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.componentService.list(true).subscribe({
      next: list => { this.components.set(list); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  needsValue(calcType: CalculationType): boolean {
    return calcType === 'FIXED' || calcType === 'MANUAL' || calcType === 'PER_DAY' || calcType === 'PER_HOUR';
  }

  needsPercentage(calcType: CalculationType): boolean {
    return calcType === 'PERCENTAGE_OF_BASIC' || calcType === 'PERCENTAGE_OF_GROSS';
  }

  addComponent(): void {
    if (!this.newName.trim()) return;
    this.saving.set(true);
    this.componentService.create({
      componentName: this.newName.trim(),
      componentType: this.newType,
      calculationType: this.newCalcType,
      value: this.needsValue(this.newCalcType) ? this.newValue : null,
      percentage: this.needsPercentage(this.newCalcType) ? this.newPercentage : null,
      taxable: this.newTaxable,
      displayOrder: 0
    }).subscribe({
      next: () => {
        this.toast.success(`Component "${this.newName}" added.`);
        this.newName = '';
        this.newValue = null;
        this.newPercentage = null;
        this.saving.set(false);
        this.load();
      },
      error: err => { this.saving.set(false); this.toast.error(err.error?.message ?? 'Unable to add component.'); }
    });
  }

  async toggle(c: SalaryComponentResponse): Promise<void> {
    const action$ = c.active ? this.componentService.deactivate(c.id) : this.componentService.activate(c.id);
    action$.subscribe({
      next: () => { this.toast.success('Updated successfully.'); this.load(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to update component.')
    });
  }
}
