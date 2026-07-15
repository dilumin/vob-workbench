import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
    
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/app-shell/app-shell.component').then((m) => m.AppShellComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'patients',
        loadComponent: () => import('./features/patients/patients.component').then((m) => m.PatientsComponent),
      },
      {
        path: 'vobs',
        loadComponent: () => import('./features/vobs/vob-list.component').then((m) => m.VobListComponent),
      },
      {
        path: 'vobs/:id',
        loadComponent: () => import('./features/vobs/vob-detail.component').then((m) => m.VobDetailComponent),
      },
      {
        path: 'admin',
        canActivate: [roleGuard],
        data: { roles: ['SUPERVISOR_ADMIN'] },
        loadComponent: () => import('./features/admin/admin.component').then((m) => m.AdminComponent),
      },
      {
        path: 'audit',
        canActivate: [roleGuard],
        data: { roles: ['SUPERVISOR_ADMIN'] },
        loadComponent: () => import('./features/audit/audit.component').then((m) => m.AuditComponent),
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
