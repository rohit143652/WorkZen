import { Routes } from '@angular/router';
import { permissionGuard } from '../core/guards/permission.guard';
import { PayrollSettingsComponent } from './components/payroll-settings/payroll-settings.component';
import { PayrollProcessingComponent } from './components/payroll-processing/payroll-processing.component';
import { PayrollRunDetailsComponent } from './components/payroll-run-details/payroll-run-details.component';
import { MyPayslipComponent } from './components/my-payslip/my-payslip.component';

/**
 * The Payroll Register itself was folded into the Monthly Attendance &
 * Payment Report (see /attendance/monthly-report) so there is one report
 * to look at, not two. This module owns the EPF/ESI/PT configuration
 * those figures are computed from, plus (Phase 2) the persisted Payroll
 * Run workflow.
 */
export const PAYROLL_ROUTES: Routes = [
  {
    // Self-service - gated on PAYSLIP_SELF_VIEW (granted to every role by default - see V84
    // migration), which any admin can turn off for a specific role at any time from Roles ->
    // Edit, rather than that decision being hardcoded here or in the migration.
    path: 'my-payslip', component: MyPayslipComponent,
    canActivate: [permissionGuard], data: { permission: 'PAYSLIP_SELF_VIEW' }
  },
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
