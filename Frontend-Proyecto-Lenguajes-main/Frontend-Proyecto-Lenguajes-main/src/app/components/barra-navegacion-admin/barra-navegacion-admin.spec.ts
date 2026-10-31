import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { LayoutComponent } from './barra-navegacion-admin';
import { AutorizacionService } from '../../core/autorizacion.service';

describe('LayoutComponent', () => {
  let component: LayoutComponent;
  let fixture: ComponentFixture<LayoutComponent>;
  let autorizacion: AutorizacionService;

  /** Inicia sesion con los roles indicados y monta el componente. */
  function sesionCon(roles: string[], nombre = 'Usuario Prueba') {
    localStorage.setItem('vetsphere_token', 'token-de-prueba');
    localStorage.setItem('vetsphere_roles', JSON.stringify(roles));
    localStorage.setItem('vetsphere_nombre', nombre);
    autorizacion.guardarSesion({ token: 'token-de-prueba', roles, nombre });

    fixture = TestBed.createComponent(LayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function titulosDeGrupos() {
    return component.gruposVisibles.map(grupo => grupo.titulo);
  }

  function rutasDeItems() {
    return component.gruposVisibles.flatMap(grupo => grupo.items.map(item => item.ruta));
  }

  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();

    await TestBed.configureTestingModule({
      imports: [LayoutComponent],
      providers: [provideRouter([]), provideNoopAnimations(), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    autorizacion = TestBed.inject(AutorizacionService);
  });

  afterEach(() => {
    autorizacion.limpiarSesion();
    localStorage.clear();
  });

  it('debe crearse correctamente', () => {
    sesionCon(['ROLE_ADMIN']);
    expect(component).toBeTruthy();
  });

  it('debe mostrar todos los grupos al administrador', () => {
    sesionCon(['ROLE_ADMIN']);
    expect(titulosDeGrupos()).toEqual(['Principal', 'Gestion', 'Clinica', 'Administracion']);
  });

  it('debe ocultar al recepcionista los accesos que son solo del administrador', () => {
    sesionCon(['ROLE_RECEPCIONISTA']);

    expect(rutasDeItems()).not.toContain('/administracion');
    expect(rutasDeItems()).not.toContain('/usuarios');
    expect(rutasDeItems()).not.toContain('/reportes');
    // Del grupo de administracion solo le corresponde la facturacion.
    const administracion = component.gruposVisibles.find(grupo => grupo.titulo === 'Administracion');
    expect(administracion).toBeTruthy();
    expect(administracion!.items.map(item => item.nombre)).toEqual(['Facturacion']);
  });

  it('debe dejar al recepcionista solo con farmacia dentro de Clinica', () => {
    sesionCon(['ROLE_RECEPCIONISTA']);

    const clinica = component.gruposVisibles.find(grupo => grupo.titulo === 'Clinica');
    expect(clinica).toBeTruthy();
    expect(clinica!.items.map(item => item.nombre)).toEqual(['Farmacia']);
  });

  it('debe dar al veterinario los modulos clinicos y ninguno de farmacia', () => {
    sesionCon(['ROLE_VETERINARIO']);

    expect(rutasDeItems()).toContain('/consultas');
    expect(rutasDeItems()).toContain('/cirugias');
    expect(rutasDeItems()).toContain('/laboratorio');
    expect(rutasDeItems()).toContain('/hospitalizacion');
    expect(rutasDeItems()).not.toContain('/farmacia');
    expect(rutasDeItems()).not.toContain('/administracion');
  });

  it('debe dejar al cliente solo con su cuenta, sus mascotas y el equipo', () => {
    sesionCon(['ROLE_CLIENTE']);

    expect(rutasDeItems()).toEqual(['/mi-cuenta', '/mascotas', '/veterinarios']);
    expect(titulosDeGrupos()).not.toContain('Clinica');
    expect(titulosDeGrupos()).not.toContain('Administracion');
  });

  it('debe mostrar el nombre y los roles de la sesion iniciada', () => {
    sesionCon(['ROLE_ADMIN', 'ROLE_VETERINARIO'], 'Dra. Sofia Cardenas');

    expect(component.nombreSesion).toBe('Dra. Sofia Cardenas');
    expect(component.rolesSesion).toContain('ROLE_ADMIN');
    expect(component.rolesSesion).toContain('ROLE_VETERINARIO');
  });

  it('debe avisar cuando la sesion no tiene ningun rol asignado', () => {
    sesionCon([]);
    expect(component.rolesSesion).toBe('Sin rol asignado');
  });

  it('debe dibujar en el menu solo los accesos permitidos', () => {
    sesionCon(['ROLE_CLIENTE']);

    const enlaces = fixture.nativeElement.querySelectorAll('a.item') as NodeListOf<HTMLAnchorElement>;
    expect(enlaces.length).toBe(3);
  });

  it('debe cerrar la sesion y volver al acceso al salir', () => {
    sesionCon(['ROLE_ADMIN']);
    const router = TestBed.inject(Router);
    const navegar = spyOn(router, 'navigateByUrl');

    component.salir();

    expect(navegar).toHaveBeenCalledWith('/login');
    expect(localStorage.getItem('vetsphere_token')).toBeNull();
    expect(localStorage.getItem('vetsphere_roles')).toBeNull();
    expect(localStorage.getItem('vetsphere_nombre')).toBeNull();
  });

  it('debe cerrar el menu movil', () => {
    sesionCon(['ROLE_ADMIN']);
    component.menuAbierto = true;
    component.cerrarMenu();
    expect(component.menuAbierto).toBe(false);
  });
});
