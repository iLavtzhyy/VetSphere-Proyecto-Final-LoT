import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ClientesComponent } from './clientes';
import { NotificacionService } from '../../core/notificacion.service';
import { Cliente, Mascota } from '../../core/modelos';
import { environment } from '../../../environments/environment';

const API = environment.apiUrl;

function mascota(id: number, nombre: string): Mascota {
  return {
    id, nombre, especie: 'Canino', raza: 'Labrador', sexo: 'Macho', color: 'Marron',
    pesoKg: 10, fechaNacimiento: '2021-03-01', estado: 'ACTIVA',
    propietario: null, historiales: null, cirugias: null
  };
}

const CLIENTES: Cliente[] = [
  {
    id: 1, nombres: 'Ana', apellidos: 'Ruiz', email: 'ana@correo.com', telefono: '0999999999',
    ciudad: 'Quito', documento: '1712345678', estado: 'Activo', totalGastado: 120.5,
    mascotas: [mascota(11, 'Firulais'), mascota(12, 'Nube')], roles: []
  },
  {
    id: 2, nombres: 'Luis', apellidos: 'Perez', email: 'luis@correo.com', telefono: '0888888888',
    ciudad: 'Guayaquil', documento: '0912345678', estado: 'Inactivo', totalGastado: 30,
    mascotas: [mascota(13, 'Michi')], roles: []
  },
  {
    id: 3, nombres: 'Marta', apellidos: 'Soto', email: 'marta@correo.com', telefono: '0777777777',
    roles: []
  }
];

