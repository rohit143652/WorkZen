import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { guestGuard } from '../core/guards/guest.guard';

export const LOGIN_ROUTES: Routes = [
  { path: '', component: LoginComponent, canActivate: [guestGuard] }
];
