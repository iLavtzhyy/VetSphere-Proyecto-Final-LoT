import { EnvironmentInjector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { SesionInactividadService } from './sesion-inactividad.service';
import { AutorizacionService } from './autorizacion.service';

describe('authGuard', () => {
  let injector: EnvironmentInjector;
  let autorizacion: { tieneRol: jasmine.Spy };
  const sesion = { sesionActiva: jasmine.createSpy('sesionActiva') };

  const ejecutar = (roles?: string[]) => {
    const route = { data: roles ? { roles } : {} } as unknown as ActivatedRouteSnapshot;
    return runInInjectionContext(injector, () => authGuard(route, {} as never));
  };

  beforeEach(() => {
    sesion.sesionActiva.calls.reset();
    autorizacion = { tieneRol: jasmine.createSpy('tieneRol') };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: SesionInactividadService, useValue: sesion },
        { provide: AutorizacionService, useValue: autorizacion }
      ]
    });
    injector = TestBed.inject(EnvironmentInjector);
  });

  it('redirige a /login cuando no hay sesion activa', () => {
    sesion.sesionActiva.and.returnValue(false);
    const resultado = ejecutar(['ROLE_ADMIN']) as UrlTree;
    expect(resultado instanceof UrlTree).toBe(true);
    expect(resultado.toString()).toBe('/login');
  });

  it('permite el acceso cuando hay sesion y el rol esta permitido', () => {
    sesion.sesionActiva.and.returnValue(true);
    autorizacion.tieneRol.and.returnValue(true);
    expect(ejecutar(['ROLE_ADMIN'])).toBe(true);
    expect(autorizacion.tieneRol).toHaveBeenCalledWith(['ROLE_ADMIN']);
  });

  it('permite el acceso cuando la ruta no restringe roles', () => {
    sesion.sesionActiva.and.returnValue(true);
    autorizacion.tieneRol.and.returnValue(true);
    expect(ejecutar()).toBe(true);
    expect(autorizacion.tieneRol).toHaveBeenCalledWith(undefined);
  });

  it('redirige al dashboard cuando el rol no esta permitido', () => {
    sesion.sesionActiva.and.returnValue(true);
    autorizacion.tieneRol.and.returnValue(false);
    const resultado = ejecutar(['ROLE_ADMIN']) as UrlTree;
    expect(resultado.toString()).toBe('/dashboard');
  });

  it('redirige a /mascotas cuando el usuario es ROLE_CLIENTE y su rol no esta permitido', () => {
    sesion.sesionActiva.and.returnValue(true);
    autorizacion.tieneRol.and.callFake((roles?: string[]) => (roles || []).includes('ROLE_CLIENTE'));
    const resultado = ejecutar(['ROLE_ADMIN']) as UrlTree;
    expect(resultado.toString()).toBe('/mascotas');
  });
});
