import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { SiteService } from '../../../site_module/services/site.service';
import { SiteResponse } from '../../../site_module/models/site.model';
import { EmployeeService } from '../../../employee_module/services/employee.service';
import { EmployeeResponse } from '../../../employee_module/models/employee.model';
import { EmployeeAssignmentService } from '../../services/employee-assignment.service';
import { EmployeeAssignmentResponse } from '../../models/employee-assignment.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { ToastService } from '../../../shared/services/toast.service';
import { ConfirmDialogService } from '../../../shared/services/confirm-dialog.service';

interface SelectableEmployee {
  employee: EmployeeResponse;
  /** Site name the employee is currently assigned to elsewhere, if any. Null = available to assign. */
  assignedElsewhere: string | null;
}

@Component({
  selector: 'app-assignment-board',
  standalone: true,
  imports: [CommonModule, FormsModule, StatusBadgeComponent],
  templateUrl: './assignment-board.component.html'
})
export class AssignmentBoardComponent {
  private readonly siteService = inject(SiteService);
  private readonly employeeService = inject(EmployeeService);
  private readonly assignmentService = inject(EmployeeAssignmentService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly route = inject(ActivatedRoute);

  readonly sites = signal<SiteResponse[]>([]);
  /** Only ACTIVE sites are selectable for assignment - a deactivated site (and its
   *  already-ended assignments) should never appear as an assignment target. */
  readonly assignableSites = signal<SiteResponse[]>([]);
  readonly selectedSiteId = signal<number | null>(null);
  readonly selectedSite = signal<SiteResponse | null>(null);

  readonly selectableEmployees = signal<SelectableEmployee[]>([]);
  readonly assignedAtSite = signal<EmployeeAssignmentResponse[]>([]);
  readonly selectedEmployeeIds = signal<Set<number>>(new Set());
  readonly selectedAssignmentIds = signal<Set<number>>(new Set());
  readonly loadingEmployees = signal(false);
  readonly assigning = signal(false);
  readonly endingSelected = signal(false);

  startDate = new Date().toISOString().slice(0, 10);
  remarks = '';
  employeeSearch = '';

  constructor() {
    this.siteService.list().subscribe(res => {
      this.sites.set(res.content);
      this.assignableSites.set(res.content.filter(s => s.status === 'ACTIVE'));
      const queryStiteId = this.route.snapshot.queryParamMap.get('siteId');
      if (queryStiteId) {
        const requested = res.content.find(s => s.id === Number(queryStiteId));
        if (requested && requested.status === 'ACTIVE') {
          this.selectedSiteId.set(Number(queryStiteId));
          this.onSiteChange();
        } else if (requested) {
          this.toast.warning('That site is deactivated and no longer accepts assignments.');
        }
      }
    });
  }

  onSiteIdChange(id: number | null): void {
    this.selectedSiteId.set(id);
    this.onSiteChange();
  }

  onSiteChange(): void {
    const siteId = this.selectedSiteId();
    this.selectedEmployeeIds.set(new Set());
    this.selectedAssignmentIds.set(new Set());
    if (!siteId) {
      this.selectedSite.set(null);
      return;
    }
    this.selectedSite.set(this.sites().find(s => s.id === siteId) ?? null);
    this.loadSelectableEmployees();
    this.loadAssigned();
  }

  /**
   * Loads every active employee, then cross-references every ACTIVE assignment
   * across the whole tenant (not just this site) so an employee who is
   * currently working at a *different* site shows up disabled here, with a
   * note on where they are, instead of being silently assignable to a second
   * site at the same time.
   */
  private loadSelectableEmployees(): void {
    this.loadingEmployees.set(true);
    const employees$ = this.employeeService.search({ status: 'ACTIVE', page: 0, size: 200, search: this.employeeSearch || undefined });
    const activeAssignments$ = this.assignmentService.listActive();

    employees$.subscribe({
      next: employeesRes => {
        activeAssignments$.subscribe({
          next: activeAssignments => {
            const assignedSiteByEmployee = new Map<number, string>();
            for (const a of activeAssignments) {
              assignedSiteByEmployee.set(a.employeeId, a.siteName ?? `Site #${a.siteId}`);
            }
            const list: SelectableEmployee[] = employeesRes.content.map(employee => ({
              employee,
              assignedElsewhere: assignedSiteByEmployee.get(employee.id) ?? null
            }));
            this.selectableEmployees.set(list);
            this.loadingEmployees.set(false);
          },
          error: () => {
            // Still show the employee list even if the active-assignments lookup fails,
            // just without the "already assigned" disabling - fail open on the read side.
            this.selectableEmployees.set(employeesRes.content.map(employee => ({ employee, assignedElsewhere: null })));
            this.loadingEmployees.set(false);
          }
        });
      },
      error: () => this.loadingEmployees.set(false)
    });
  }

  private loadAssigned(): void {
    const siteId = this.selectedSiteId();
    if (!siteId) return;
    this.siteService.employeesAtSite(siteId).subscribe(list => this.assignedAtSite.set(list));
  }

  isAssignable(item: SelectableEmployee): boolean {
    return item.assignedElsewhere === null;
  }

  toggleEmployee(item: SelectableEmployee): void {
    if (!this.isAssignable(item)) return;
    const set = new Set(this.selectedEmployeeIds());
    if (set.has(item.employee.id)) set.delete(item.employee.id); else set.add(item.employee.id);
    this.selectedEmployeeIds.set(set);
  }

  isSelected(id: number): boolean {
    return this.selectedEmployeeIds().has(id);
  }

  projectedAllocation(): string {
    const site = this.selectedSite();
    if (!site) return '';
    const projected = site.assignedEmployeeCount + this.selectedEmployeeIds().size;
    return site.requiredEmployeeCount > 0 ? `${projected} / ${site.requiredEmployeeCount}` : `${projected}`;
  }

  async assignSelected(): Promise<void> {
    const siteId = this.selectedSiteId();
    const ids = Array.from(this.selectedEmployeeIds());
    if (!siteId || ids.length === 0) return;

    const ok = await this.confirmDialog.ask({
      title: 'Assign employees?',
      message: `Assign ${ids.length} employee(s) to ${this.selectedSite()?.siteName} starting ${this.startDate}?`,
      confirmLabel: 'Assign'
    });
    if (!ok) return;

    this.assigning.set(true);
    this.assignmentService.bulkAssign({ siteId, employeeIds: ids, startDate: this.startDate, remarks: this.remarks }).subscribe({
      next: result => {
        this.assigning.set(false);
        if (result.rejected.length > 0) {
          this.toast.warning(`${result.assigned} of ${result.requested} assigned. ${result.rejected.length} rejected - see details.`);
        } else {
          this.toast.success(`${result.assigned} employee(s) assigned successfully.`);
        }
        this.selectedEmployeeIds.set(new Set());
        this.onSiteChange();
      },
      error: err => { this.assigning.set(false); this.toast.error(err.error?.message ?? 'Unable to assign employees.'); }
    });
  }

  async endAssignment(assignmentId: number): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'End assignment?',
      message: 'This employee will be removed from this site as of today.',
      danger: true
    });
    if (!ok) return;
    this.assignmentService.endAssignment(assignmentId).subscribe({
      next: () => { this.toast.success('Assignment ended.'); this.onSiteChange(); },
      error: err => this.toast.error(err.error?.message ?? 'Unable to end assignment.')
    });
  }

  toggleAssignmentSelection(id: number): void {
    const set = new Set(this.selectedAssignmentIds());
    if (set.has(id)) set.delete(id); else set.add(id);
    this.selectedAssignmentIds.set(set);
  }

  isAssignmentSelected(id: number): boolean {
    return this.selectedAssignmentIds().has(id);
  }

  toggleSelectAllAssigned(checked: boolean): void {
    this.selectedAssignmentIds.set(checked ? new Set(this.assignedAtSite().map(a => a.id)) : new Set());
  }

  async endSelectedAssignments(): Promise<void> {
    const ids = Array.from(this.selectedAssignmentIds());
    if (ids.length === 0) return;

    const ok = await this.confirmDialog.ask({
      title: 'End selected assignments?',
      message: `Remove ${ids.length} employee(s) from ${this.selectedSite()?.siteName}? They will become available to assign elsewhere.`,
      confirmLabel: 'End Selected',
      danger: true
    });
    if (!ok) return;

    this.endingSelected.set(true);
    this.assignmentService.bulkEnd({ assignmentIds: ids }).subscribe({
      next: result => {
        this.endingSelected.set(false);
        if (result.failed.length > 0) {
          this.toast.warning(`${result.ended} of ${result.requested} ended. ${result.failed.length} could not be ended.`);
        } else {
          this.toast.success(`${result.ended} assignment(s) ended successfully.`);
        }
        this.selectedAssignmentIds.set(new Set());
        this.onSiteChange();
      },
      error: err => { this.endingSelected.set(false); this.toast.error(err.error?.message ?? 'Unable to end selected assignments.'); }
    });
  }
}
