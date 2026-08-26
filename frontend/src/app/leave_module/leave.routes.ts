import { Routes } from '@angular/router';
import { permissionGuard } from '../core/guards/permission.guard';
import { PaidLeaveSettingsComponent } from './components/paid-leave-settings/paid-leave-settings.component';

export const LEAVE_ROUTES: Routes = [
  {
    path: 'settings', component: PaidLeaveSettingsComponent,
    canActivate: [permissionGuard], data: { permission: 'PAID_LEAVE_CONFIG_UPDATE' }
  }
];
