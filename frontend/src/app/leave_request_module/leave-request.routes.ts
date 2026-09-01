import { Routes } from '@angular/router';
import { permissionGuard } from '../core/guards/permission.guard';
import { LeaveManagementComponent } from './components/leave-management/leave-management.component';

export const LEAVE_REQUEST_ROUTES: Routes = [
  {
    path: '', component: LeaveManagementComponent,
    canActivate: [permissionGuard], data: { permission: ['LEAVE_REQUEST_READ', 'LEAVE_REQUEST_SELF_CREATE'] }
  }
];
