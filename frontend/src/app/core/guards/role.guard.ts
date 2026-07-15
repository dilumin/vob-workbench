import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from '../models/api.types';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const allowedRoles = (route.data['roles'] || []) as Role[];
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!allowedRoles.length || auth.hasRole(allowedRoles)) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};
