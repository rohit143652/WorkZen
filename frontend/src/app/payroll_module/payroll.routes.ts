import { Routes } from '@angular/router';
import { permissionGuard } from '../core/guards/permission.guard';
import { PayrollSettingsComponent } from './components/payroll-settings/payroll-settings.component';
import { PayrollProcessingComponent } from './components/payroll-processing/payroll-processing.component';
import { PayrollRunDetailsComponent } from './components/payroll-run-details/payroll-run-details.component';

/**
 * The Payroll Register itself was folded into the Monthly Attendance &
 * Payment Report (see /attendance/monthly-report) so there is one report
 * to look at, not two. This module owns the EPF/ESI/PT configuration
 * those figures are computed from, plus (Phase 2) the persisted Payroll
 * Run workflow.
 */
export const PAYROLL_ROUTES: Routes = [
  {
    path: 'settings', component: PayrollSettingsComponent,
    canActivate: [permissionGuard], data: { permission: 'PAYROLL_REGISTER_EXPORT' }
  },
  {
    path: 'runs', component: PayrollProcessingComponent,
    canActivate: [permissionGuard], data: { permission: 'PAYROLL_RUN_READ' }
  },
  {
    path: 'runs/:id', component: PayrollRunDetailsComponent,
    canActivate: [permissionGuard], data: { permission: 'PAYROLL_RUN_READ' }
  }
];
