import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { VeterinariosComponent } from './veterinarios';
import { AutorizacionService } from '../../core/autorizacion.service';
import { NotificacionService } from '../../core/notificacion.service';
import { Usuario } from '../../core/modelos';
import { environment } from '../../../environments/environment';

const API = environment.apiUrl;

const VETERINARIOS: Usuario[] = [
  {
    id: 1, nombres: 'Carlos', apellidos: 'Mena', email: 'carlos@vet.com', telefono: '0991111111',
    especialidad: 'Cirugia', cedulaProfesional: 'VET-001', biografia: 'Especialista en cirugias veterinarias',
    fotoUrl: 'http://localhost:8080/uploads/carlos.jpg', roles: [{ id: 1, nombre: 'VETERINARIO' }]
  },
  {
    id: 2, nombres: 'Sofia', apellidos: 'Ramos', email: 'sofia@vet.com', telefono: '0992222222',
    especialidad: 'Dermatologia', cedulaProfesional: 'VET-002', biografia: 'Tratamiento de piel y pelaje',
    roles: []
  }
];

describe('VeterinariosComponent', () => {
  let component: VeterinariosComponent;
  let fixture: ComponentFixture<VeterinariosComponent>;
  let httpMock: HttpTestingController;
  let autorizacion: AutorizacionService;
  let notificaciones: NotificacionService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VeterinariosComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    autorizacion = TestBed.inject(AutorizacionService);
    notificaciones = TestBed.inject(NotificacionService);

    autorizacion.guardarSesion({ token: 'tok-admin', roles: ['ROLE_ADMIN'] });
    fixture = TestBed.createComponent(VeterinariosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    httpMock.expectOne(`${API}/veterinarios/publico`).flush({ ok: true, mensaje: '', data: VETERINARIOS });
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('consulta el endpoint publico de veterinarios al arrancar', () => {
    const fx = TestBed.createComponent(VeterinariosComponent);
    fx.detectChanges();

    const peticion = httpMock.expectOne(`${API}/veterinarios/publico`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush({ ok: true, mensaje: '', data: VETERINARIOS });
  });

  it('carga el equipo medico con el listado recibido', () => {
    expect(component.veterinarios.length).toBe(2);
    expect(component.cargando).toBe(false);
    expect(component.veterinarios[0].nombres).toBe('Carlos');
  });

  it('muestra la especialidad, la cedula, la biografia y el contacto de cada veterinario', () => {
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Cirugia');
    expect(texto).toContain('VET-001');
    expect(texto).toContain('Especialista en cirugias veterinarias');
    expect(texto).toContain('carlos@vet.com - 0991111111');
    expect(texto).toContain('Carlos Mena');
  });

  it('permite editar el equipo medico cuando el usuario es administrador', () => {
    expect(component.puedeEditar()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Editar informacion');
  });

  it('oculta la edicion cuando el usuario no es administrador', () => {
    autorizacion.guardarSesion({ token: 'tok-vet', roles: ['ROLE_VETERINARIO'] });
    const fx = TestBed.createComponent(VeterinariosComponent);
    fx.detectChanges();
    httpMock.expectOne(`${API}/veterinarios/publico`).flush({ ok: true, mensaje: '', data: VETERINARIOS });
    fx.detectChanges();

    expect(fx.componentInstance.puedeEditar()).toBe(false);
    expect(fx.nativeElement.textContent).not.toContain('Editar informacion');
  });

  it('copia los datos del veterinario al formulario de edicion', () => {
    component.editar(VETERINARIOS[0]);

    expect(component.editando?.id).toBe(1);
    expect(component.editando?.nombres).toBe('Carlos');
    expect(component.editando?.especialidad).toBe('Cirugia');
    expect(component.editando?.roles.length).toBe(1);
    expect(component.mensaje).toBe('');
  });

  it('guarda los cambios del veterinario contra el endpoint del equipo veterinario', () => {
    component.editar(VETERINARIOS[0]);
    component.editando!.especialidad = 'Cirugia General';
    component.editando!.telefono = '0993333333';

    component.guardar();

    const peticion = httpMock.expectOne(`${API}/equipo-veterinario/1`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body.especialidad).toBe('Cirugia General');
    expect(peticion.request.body.telefono).toBe('0993333333');
    expect(peticion.request.body.nombres).toBe('Carlos');

    peticion.flush({ ok: true, mensaje: '', data: { ...VETERINARIOS[0], especialidad: 'Cirugia General' } });

    expect(component.mensaje).toBe('Veterinario actualizado.');
    expect(component.editando).toBeUndefined();
    expect(component.veterinarios[0].especialidad).toBe('Cirugia General');
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('Veterinario actualizado correctamente.');
  });

  it('no envia nada si no hay un veterinario en edicion', () => {
    component.guardar();
    httpMock.expectNone(`${API}/equipo-veterinario/1`);
    expect(component.mensaje).toBe('');
  });

  it('muestra el mensaje de error devuelto por el backend al guardar', () => {
    component.editar(VETERINARIOS[0]);
    component.guardar();

    const peticion = httpMock.expectOne(`${API}/equipo-veterinario/1`);
    peticion.flush({ mensaje: 'Permiso denegado' }, { status: 403, statusText: 'Forbidden' });

    expect(component.mensaje).toBe('Permiso denegado');
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('Permiso denegado');
    expect(component.editando?.id).toBe(1);
  });

  it('muestra un mensaje generico cuando el backend falla sin detalle', () => {
    component.editar(VETERINARIOS[0]);
    component.guardar();

    const peticion = httpMock.expectOne(`${API}/equipo-veterinario/1`);
    peticion.flush('fallo del servidor', { status: 500, statusText: 'Server Error' });

    expect(component.mensaje).toBe('No se pudo guardar. Verifica la conexión y los permisos de administrador.');
  });

  it('cierra el formulario de edicion sin guardar cambios', () => {
    component.editar(VETERINARIOS[0]);
    component.cerrar();
    expect(component.editando).toBeUndefined();
  });

  it('informa del error devuelto por el backend al listar el equipo medico', () => {
    const fx = TestBed.createComponent(VeterinariosComponent);
    fx.detectChanges();

    httpMock.expectOne(`${API}/veterinarios/publico`).flush({ mensaje: 'Sin permiso' }, { status: 403, statusText: 'Forbidden' });

    expect(fx.componentInstance.veterinarios.length).toBe(0);
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('Sin permiso');
  });

  it('muestra un mensaje generico cuando el listado del equipo medico falla sin detalle', () => {
    const fx = TestBed.createComponent(VeterinariosComponent);
    fx.detectChanges();

    httpMock.expectOne(`${API}/veterinarios/publico`).flush('fallo del servidor', { status: 500, statusText: 'Server Error' });

    expect(fx.componentInstance.veterinarios.length).toBe(0);
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('No se pudo cargar el equipo de veterinarios.');
  });
});
