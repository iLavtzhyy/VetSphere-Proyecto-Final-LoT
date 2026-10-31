import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SesionInactividadService } from './sesion-inactividad.service';
import { AutorizacionService } from './autorizacion.service';

export const authGuard: CanActivateFn = (route) => {
  const router = inject(Router);
  const sesionInactividad = inject(SesionInactividadService);
  const autorizacion = inject(AutorizacionService);
  if (!sesionInactividad.sesionActiva()) return router.createUrlTree(['/login']);
  const rolesPermitidos = route.data?.['roles'] as string[] | undefined;
  const fallback = autorizacion.tieneRol(['ROLE_CLIENTE']) ? '/mascotas' : '/dashboard';
  return autorizacion.tieneRol(rolesPermitidos) ? true : router.createUrlTree([fallback]);
};
