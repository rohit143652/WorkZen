import { Routes } from '@angular/router';
import { permissionGuard } from '../core/guards/permission.guard';
import { CalendarComponent } from './components/calendar/calendar.component';

export const CALENDAR_ROUTES: Routes = [
  { path: '', component: CalendarComponent, canActivate: [permissionGuard], data: { permission: 'EVENT_READ' } }
];
