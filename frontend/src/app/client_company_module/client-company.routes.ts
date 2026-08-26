import { Routes } from '@angular/router';
import { ClientCompanyListComponent } from './components/client-company-list/client-company-list.component';
import { ClientCompanyFormComponent } from './components/client-company-form/client-company-form.component';

export const CLIENT_COMPANY_ROUTES: Routes = [
  { path: '', component: ClientCompanyListComponent },
  { path: 'new', component: ClientCompanyFormComponent, data: { permission: 'CLIENT_COMPANY_CREATE' } },
  { path: ':id', component: ClientCompanyFormComponent },
  { path: ':id/edit', component: ClientCompanyFormComponent, data: { permission: 'CLIENT_COMPANY_UPDATE' } }
];
