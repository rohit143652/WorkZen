import { Routes } from '@angular/router';
import { permissionGuard } from '../core/guards/permission.guard';
import { featureGuard } from '../core/guards/feature.guard';
import { MarkAttendanceComponent } from './components/mark-attendance/mark-attendance.component';
import { MarkMyAttendanceComponent } from './components/mark-my-attendance/mark-my-attendance.component';
import { AttendanceHistoryComponent } from './components/attendance-history/attendance-history.component';
import { MonthlyReportComponent } from './components/monthly-report/monthly-report.component';
import { CorrectionRequestComponent } from './components/correction-request/correction-request.component';
import { CorrectionReviewComponent } from './components/correction-review/correction-review.component';
import { AttendanceRulesComponent } from './components/attendance-rules/attendance-rules.component';

// Every route here also runs featureGuard for ATTENDANCE_MANAGEMENT (the module-level toggle) -
// if a Super Admin has switched Attendance off for a company entirely, NONE of these pages
// should be reachable by direct URL either, not just hidden from the sidebar (see
// core/guards/feature.guard.ts). 'my' additionally requires EMPLOYEE_SELF_ATTENDANCE.
// Correction Requests/Review additionally require EMPLOYEE_SELF_ATTENDANCE - a correction is
// fixing an employee's own check-in/check-out, so if employees aren't self-marking at all,
// there's nothing self-service to correct (or, on the admin side, review) in the first place.
export const ATTENDANCE_ROUTES: Routes = [
  { path: '', component: MarkAttendanceComponent, canActivate: [permissionGuard, featureGuard], data: { permission: 'ATTENDANCE_CREATE', feature: 'ATTENDANCE_MANAGEMENT' } },
  { path: 'my', component: MarkMyAttendanceComponent, canActivate: [permissionGuard, featureGuard], data: { permission: 'ATTENDANCE_SELF_MARK', feature: ['ATTENDANCE_MANAGEMENT', 'EMPLOYEE_SELF_ATTENDANCE'] } },
  { path: 'history', component: AttendanceHistoryComponent, canActivate: [permissionGuard, featureGuard], data: { permission: 'ATTENDANCE_READ', feature: 'ATTENDANCE_MANAGEMENT' } },
  { path: 'monthly-report', component: MonthlyReportComponent, canActivate: [permissionGuard, featureGuard], data: { permission: 'MONTHLY_PAYMENT_REPORT_EXPORT', feature: 'ATTENDANCE_MANAGEMENT' } },
  { path: 'correction-requests', component: CorrectionRequestComponent, canActivate: [permissionGuard, featureGuard], data: { permission: 'ATTENDANCE_CORRECTION_REQUEST', feature: ['ATTENDANCE_MANAGEMENT', 'EMPLOYEE_SELF_ATTENDANCE'] } },
  { path: 'correction-review', component: CorrectionReviewComponent, canActivate: [permissionGuard, featureGuard], data: { permission: 'ATTENDANCE_CORRECTION_REVIEW', feature: ['ATTENDANCE_MANAGEMENT', 'EMPLOYEE_SELF_ATTENDANCE'] } },
  { path: 'rules', component: AttendanceRulesComponent, canActivate: [permissionGuard, featureGuard], data: { permission: 'ATTENDANCE_RULES_MANAGE', feature: ['ATTENDANCE_MANAGEMENT', 'EMPLOYEE_SELF_ATTENDANCE'] } }
];
