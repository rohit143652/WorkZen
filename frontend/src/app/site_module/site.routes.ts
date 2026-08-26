import { Routes } from '@angular/router';
import { SiteListComponent } from './components/site-list/site-list.component';
import { SiteFormComponent } from './components/site-form/site-form.component';

export const SITE_ROUTES: Routes = [
  { path: '', component: SiteListComponent },
  { path: 'new', component: SiteFormComponent, data: { permission: 'SITE_CREATE' } },
  { path: ':id/edit', component: SiteFormComponent, data: { permission: 'SITE_UPDATE' } }
];
