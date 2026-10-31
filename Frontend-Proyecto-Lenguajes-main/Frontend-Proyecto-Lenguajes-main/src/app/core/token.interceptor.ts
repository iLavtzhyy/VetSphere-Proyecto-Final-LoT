import { HttpInterceptorFn } from '@angular/common/http';
import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { SesionInactividadService } from './sesion-inactividad.service';
import { environment } from '../../environments/environment';

export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const sesionInactividad = inject(SesionInactividadService);
  const token = localStorage.getItem('vetsphere_token');
  const peticion = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(peticion).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Only the VetSphere REST API is authoritative for the browser session.
        // A downstream service (for example IoT) may reject a request because
        // of its own configuration; that must not erase the user's login.
        const esApiVetSphere = req.url.startsWith(environment.apiUrl);
        if (esApiVetSphere) {
          sesionInactividad.cerrarSesion('Tu token expiro o no es valido. Inicia sesion nuevamente.');
        }
      }
      return throwError(() => error);
    })
  );
};
