import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { AdministracionComponent } from './administracion';
import { NotificacionService } from '../../core/notificacion.service';
import { Usuario } from '../../core/modelos';
import { environment } from '../../../environments/environment';

describe('AdministracionComponent', () => {
  let component: AdministracionComponent;
  let fixture: ComponentFixture<AdministracionComponent>;
  let httpMock: HttpTestingController;
  let notificaciones: NotificacionService;

  const URL_INDICADORES = `${environment.apiUrl}/administracion/indicadores`;
  const URL_CODIGO_REGISTRO = `${environment.apiUrl}/administracion/codigo-registro`;
  const URL_SOLICITUDES = `${environment.apiUrl}/admin/usuarios/solicitudes-pendientes`;
  const URL_USUARIOS = `${environment.apiUrl}/admin/usuarios`;

  const INDICADORES: Record<string, number> = {
    usuarios: 12,
    veterinarios: 4,
    solicitudes: 2,
    clientes: 30
  };

  const SOLICITUDES: Usuario[] = [
    {
      id: 21, nombres: 'Diego', apellidos: 'Soto', email: 'diego@vet.com', telefono: '0995555555',
      roles: [{ id: 3, nombre: 'ROLE_RECEPCIONISTA' }]
    },
    {
      id: 22, nombres: 'Sara', apellidos: 'Gil', email: 'sara@vet.com', telefono: '0996666666',
      roles: [{ id: 2, nombre: 'ROLE_VETERINARIO' }]
    }
  ];

  function textos(): string {
    return fixture.nativeElement.textContent || '';
  }

  /** Copias de los datos: el componente modifica el objeto de indicadores en memoria. */
  function datosIndicadores(): Record<string, number> {
    return { ...INDICADORES };
  }

  function datosSolicitudes(): Usuario[] {
    return SOLICITUDES.map(u => ({ ...u, roles: u.roles.map(r => ({ ...r })) }));
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdministracionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    notificaciones = TestBed.inject(NotificacionService);

    fixture = TestBed.createComponent(AdministracionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // Peticiones iniciales de ngOnInit.
    const reqCodigo = httpMock.expectOne(URL_CODIGO_REGISTRO);
    reqCodigo.flush({ ok: true, mensaje: '', data: { codigo: '123456', segundosRestantes: 30 } });
    const reqIndicadores = httpMock.expectOne((req) => req.method === 'GET' && req.url === URL_INDICADORES);
    reqIndicadores.flush({ ok: true, mensaje: '', data: datosIndicadores() });
    const reqSolicitudes = httpMock.expectOne((req) => req.method === 'GET' && req.url === URL_SOLICITUDES);
    reqSolicitudes.flush({ ok: true, mensaje: '', data: datosSolicitudes() });
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('carga del servidor los indicadores administrativos y las solicitudes pendientes', () => {
    expect(component.indicadores['usuarios']).toBe(12);
    expect(component.indicadores['solicitudes']).toBe(2);
    expect(component.solicitudes.length).toBe(2);
    expect(component.cargando).toBe(false);

    expect(textos()).toContain('usuarios');
    expect(textos()).toContain('12');
    expect(textos()).toContain('diego@vet.com');
    expect(textos()).toContain('sara@vet.com');
  });

  it('muestra el codigo de registro para el personal de la clinica', () => {
    expect(textos()).toContain('Código de Registro para Personal');
    expect(textos()).toContain('••••••');
    expect(textos()).not.toContain('123456');
    component.alternarCodigo();
    fixture.detectChanges();
    expect(textos()).toContain('123456');
  });

  it('aprueba una solicitud, la retira de la lista y descuenta el indicador', () => {
    component.aprobar(21);

    const req = httpMock.expectOne({ method: 'PUT', url: `${URL_USUARIOS}/21/aprobar` });
    expect(req.request.body).toEqual({});
    req.flush({ ok: true, mensaje: '', data: SOLICITUDES[0] });
    fixture.detectChanges();

    expect(component.solicitudes.length).toBe(1);
    expect(component.solicitudes[0].id).toBe(22);
    expect(component.indicadores['solicitudes']).toBe(1);
    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Solicitud aprobada correctamente.');
    expect(textos()).not.toContain('diego@vet.com');
  });

  it('rechaza una solicitud, la retira de la lista y descuenta el indicador', () => {
    component.rechazar(22);

    const req = httpMock.expectOne({ method: 'PUT', url: `${URL_USUARIOS}/22/rechazar` });
    expect(req.request.body).toEqual({});
    req.flush({ ok: true, mensaje: '', data: null });
    fixture.detectChanges();

    expect(component.solicitudes.length).toBe(1);
    expect(component.solicitudes[0].id).toBe(21);
    expect(component.indicadores['solicitudes']).toBe(1);
    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Solicitud rechazada.');
    expect(textos()).not.toContain('sara@vet.com');
  });

  it('el contador de solicitudes pendientes nunca queda por debajo de cero', () => {
    component.indicadores['solicitudes'] = 0;

    component.aprobar(21);

    httpMock.expectOne({ method: 'PUT', url: `${URL_USUARIOS}/21/aprobar` })
      .flush({ ok: true, mensaje: '', data: SOLICITUDES[0] });

    expect(component.indicadores['solicitudes']).toBe(0);
  });

  it('muestra el mensaje del backend cuando no se puede aprobar la solicitud', () => {
    component.aprobar(21);

    httpMock.expectOne({ method: 'PUT', url: `${URL_USUARIOS}/21/aprobar` })
      .flush({ mensaje: 'La solicitud ya fue procesada' }, { status: 409, statusText: 'Conflict' });

    expect(component.solicitudes.length).toBe(2);
    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('La solicitud ya fue procesada');
  });

  it('muestra el mensaje generico cuando el backend no devuelve detalle al aprobar', () => {
    component.aprobar(21);

    httpMock.expectOne({ method: 'PUT', url: `${URL_USUARIOS}/21/aprobar` })
      .flush(null, { status: 500, statusText: 'Error' });

    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('No se pudo aprobar la solicitud.');
  });

  it('muestra el mensaje generico cuando el backend no devuelve detalle al rechazar', () => {
    component.rechazar(21);

    httpMock.expectOne({ method: 'PUT', url: `${URL_USUARIOS}/21/rechazar` })
      .flush(null, { status: 500, statusText: 'Error' });

    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('No se pudo rechazar la solicitud.');
  });

  it('la tabla de solicitudes muestra el rol y los datos de contacto del servidor', () => {
    expect(textos()).toContain('ROLE_RECEPCIONISTA');
    expect(textos()).toContain('ROLE_VETERINARIO');
    expect(textos()).toContain('0995555555');
  });
});
