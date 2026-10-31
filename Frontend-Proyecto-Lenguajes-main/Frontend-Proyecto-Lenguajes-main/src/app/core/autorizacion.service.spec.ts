import { TestBed } from '@angular/core/testing';
import { AutorizacionService } from './autorizacion.service';

describe('AutorizacionService', () => {
  let servicio: AutorizacionService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    servicio = TestBed.inject(AutorizacionService);
  });

  afterEach(() => localStorage.clear());

  it('lee los roles almacenados en localStorage', () => {
    localStorage.setItem('vetsphere_roles', JSON.stringify(['ROLE_ADMIN', 'ROLE_VET']));
    expect(servicio.roles()).toEqual(['ROLE_ADMIN', 'ROLE_VET']);
  });

  it('devuelve una lista vacia cuando no hay roles guardados', () => {
    expect(servicio.roles()).toEqual([]);
  });

  it('devuelve una lista vacia cuando el JSON de roles esta corrupto', () => {
    localStorage.setItem('vetsphere_roles', '{corrupto');
    expect(servicio.roles()).toEqual([]);
  });

  it('tieneRol permite el acceso cuando no se especifican roles', () => {
    expect(servicio.tieneRol()).toBe(true);
    expect(servicio.tieneRol([])).toBe(true);
  });

  it('tieneRol es true cuando el usuario posee alguno de los roles permitidos', () => {
    servicio.guardarSesion({ token: 'tok', roles: ['ROLE_VET'], nombre: 'Ana' });
    expect(servicio.tieneRol(['ROLE_ADMIN', 'ROLE_VET'])).toBe(true);
  });

  it('tieneRol es false cuando el usuario no posee ningun rol permitido', () => {
    servicio.guardarSesion({ token: 'tok', roles: ['ROLE_CLIENTE'], nombre: 'Luis' });
    expect(servicio.tieneRol(['ROLE_ADMIN'])).toBe(false);
  });

  it('guardarSesion persiste token, roles y nombre en localStorage', () => {
    servicio.guardarSesion({ token: 'tok-123', roles: ['ROLE_ADMIN'], nombre: 'Ana' });
    expect(localStorage.getItem('vetsphere_token')).toBe('tok-123');
    expect(localStorage.getItem('vetsphere_roles')).toEqual(JSON.stringify(['ROLE_ADMIN']));
    expect(localStorage.getItem('vetsphere_nombre')).toBe('Ana');
    expect(servicio.nombre()).toBe('Ana');
  });

  it('guardarSesion usa valores por defecto si faltan roles o nombre', () => {
    servicio.guardarSesion({ token: 'tok-123' });
    expect(servicio.roles()).toEqual([]);
    expect(servicio.nombre()).toBe('Usuario VetSphere');
  });

  it('limpiarSesion borra las claves y reinicia los valores en cache', () => {
    servicio.guardarSesion({ token: 'tok-123', roles: ['ROLE_ADMIN'], nombre: 'Ana' });
    servicio.limpiarSesion();
    expect(localStorage.getItem('vetsphere_token')).toBeNull();
    expect(localStorage.getItem('vetsphere_roles')).toBeNull();
    expect(localStorage.getItem('vetsphere_nombre')).toBeNull();
    expect(servicio.roles()).toEqual([]);
    expect(servicio.nombre()).toBe('Usuario VetSphere');
  });
});
