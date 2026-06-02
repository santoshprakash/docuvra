import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.loadCurrentUser().pipe(
    map(user => {
      if (user.loginEnabled && user.forcePasswordChange) {
        return router.createUrlTree(['/change-password']);
      }
      return true;
    }),
    catchError(() => of(router.createUrlTree(['/login'])))
  );
};

export const passwordChangeGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.loadCurrentUser().pipe(
    map(() => true),
    catchError(() => of(router.createUrlTree(['/login'])))
  );
};

export const supervisorGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.loadCurrentUser().pipe(
    map(user => user.role === 'SUPERVISOR' ? true : router.createUrlTree(['/documents'])),
    catchError(() => of(router.createUrlTree(['/login'])))
  );
};
