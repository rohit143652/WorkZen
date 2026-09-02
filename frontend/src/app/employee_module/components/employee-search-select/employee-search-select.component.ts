import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EmployeeResponse } from '../../models/employee.model';

/**
 * A plain <select> with 100+ employees in it is painful to use - you either scroll through
 * everyone or type-to-jump by first letter, which only works for the first name shown. This is
 * a lightweight, reusable replacement: type anything (code or name) to filter, click a result to
 * pick it. Used via [(selectedId)] two-way binding, e.g.:
 *   <app-employee-search-select [employees]="activeEmployees()" [(selectedId)]="newEmployeeId" />
 * wherever a form needs "pick one employee from the full list" - Leave Requests' admin add,
 * Exit Management's resignation form, and anywhere else that grows the same need later.
 */
@Component({
  selector: 'app-employee-search-select',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './employee-search-select.component.html'
})
export class EmployeeSearchSelectComponent {
  @Input() employees: EmployeeResponse[] = [];
  @Input() selectedId: number | null = null;
  @Input() placeholder = 'Search by name or code...';
  @Output() selectedIdChange = new EventEmitter<number | null>();

  searchTerm = '';
  showDropdown = false;

  get filteredEmployees(): EmployeeResponse[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) return this.employees;
    return this.employees.filter(e =>
      e.employeeCode.toLowerCase().includes(term) ||
      `${e.firstName} ${e.lastName}`.toLowerCase().includes(term)
    );
  }

  get selectedEmployee(): EmployeeResponse | undefined {
    return this.employees.find(e => e.id === this.selectedId);
  }

  onFocus(): void {
    this.searchTerm = '';
    this.showDropdown = true;
  }

  onBlur(): void {
    // Delayed so a click on a dropdown option registers BEFORE the dropdown closes - a plain
    // (blur) firing immediately would close it first and swallow the click.
    setTimeout(() => (this.showDropdown = false), 150);
  }

  select(employee: EmployeeResponse): void {
    this.selectedId = employee.id;
    this.selectedIdChange.emit(employee.id);
    this.searchTerm = '';
    this.showDropdown = false;
  }

  clear(): void {
    this.selectedId = null;
    this.selectedIdChange.emit(null);
    this.searchTerm = '';
  }
}
