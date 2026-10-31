import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { CirugiasComponent } from './cirugias';
import { environment } from '../../../environments/environment';
import { NotificacionService } from '../../core/notificacion.service';
import { Cirugia, Factura, Mascota, Usuario } from '../../core/modelos';

/**
 * Pruebas de la pantalla de cirugias y postoperatorios.
 *
 * Se comprueba la carga de los endpoints del modulo (cirugias, facturacion,
 * mascotas y veterinarios), los contadores por estado, el filtrado del
 * listado, el formateo de la fecha y del precio mostrados y el envio del
 * alta, la edicion y la eliminacion de una cirugia.
 */
describe('CirugiasComponent', () => {
  let component: CirugiasComponent;
  let fixture: ComponentFixture<CirugiasComponent>;
  let httpMock: HttpTestingController;
  let avisarError: jasmine.Spy;
  let avisarExito: jasmine.Spy;

  const api = environment.apiUrl;

  const veterinario = (over: Partial<Usuario> = {}): Usuario => ({
    id: 3,
    nombres: 'Luis',
    apellidos: 'Perez',
    email: 'luis.perez@vetsphere.test',
    telefono: '0999999999',
    roles: [{ id: 1, nombre: 'VETERINARIO' }],
    ...over
  });

  const mascota = (over: Partial<Mascota> = {}): Mascota => ({
    id: 10,
    nombre: 'Toby',
    especie: 'Perro',
    raza: 'Golden Retriever',
    sexo: 'Macho',
    color: 'Dorado',
    pesoKg: 12.5,
    fechaNacimiento: '2020-03-01',
    estado: 'Activo',
    propietario: null,
    historiales: null,
    cirugias: null,
    ...over
  });

  const cirugia = (over: Partial<Cirugia> = {}): Cirugia => ({
    id: 1,
    fechaProgramada: '2026-05-28T09:45:00',
    procedimiento: 'Castracion',
    tipoAnestesia: 'General inhalatoria',
    estadoPostoperatorio: 'Sin complicaciones',
    estado: 'Pendiente',
    constantesVitales: 'FC 90 / FR 24 / Temp 38.4',
    precio: 280,
    facturaId: 1,
    ...over
  });

  const factura = (over: Partial<Factura> = {}): Factura => ({
    id: 1,
    numero: 'FAC-2026-000001',
    cliente: 'Ana Molina',
    mascota: 'Toby',
    concepto: 'Cirugia',
    total: 280,
    estadoPago: 'Pagado',
    fecha: '2026-05-28T09:45:00',
    ...over
  });

  const CIRUGIAS: Cirugia[] = [
    cirugia({ mascota: mascota(), cirujano: veterinario() }),
    cirugia({
      id: 2, fechaProgramada: '2026-06-01T11:00:00', procedimiento: 'Cesarrea',
      tipoAnestesia: 'Epidural', estadoPostoperatorio: 'En recuperacion', estado: 'Completada',
      constantesVitales: 'FC 80 / FR 20 / Temp 37.9', precio: 450, facturaId: 2,
      mascota: mascota({ id: 11, nombre: 'Luna' }), cirujanoPrincipal: 'Dra. Sara Ruiz'
    }),
    cirugia({
      id: 3, procedimiento: 'Drenaje', estadoPostoperatorio: 'Drenaje activo', estado: 'Postoperatorio',
      precio: 320, mascota: mascota({ id: 12, nombre: 'Rex' })
    }),
    cirugia({
      id: 4, procedimiento: 'Biopsia', tipoAnestesia: '', estadoPostoperatorio: '', notas: '',
      constantesVitales: '', estado: 'Cancelada', precio: 150, facturaId: undefined
    })
  ];

  const FACTURAS: Factura[] = [
    factura(),
    factura({
      id: 2, numero: 'FAC-2026-000002', cliente: 'Carlos Ruiz', mascota: 'Luna',
      concepto: 'Cesarrea', total: 450, estadoPago: 'Pendiente'
    })
  ];

  /** Responde a las cuatro peticiones que dispara ngOnInit. */
  const flushInicial = () => {
    httpMock.expectOne(req => req.url === `${api}/cirugias`).flush({ ok: true, data: CIRUGIAS, mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({ ok: true, data: FACTURAS, mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/mascotas`).flush({ ok: true, data: [mascota()], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/veterinarios/publico`).flush({ ok: true, data: [veterinario()], mensaje: '' });
    fixture.detectChanges();
  };

  /** Responde a la recarga que dispara cargar() tras guardar o tras pagar. */
  const flushRecarga = () => {
    httpMock.expectOne(req => req.url === `${api}/cirugias`).flush({ ok: true, data: CIRUGIAS, mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({ ok: true, data: FACTURAS, mensaje: '' });
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CirugiasComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    fixture = TestBed.createComponent(CirugiasComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    const notificaciones = TestBed.inject(NotificacionService);
    avisarError = spyOn(notificaciones, 'mostrarError');
    avisarExito = spyOn(notificaciones, 'mostrarExito');

    fixture.detectChanges();
    flushInicial();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  describe('Carga inicial', () => {
    it('carga cirugias, facturas, mascotas y veterinarios del backend', () => {
      expect(component.cirugias.length).toBe(4);
      expect(component.facturas.length).toBe(2);
      expect(component.mascotas.length).toBe(1);
      expect(component.veterinarios.length).toBe(1);
      expect(component.cargando).toBe(false);
    });

    it('avisa si no se pudieron cargar las cirugias', () => {
      component.cargar();
      httpMock.expectOne(req => req.url === `${api}/cirugias`).flush(
        { ok: false, data: null, mensaje: '' }, { status: 500, statusText: 'Error' });
      httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({ ok: true, data: [], mensaje: '' });

      expect(avisarError).toHaveBeenCalledWith('No se pudieron cargar las cirugías. Revisa que el backend esté activo.');
      expect(component.cargando).toBe(false);
    });
  });

  describe('Contadores y estados', () => {
    it('cuenta las cirugias por estado', () => {
      expect(component.contarEstado('Pendiente')).toBe(1);
      expect(component.contarEstado('Completada')).toBe(1);
      expect(component.contarEstado('Cancelada')).toBe(1);
      expect(component.contarEstado('Quirófano')).toBe(0);
    });

    it('cuenta aparte las cirugias en postoperatorio', () => {
      expect(component.postoperatorios).toBe(1);
    });

    it('asigna la insignia segun el estado de la cirugia', () => {
      expect(component.badgeEstado('Completada')).toBe('badge-ok');
      expect(component.badgeEstado('Cancelada')).toBe('badge-danger');
      expect(component.badgeEstado('Postoperatorio')).toBe('badge-info');
      expect(component.badgeEstado('Pendiente')).toBe('badge-warn');
      expect(component.badgeEstado(undefined)).toBe('badge-warn');
    });

    it('muestra el nombre del cirujano o Sin asignar si no hay ninguno', () => {
      expect(component.nombreUsuario(veterinario())).toBe('Luis Perez');
      expect(component.nombreUsuario(undefined)).toBe('Sin asignar');
    });
  });

  describe('Filtrado del listado', () => {
    it('filtra por procedimiento, estado, mascota o cirujano', () => {
      component.busqueda = 'castracion';
      expect(component.filtradas.length).toBe(1);
      expect(component.filtradas[0].id).toBe(1);

      component.busqueda = 'postoperatorio';
      expect(component.filtradas.length).toBe(1);
      expect(component.filtradas[0].id).toBe(3);

      component.busqueda = 'toby';
      expect(component.filtradas.length).toBe(1);

      component.busqueda = 'sara ruiz';
      expect(component.filtradas.length).toBe(1);
      expect(component.filtradas[0].id).toBe(2);

      component.busqueda = 'cirugia inexistente';
      expect(component.filtradas.length).toBe(0);
    });
  });

  describe('Factura vinculada', () => {
    it('devuelve Pendiente si la cirugia no tiene factura o la factura no existe', () => {
      expect(component.obtenerEstadoPago(undefined)).toBe('Pendiente');
      expect(component.obtenerEstadoPago(999)).toBe('Pendiente');
    });

    it('devuelve el estado de pago real de la factura vinculada', () => {
      expect(component.obtenerEstadoPago(1)).toBe('Pagado');
      expect(component.obtenerEstadoPago(2)).toBe('Pendiente');
    });

    it('no marca como pagada una cirugia sin factura asociada', () => {
      component.pagarFactura(undefined);
      httpMock.expectNone(req => req.url === `${api}/facturacion/1/estado`);
    });

    it('marca la factura como Pagado y recarga el listado', () => {
      component.pagarFactura(2);

      const peticion = httpMock.expectOne(req => req.url === `${api}/facturacion/2/estado` && req.method === 'PUT');
      expect(peticion.request.body.estadoPago).toBe('Pagado');
      peticion.flush({ ok: true, data: null, mensaje: '' });

      flushRecarga();
      expect(component.cargando).toBe(false);
    });
  });

  describe('Alta de cirugias', () => {
    it('prepara el formulario con la primera mascota y el primer cirujano', () => {
      component.nueva();

      expect(component.formularioAbierto).toBe(true);
      expect(component.editandoId).toBeUndefined();
      expect(component.form.mascotaId).toBe(10);
      expect(component.form.cirujanoId).toBe(3);
      expect(component.form.tipoAnestesia).toBe('General inhalatoria');
      expect(component.form.constantesVitales).toBe('FC 90 / FR 24 / Temp 38.4');
      expect(component.form.estado).toBe('Pendiente');
      expect(component.form.precio).toBe(280);
      expect(component.form.fechaProgramada).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
    });

    it('programa la cirugia con los datos del formulario', () => {
      component.nueva();
      component.form.procedimiento = 'Tumorectomia';
      component.form.estado = 'En quirofano';
      component.form.precio = 350;

      component.guardar();

      const peticion = httpMock.expectOne(req => req.url === `${api}/cirugias` && req.method === 'POST');
      const cuerpo = peticion.request.body;
      expect(cuerpo.procedimiento).toBe('Tumorectomia');
      expect(cuerpo.estado).toBe('En quirofano');
      expect(cuerpo.precio).toBe(350);
      expect(cuerpo.mascotaId).toBe(10);
      expect(cuerpo.cirujanoId).toBe(3);
      expect(cuerpo.tipoAnestesia).toBe('General inhalatoria');
      peticion.flush({ ok: true, data: null, mensaje: '' });

      expect(avisarExito).toHaveBeenCalledWith('Cirugía guardada correctamente.');
      expect(component.formularioAbierto).toBe(false);
      flushRecarga();
    });

    it('avisa si no se pudo guardar la cirugia', () => {
      component.nueva();
      component.guardar();

      httpMock.expectOne(req => req.url === `${api}/cirugias` && req.method === 'POST').flush(
        { ok: false, data: null, mensaje: 'Quirófano ocupado' }, { status: 409, statusText: 'Conflict' });

      expect(avisarError).toHaveBeenCalledWith('Quirófano ocupado');
      expect(component.formularioAbierto).toBe(true);
    });
  });

  describe('Edicion de cirugias', () => {
    it('precarga el formulario con los datos de la cirugia seleccionada', () => {
      component.editar(component.cirugias[1]);

      expect(component.formularioAbierto).toBe(true);
      expect(component.editandoId).toBe(2);
      expect(component.form.procedimiento).toBe('Cesarrea');
      expect(component.form.tipoAnestesia).toBe('Epidural');
      expect(component.form.estado).toBe('Completada');
      expect(component.form.precio).toBe(450);
      expect(component.form.mascotaId).toBe(11);
      expect(component.form.fechaProgramada).toBe('2026-06-01T11:00');
    });

    it('usa el nombre del-cirujano cuando la cirugia no lo tiene informado', () => {
      component.editar(component.cirugias[0]);
      expect(component.form.cirujanoPrincipal).toBe('Luis Perez');

      component.editar(component.cirugias[3]);
      expect(component.form.cirujanoPrincipal).toBe('Sin asignar');
    });

    it('actualiza la cirugia con PUT y no crea una nueva', () => {
      component.editar(component.cirugias[0]);
      component.form.estado = 'Completada';
      component.guardar();

      httpMock.expectNone(req => req.url === `${api}/cirugias` && req.method === 'POST');
      const peticion = httpMock.expectOne(req => req.url === `${api}/cirugias/1` && req.method === 'PUT');
      expect(peticion.request.body.estado).toBe('Completada');
      expect(peticion.request.body.procedimiento).toBe('Castracion');
      expect(peticion.request.body.precio).toBe(280);
      peticion.flush({ ok: true, data: null, mensaje: '' });

      expect(avisarExito).toHaveBeenCalledWith('Cirugía guardada correctamente.');
      flushRecarga();
    });
  });

  describe('Eliminacion de cirugias', () => {
    it('no envia nada si el usuario no confirma la eliminacion', () => {
      const confirmar = spyOn(window, 'confirm').and.returnValue(false);

      component.eliminar(component.cirugias[0]);

      expect(confirmar).toHaveBeenCalledWith('Eliminar cirugia Castracion?');
      httpMock.expectNone(req => req.url === `${api}/cirugias/1`);
      expect(component.cirugias.length).toBe(4);
    });

    it('elimina la cirugia y la quita del listado cuando se confirma', () => {
      spyOn(window, 'confirm').and.returnValue(true);

      component.eliminar(component.cirugias[0]);

      const peticion = httpMock.expectOne(req => req.url === `${api}/cirugias/1` && req.method === 'DELETE');
      peticion.flush({ ok: true, data: null, mensaje: '' });

      expect(component.cirugias.length).toBe(3);
      expect(component.cirugias.find(c => c.id === 1)).toBeUndefined();
    });
  });

  describe('Lo que se muestra en pantalla', () => {
    it('muestra el precio con dos decimales y el estado de facturacion', () => {
      const texto = fixture.nativeElement.textContent as string;

      expect(texto).toContain('Cirugias y postoperatorios');
      expect(texto).toContain('$280.00');
      expect(texto).toContain('$450.00');
      // Algunas cirugias no tienen facturaId, otras si; ambos casos se muestran en el listado
      expect(texto).toContain('Sin Facturar');
      expect(texto).toContain('Pagado');
      expect(texto).toContain('Pendiente');
    });

    it('sustituye los datos que faltan por textos por defecto', () => {
      const texto = fixture.nativeElement.textContent as string;

      expect(texto).toContain('Paciente sin asignar');
      expect(texto).toContain('Anestesia pendiente');
      expect(texto).toContain('Sin registro');
      expect(texto).toContain('Sin notas postoperatorias.');
    });

    it('muestra un contador por cada estado de cirugia', () => {
      const valores = Array.from(fixture.nativeElement.querySelectorAll('.stat-value') as NodeListOf<HTMLElement>)
        .map(el => el.textContent?.trim());

      expect(valores).toEqual(['4', '1', '1', '1']);
    });
  });
});