describe('ClientesComponent', () => {
  let component: ClientesComponent;
  let fixture: ComponentFixture<ClientesComponent>;
  let httpMock: HttpTestingController;
  let notificaciones: NotificacionService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClientesComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    notificaciones = TestBed.inject(NotificacionService);

    fixture = TestBed.createComponent(ClientesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    httpMock.expectOne(`${API}/clientes`).flush({ ok: true, mensaje: '', data: CLIENTES });
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('carga los clientes en el arranque', () => {
    expect(component.clientes.length).toBe(3);
    expect(component.cargando).toBe(false);
  });

  it('muestra los indicadores de clientes, mascotas, gasto y activos', () => {
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Ana Ruiz');
    expect(texto).toContain('ana@correo.com');
    expect(texto).toContain('0999999999');
    expect(texto).toContain('$150.50');
    expect(texto).toContain('$120.50');
    expect(texto).toContain('$30.00');
  });

  it('suma el numero de mascotas asociadas de todos los clientes', () => {
    expect(component.totalMascotas).toBe(3);
  });

  it('suma el gasto acumulado de todos los clientes', () => {
    expect(component.totalGastado).toBe(150.5);
  });

  it('cuenta como activos a los clientes con estado Activo o sin estado definido', () => {
    expect(component.activos).toBe(2);
  });

  it('filtra los clientes por nombre', () => {
    component.busqueda = 'ana';
    expect(component.filtrados.map(c => c.id)).toEqual([1]);
  });

  it('filtra los clientes por email', () => {
    component.busqueda = 'luis@correo.com';
    expect(component.filtrados.map(c => c.id)).toEqual([2]);
  });

  it('filtra los clientes por telefono', () => {
    component.busqueda = '0777777777';
    expect(component.filtrados.map(c => c.id)).toEqual([3]);
  });

  it('devuelve una lista vacia cuando la busqueda no coincide con ningun cliente', () => {
    component.busqueda = 'zzzz';
    expect(component.filtrados.length).toBe(0);
  });

  it('abre el formulario de alta con los valores por defecto', () => {
    component.abrirNuevoCliente();
    expect(component.mostrarFormCliente).toBe(true);
    expect(component.esNuevo).toBe(true);
    expect(component.clienteEdicion.nombres).toBe('');
    expect(component.clienteEdicion.ciudad).toBe('Quito');
    expect(component.clienteEdicion.estado).toBe('Activo');
  });

  it('no guarda el cliente si falta el nombre', () => {
    component.abrirNuevoCliente();
    component.clienteEdicion.apellidos = 'Ruiz';
    component.clienteEdicion.email = 'ana@correo.com';

    component.guardarCliente();

    httpMock.expectNone(`${API}/clientes`);
    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Completa nombres, apellidos, correo y una cédula válida de 10 dígitos.');
  });

  it('no guarda el cliente si falta el email', () => {
    component.abrirNuevoCliente();
    component.clienteEdicion.nombres = 'Ana';
    component.clienteEdicion.apellidos = 'Ruiz';

    component.guardarCliente();

    httpMock.expectNone(`${API}/clientes`);
    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Completa nombres, apellidos, correo y una cédula válida de 10 dígitos.');
  });

  it('no registra un cliente sin una cédula de 10 dígitos', () => {
    component.abrirNuevoCliente();
    component.clienteEdicion.nombres = 'Ana';
    component.clienteEdicion.apellidos = 'Ruiz';
    component.clienteEdicion.email = 'ana@correo.com';
    component.clienteEdicion.documento = '12345';

    component.guardarCliente();

    httpMock.expectNone(`${API}/clientes`);
    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Completa nombres, apellidos, correo y una cédula válida de 10 dígitos.');
  });

  it('crea el cliente con los datos del formulario y recarga la lista', () => {
    component.abrirNuevoCliente();
    component.clienteEdicion.nombres = 'Pedro';
    component.clienteEdicion.apellidos = 'Crespo';
    component.clienteEdicion.email = 'pedro@correo.com';
    component.clienteEdicion.telefono = '0966666666';
    component.clienteEdicion.documento = '1712345678';

    component.guardarCliente();

    const peticion = httpMock.expectOne(`${API}/clientes`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body.nombres).toBe('Pedro');
    expect(peticion.request.body.apellidos).toBe('Crespo');
    expect(peticion.request.body.email).toBe('pedro@correo.com');
    expect(peticion.request.body.telefono).toBe('0966666666');
    expect(peticion.request.body.documento).toBe('1712345678');
    expect(peticion.request.body.ciudad).toBe('Quito');

    peticion.flush({ ok: true, mensaje: 'Cliente guardado correctamente.', data: CLIENTES[0] });
    httpMock.expectOne(`${API}/clientes`).flush({ ok: true, mensaje: '', data: CLIENTES });
    fixture.detectChanges();

    expect(component.mostrarFormCliente).toBe(false);
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('Cliente guardado correctamente.');
  });

  it('muestra el mensaje del servidor y no recarga la lista cuando la respuesta viene con ok false', () => {
    component.abrirNuevoCliente();
    component.clienteEdicion.nombres = 'Pedro';
    component.clienteEdicion.apellidos = 'Crespo';
    component.clienteEdicion.email = 'pedro@correo.com';
    component.clienteEdicion.documento = '1712345678';

    component.guardarCliente();

    const peticion = httpMock.expectOne(`${API}/clientes`);
    peticion.flush({ ok: false, mensaje: 'El correo ya esta registrado', data: null });

    httpMock.expectNone(`${API}/clientes`);
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('El correo ya esta registrado');
    expect(component.mostrarFormCliente).toBe(true);
  });

  it('actualiza el cliente en edicion contra el endpoint del cliente', () => {
    component.abrirEditarCliente(CLIENTES[0]);
    expect(component.esNuevo).toBe(false);
    expect(component.mostrarFormCliente).toBe(true);
    expect(component.clienteEdicion.id).toBe(1);

    component.clienteEdicion.telefono = '0900000000';
    component.guardarCliente();

    const peticion = httpMock.expectOne(`${API}/clientes/1`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body.telefono).toBe('0900000000');
    expect(peticion.request.body.nombres).toBe('Ana');

    peticion.flush({ ok: true, mensaje: 'Cliente actualizado correctamente.', data: CLIENTES[0] });
    httpMock.expectOne(`${API}/clientes`).flush({ ok: true, mensaje: '', data: CLIENTES });

    expect(component.mostrarFormCliente).toBe(false);
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('Cliente actualizado correctamente.');
  });

  it('informa del error devuelto por el backend al actualizar un cliente', () => {
    component.abrirEditarCliente(CLIENTES[0]);
    component.guardarCliente();

    const peticion = httpMock.expectOne(`${API}/clientes/1`);
    peticion.flush({ ok: false, mensaje: 'No se pudo actualizar el cliente', data: null });

    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('No se pudo actualizar el cliente');
  });

  it('abre el detalle del cliente con sus mascotas asociadas', () => {
    component.abrirDetallesCliente(CLIENTES[0]);
    fixture.detectChanges();

    const texto = fixture.nativeElement.textContent;
    expect(component.clienteDetalles?.id).toBe(1);
    expect(texto).toContain('Detalles del Cliente');
    expect(texto).toContain('Mascotas registradas (2)');
    expect(texto).toContain('Firulais');
    expect(texto).toContain('Nube');
    expect(texto).toContain('1712345678');
  });

  it('indica que el cliente no tiene mascotas asociadas cuando la lista esta vacia', () => {
    component.abrirDetallesCliente(CLIENTES[2]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No tiene mascotas asociadas.');
    expect(fixture.nativeElement.textContent).toContain('Mascotas registradas (0)');
  });

  it('cierra el detalle del cliente', () => {
    component.abrirDetallesCliente(CLIENTES[0]);
    component.cerrarDetallesCliente();
    expect(component.clienteDetalles).toBeNull();
  });

  it('cierra el formulario de cliente y descarta la edicion', () => {
    component.abrirEditarCliente(CLIENTES[0]);
    component.cerrarFormCliente();
    expect(component.mostrarFormCliente).toBe(false);
    expect(component.clienteEdicion).toEqual({});
  });

  it('informa del error cuando el backend no responde al listar los clientes', () => {
    const fx = TestBed.createComponent(ClientesComponent);
    fx.detectChanges();

    httpMock.expectOne(`${API}/clientes`).flush({ mensaje: 'Sin permiso' }, { status: 403, statusText: 'Forbidden' });

    expect(fx.componentInstance.clientes.length).toBe(0);
    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('No se pudieron cargar los clientes. Revisa que el backend esté activo y tu rol tenga permiso.');
  });
});
