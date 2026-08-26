import { Routes } from '@angular/router';
import { permissionGuard } from '../core/guards/permission.guard';
import { AdvanceDashboardComponent } from './components/advance-dashboard/advance-dashboard.component';

/**
 * The Advance Dashboard is the only top-level advance route - actually
 * giving an advance, editing recovery, or settling one still happens from
 * the "Employee Advances" card embedded in that specific employee's own
 * Details page (see employee_module), not here. This route exists purely
 * so there's a company-wide place to SEE every advance at once (spec
 * section 12) - the completion audit found this was entirely missing.
 */
export const ADVANCE_ROUTES: Routes = [
  {
    path: '', component: AdvanceDashboardComponent,
    canActivate: [permissionGuard], data: { permission: 'ADVANCE_READ' }
  }
];
