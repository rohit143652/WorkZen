import { Routes } from '@angular/router';
import { permissionGuard } from '../core/guards/permission.guard';
import { HolidayCalendarComponent } from './components/holiday-calendar/holiday-calendar.component';

export const HOLIDAY_ROUTES: Routes = [
  {
    path: '', component: HolidayCalendarComponent,
    canActivate: [permissionGuard], data: { permission: 'HOLIDAY_READ' }
  }
];
