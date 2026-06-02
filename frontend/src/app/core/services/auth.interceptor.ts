import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const token = authService.token;
  const headers: Record<string, string> = {
    'X-Docuvra-Role': authService.openRole
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return next(request.clone({
    setHeaders: headers
  }));
};
