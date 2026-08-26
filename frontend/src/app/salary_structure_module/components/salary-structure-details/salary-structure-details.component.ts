import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { SalaryStructureService } from '../../services/salary-structure.service';
import { SalaryStructureResponse } from '../../models/salary-structure.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ToastService } from '../../../shared/services/toast.service';

/**
 * Read-only "view" for a Salary Structure - the landing page after create/save,
 * and what the list's "View" action opens. Editing always happens explicitly via
 * the "Edit" button here, which routes to the separate form component; this page
 * itself never lets you change anything, so a structure someone is just looking
 * at can't be accidentally modified.
 */
@Component({
  selector: 'app-salary-structure-details',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusBadgeComponent, HasPermissionDirective],
  templateUrl: './salary-structure-details.component.html'
})
export class SalaryStructureDetailsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly structureService = inject(SalaryStructureService);
  private readonly toast = inject(ToastService);

  readonly structure = signal<SalaryStructureResponse | null>(null);
  readonly loading = signal(true);

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.structureService.getById(id).subscribe({
      next: s => { this.structure.set(s); this.loading.set(false); },
      error: () => { this.toast.error('Unable to load salary structure.'); this.loading.set(false); }
    });
  }
}
