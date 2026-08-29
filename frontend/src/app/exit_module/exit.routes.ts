import { Routes } from '@angular/router';
import { permissionGuard } from '../core/guards/permission.guard';
import { EmployeeExitsComponent } from './components/employee-exits/employee-exits.component';

export const EXIT_ROUTES: Routes = [
  {
    path: '', component: EmployeeExitsComponent,
    canActivate: [permissionGuard], data: { permission: 'EMPLOYEE_EXIT_READ' }
  }
];
