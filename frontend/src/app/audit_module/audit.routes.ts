import { Routes } from '@angular/router';
import { AuditLogListComponent } from './components/audit-log-list/audit-log-list.component';

export const AUDIT_ROUTES: Routes = [
  { path: '', component: AuditLogListComponent }
];
