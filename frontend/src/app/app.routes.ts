import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';
import { AppShellComponent } from './shared/components/app-shell/app-shell.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'login',
    loadChildren: () => import('./login_module/login.routes').then(m => m.LOGIN_ROUTES)
  },
  {
    path: 'change-password',
    canActivate: [authGuard],
    loadChildren: () => import('./login_module/change-password.routes').then(m => m.CHANGE_PASSWORD_ROUTES)
  },
  {
    // All authenticated, shell-wrapped pages live under this parent so the
    // sidebar/header render exactly once and persist across navigation.
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadChildren: () => import('./dashboard_module/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
      },
      {
        path: 'employees',
        canActivate: [permissionGuard],
        data: { permission: 'EMPLOYEE_READ' },
        loadChildren: () => import('./employee_module/employee.routes').then(m => m.EMPLOYEE_ROUTES)
      },
      {
        path: 'org-settings',
        canActivate: [permissionGuard],
        data: { permission: 'DEPARTMENT_READ' },
        loadChildren: () => import('./department_module/org-settings.routes').then(m => m.ORG_SETTINGS_ROUTES)
      },
      {
        path: 'clients',
        canActivate: [permissionGuard],
        data: { permission: 'CLIENT_COMPANY_READ' },
        loadChildren: () => import('./client_company_module/client-company.routes').then(m => m.CLIENT_COMPANY_ROUTES)
      },
      {
        path: 'sites',
        canActivate: [permissionGuard],
        data: { permission: 'SITE_READ' },
        loadChildren: () => import('./site_module/site.routes').then(m => m.SITE_ROUTES)
      },
      {
        path: 'employee-assignments',
        canActivate: [permissionGuard],
        data: { permission: 'EMPLOYEE_ASSIGNMENT_READ' },
        loadChildren: () => import('./employee_assignment_module/employee-assignment.routes').then(m => m.EMPLOYEE_ASSIGNMENT_ROUTES)
      },
      {
        // No single permission gate here - the two child routes (mark vs.
        // history) each require a different permission and guard
        // themselves individually, since a role could plausibly hold one
        // without the other (e.g. a future read-only Accountant-style role).
        path: 'attendance',
        loadChildren: () => import('./attendance_module/attendance.routes').then(m => m.ATTENDANCE_ROUTES)
      },
      {
        path: 'holidays',
        loadChildren: () => import('./holiday_module/holiday.routes').then(m => m.HOLIDAY_ROUTES)
      },
      {
        path: 'leave-requests',
        loadChildren: () => import('./leave_request_module/leave-request.routes').then(m => m.LEAVE_REQUEST_ROUTES)
      },
      {
        path: 'employee-exits',
        loadChildren: () => import('./exit_module/exit.routes').then(m => m.EXIT_ROUTES)
      },
      {
        // Sub-routes (settings) guard themselves individually.
        path: 'paid-leave',
        loadChildren: () => import('./leave_module/leave.routes').then(m => m.LEAVE_ROUTES)
      },
      {
        path: 'payroll',
        loadChildren: () => import('./payroll_module/payroll.routes').then(m => m.PAYROLL_ROUTES)
      },
      {
        path: 'advances',
        loadChildren: () => import('./advance_module/advance.routes').then(m => m.ADVANCE_ROUTES)
      },
      {
        path: 'salary-structures',
        canActivate: [permissionGuard],
        data: { permission: 'SALARY_STRUCTURE_READ' },
        loadChildren: () => import('./salary_structure_module/salary-structure.routes').then(m => m.SALARY_STRUCTURE_ROUTES)
      },
      {
        path: 'salary-components',
        canActivate: [permissionGuard],
        data: { permission: 'SALARY_STRUCTURE_READ' },
        loadChildren: () => import('./salary_structure_module/salary-structure.routes').then(m => m.SALARY_COMPONENT_ROUTES)
      },
      {
        path: 'users',
        canActivate: [permissionGuard],
        data: { permission: 'USER_READ' },
        loadChildren: () => import('./user_module/user.routes').then(m => m.USER_ROUTES)
      },
      {
        path: 'roles',
        canActivate: [permissionGuard],
        data: { permission: 'ROLE_READ' },
        loadChildren: () => import('./role_module/role.routes').then(m => m.ROLE_ROUTES)
      },
      {
        path: 'permissions',
        canActivate: [permissionGuard],
        data: { permission: 'PERMISSION_READ' },
        loadChildren: () => import('./permission_module/permission.routes').then(m => m.PERMISSION_ROUTES)
      },
      {
        path: 'audit-logs',
        canActivate: [permissionGuard],
        data: { permission: 'AUDIT_LOG_READ' },
        loadChildren: () => import('./audit_module/audit.routes').then(m => m.AUDIT_ROUTES)
      }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
