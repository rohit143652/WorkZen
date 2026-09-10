import { Routes } from '@angular/router';
import { PlanListComponent } from './components/plan-list/plan-list.component';
import { PlanFormComponent } from './components/plan-form/plan-form.component';

/** Super Admin only - see SUBSCRIPTION_PLAN_* permissions (V110 migration). */
export const SUBSCRIPTION_PLAN_ROUTES: Routes = [
  { path: '', component: PlanListComponent, data: { permission: 'SUBSCRIPTION_PLAN_READ' } },
  { path: 'new', component: PlanFormComponent, data: { permission: 'SUBSCRIPTION_PLAN_CREATE' } },
  { path: ':id/edit', component: PlanFormComponent, data: { permission: 'SUBSCRIPTION_PLAN_UPDATE' } }
];
