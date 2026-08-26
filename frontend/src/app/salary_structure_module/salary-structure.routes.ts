import { Routes } from '@angular/router';
import { permissionGuard } from '../core/guards/permission.guard';
import { SalaryComponentListComponent } from './components/salary-component-list/salary-component-list.component';
import { SalaryStructureListComponent } from './components/salary-structure-list/salary-structure-list.component';
import { SalaryStructureFormComponent } from './components/salary-structure-form/salary-structure-form.component';
import { SalaryStructureDetailsComponent } from './components/salary-structure-details/salary-structure-details.component';

export const SALARY_STRUCTURE_ROUTES: Routes = [
  { path: '', component: SalaryStructureListComponent },
  {
    path: 'new', component: SalaryStructureFormComponent,
    canActivate: [permissionGuard], data: { permission: 'SALARY_STRUCTURE_CREATE' }
  },
  {
    path: ':id/edit', component: SalaryStructureFormComponent,
    canActivate: [permissionGuard], data: { permission: 'SALARY_STRUCTURE_UPDATE' }
  },
  { path: ':id', component: SalaryStructureDetailsComponent }
];

export const SALARY_COMPONENT_ROUTES: Routes = [
  { path: '', component: SalaryComponentListComponent }
];
