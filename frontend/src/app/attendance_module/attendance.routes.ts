import { Routes } from '@angular/router';
import { permissionGuard } from '../core/guards/permission.guard';
import { MarkAttendanceComponent } from './components/mark-attendance/mark-attendance.component';
import { AttendanceHistoryComponent } from './components/attendance-history/attendance-history.component';
import { MonthlyReportComponent } from './components/monthly-report/monthly-report.component';

export const ATTENDANCE_ROUTES: Routes = [
  { path: '', component: MarkAttendanceComponent, canActivate: [permissionGuard], data: { permission: 'ATTENDANCE_CREATE' } },
  { path: 'history', component: AttendanceHistoryComponent, canActivate: [permissionGuard], data: { permission: 'ATTENDANCE_READ' } },
  { path: 'monthly-report', component: MonthlyReportComponent, canActivate: [permissionGuard], data: { permission: 'MONTHLY_PAYMENT_REPORT_EXPORT' } }
];
