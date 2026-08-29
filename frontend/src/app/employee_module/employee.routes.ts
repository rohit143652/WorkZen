import { Routes } from '@angular/router';
import { EmployeeListComponent } from './components/employee-list/employee-list.component';
import { EmployeeFormComponent } from './components/employee-form/employee-form.component';
import { EmployeeDetailsComponent } from './components/employee-details/employee-details.component';
import { EmployeeBulkImportComponent } from './components/employee-bulk-import/employee-bulk-import.component';

export const EMPLOYEE_ROUTES: Routes = [
  { path: '', component: EmployeeListComponent },
  { path: 'new', component: EmployeeFormComponent, data: { permission: 'EMPLOYEE_CREATE' } },
  { path: 'bulk-import', component: EmployeeBulkImportComponent, data: { permission: 'EMPLOYEE_CREATE' } },
  { path: ':id', component: EmployeeDetailsComponent },
  { path: ':id/edit', component: EmployeeFormComponent, data: { permission: 'EMPLOYEE_UPDATE' } }
];
