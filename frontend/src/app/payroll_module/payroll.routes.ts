import { Routes } from '@angular/router';
import { permissionGuard } from '../core/guards/permission.guard';
import { featureGuard } from '../core/guards/feature.guard';
import { PayrollSettingsComponent } from './components/payroll-settings/payroll-settings.component';
import { PayrollProcessingComponent } from './components/payroll-processing/payroll-processing.component';
import { PayrollRunDetailsComponent } from './components/payroll-run-details/payroll-run-details.component';
import { MyPayslipComponent } from './components/my-payslip/my-payslip.component';
import { OvertimeRegisterComponent } from './components/overtime-register/overtime-register.component';

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
  },
  {
    // Both OVERTIME_RECORD_MANAGE (log/edit/delete) and OVERTIME_RECORD_READ (view only) can
    // reach this page - the component itself checks which one to decide whether to show the
    // "Log Overtime" form. Blocked entirely if the company's own OVERTIME_MANAGEMENT feature is
    // off, same as every other feature-gated route (see core/guards/feature.guard.ts).
    path: 'overtime', component: OvertimeRegisterComponent,
    canActivate: [permissionGuard, featureGuard],
    data: { permission: ['OVERTIME_RECORD_MANAGE', 'OVERTIME_RECORD_READ'], feature: 'OVERTIME_MANAGEMENT' }
  }
];
