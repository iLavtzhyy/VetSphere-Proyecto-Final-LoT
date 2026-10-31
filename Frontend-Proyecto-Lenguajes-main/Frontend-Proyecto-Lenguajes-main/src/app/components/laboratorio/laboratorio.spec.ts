import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { LaboratorioComponent } from './laboratorio';
import { environment } from '../../../environments/environment';
import { NotificacionService } from '../../core/notificacion.service';
import { Factura, Mascota, OrdenLaboratorio, Usuario } from '../../core/modelos';

/**
 * Pruebas de la pantalla de laboratorio.
 *
 * Se comprueba la carga de los endpoints del modulo (laboratorio, facturacion,
 * mascotas y veterinarios), los contadores por estado, el filtrado del listado,
 * el enlace al archivo de resultado y el envio del alta, la edicion y la
 * eliminacion de una orden.
 */
describe('LaboratorioComponent', () => {
  let component: LaboratorioComponent;
  let fixture: ComponentFixture<LaboratorioComponent>;
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

  const orden = (over: Partial<OrdenLaboratorio> = {}): OrdenLaboratorio => ({
    id: 1,
    fecha: '2026-05-28T09:45:00',
    tipoPrueba: 'Hemograma completo',
    resultado: 'Valores dentro de rango',
    estado: 'Pendiente',
    precio: 35,
    facturaId: 1,
    ...over
  });

  const factura = (over: Partial<Factura> = {}): Factura => ({
    id: 1,
    numero: 'FAC-2026-000001',
    cliente: 'Ana Molina',
    mascota: 'Toby',
    concepto: 'Laboratorio',
    total: 35,
    estadoPago: 'Pagado',
    fecha: '2026-05-28T09:45:00',
    ...over
  });

  const ORDENES: OrdenLaboratorio[] = [
    orden({ mascota: mascota(), veterinarioSolicitante: veterinario() }),
    orden({
      id: 2, fecha: '2026-05-29T10:00:00', tipoPrueba: 'Radiografia', resultado: 'Sin alteraciones',
      estado: 'Procesando', precio: 55, facturaId: undefined,
      archivoResultadoUrl: 'http://localhost:8080/api/archivos/orden-2.pdf',
      mascota: mascota({ id: 11, nombre: 'Luna' })
    }),
    orden({
      id: 3, tipoPrueba: 'Cultivo', resultado: 'Negativo', estado: 'Entregado', precio: 40, facturaId: 2,
      mascota: mascota({ id: 12, nombre: 'Rex' })
    }),
    orden({
      id: 4, tipoPrueba: 'Perfil renal', resultado: '', estado: 'Repetir muestra', precio: 25,
      mascota: mascota({ id: 13, nombre: 'Firulais' })
    })
  ];

  const FACTURAS: Factura[] = [
    factura(),
    factura({
      id: 2, numero: 'FAC-2026-000002', cliente: 'Carlos Ruiz', mascota: 'Rex',
      concepto: 'Cultivo', total: 40, estadoPago: 'Pendiente'
    })
  ];

  /** Responde a las cuatro peticiones que dispara ngOnInit. */
  const flushInicial = () => {
    httpMock.expectOne(req => req.url === `${api}/laboratorio`).flush({ ok: true, data: ORDENES, mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({ ok: true, data: FACTURAS, mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/mascotas`).flush({ ok: true, data: [mascota()], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/veterinarios/publico`).flush({ ok: true, data: [veterinario()], mensaje: '' });
    fixture.detectChanges();
  };

  /** Responde a la recarga que dispara cargar() tras guardar o tras pagar. */
  const flushRecarga = () => {
    httpMock.expectOne(req => req.url === `${api}/laboratorio`).flush({ ok: true, data: ORDENES, mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({ ok: true, data: FACTURAS, mensaje: '' });
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LaboratorioComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    fixture = TestBed.createComponent(LaboratorioComponent);
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
    it('carga ordenes, facturas, mascotas y veterinarios del backend', () => {
      expect(component.ordenes.length).toBe(4);
      expect(component.facturas.length).toBe(2);
      expect(component.mascotas.length).toBe(1);
      expect(component.veterinarios.length).toBe(1);
      expect(component.cargando).toBe(false);
    });

    it('avisa si no se pudieron cargar las ordenes de laboratorio', () => {
      component.cargar();
      httpMock.expectOne(req => req.url === `${api}/laboratorio`).flush(
        { ok: false, data: null, mensaje: '' }, { status: 500, statusText: 'Error' });
      httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({ ok: true, data: [], mensaje: '' });

      expect(avisarError).toHaveBeenCalledWith('No se pudieron cargar las órdenes de laboratorio. Revisa que el backend esté activo.');
      expect(component.cargando).toBe(false);
    });
  });

  describe('Contadores y estados', () => {
    it('cuenta las ordenes por estado', () => {
      expect(component.contar('Pendiente')).toBe(1);
      expect(component.contar('Procesando')).toBe(1);
      expect(component.contar('Entregado')).toBe(1);
      expect(component.contar('Repetir muestra')).toBe(1);
    });

    it('asigna la insignia segun el estado de la orden', () => {
      expect(component.badgeEstado('Entregado')).toBe('badge-ok');
      expect(component.badgeEstado('Repetir muestra')).toBe('badge-danger');
      expect(component.badgeEstado('Procesando')).toBe('badge-info');
      expect(component.badgeEstado('Pendiente')).toBe('badge-warn');
      expect(component.badgeEstado(undefined)).toBe('badge-warn');
    });

    it('muestra el nombre del veterinario o Sin veterinario si no hay ninguno', () => {
      expect(component.nombreUsuario(veterinario())).toBe('Luis Perez');
      expect(component.nombreUsuario(undefined)).toBe('Sin veterinario');
    });
  });

  describe('Filtrado del listado', () => {
    it('filtra por prueba, estado, resultado o mascota', () => {
      component.busqueda = 'hemograma';
      expect(component.filtradas.length).toBe(1);
      expect(component.filtradas[0].id).toBe(1);

      component.busqueda = 'procesando';
      expect(component.filtradas.length).toBe(1);
      expect(component.filtradas[0].id).toBe(2);

      component.busqueda = 'negativo';
      expect(component.filtradas.length).toBe(1);
      expect(component.filtradas[0].id).toBe(3);

      component.busqueda = 'firulais';
      expect(component.filtradas.length).toBe(1);

      component.busqueda = 'prueba inexistente';
      expect(component.filtradas.length).toBe(0);
    });
  });

  describe('Factura vinculada', () => {
    it('devuelve Pendiente si la orden no tiene factura o la factura no existe', () => {
      expect(component.obtenerEstadoPago(undefined)).toBe('Pendiente');
      expect(component.obtenerEstadoPago(999)).toBe('Pendiente');
    });

    it('devuelve el estado de pago real de la factura vinculada', () => {
      expect(component.obtenerEstadoPago(1)).toBe('Pagado');
      expect(component.obtenerEstadoPago(2)).toBe('Pendiente');
    });

    it('no marca como pagada una orden sin factura asociada', () => {
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

  describe('Alta de ordenes', () => {
    it('prepara el formulario con la primera mascota y el primer veterinario', () => {
      component.nueva();

      expect(component.formularioAbierto).toBe(true);
      expect(component.editandoId).toBeUndefined();
      expect(component.form.tipoPrueba).toBe('Hemograma completo');
      expect(component.form.estado).toBe('Pendiente');
      expect(component.form.resultado).toBe('');
      expect(component.form.archivoResultadoUrl).toBe('');
      expect(component.form.precio).toBe(35);
      expect(component.form.mascotaId).toBe(10);
      expect(component.form.veterinarioId).toBe(3);
    });

    it('crea la orden con la fecha actual y los ids de mascota y veterinario', () => {
      component.nueva();
      component.form.tipoPrueba = 'Hemograma y bioquimica';
      component.form.resultado = 'Hematocrito 45%';
      component.form.estado = 'Procesando';

      component.guardar();

      const peticion = httpMock.expectOne(req => req.url === `${api}/laboratorio` && req.method === 'POST');
      const cuerpo = peticion.request.body;
      expect(cuerpo.tipoPrueba).toBe('Hemograma y bioquimica');
      expect(cuerpo.resultado).toBe('Hematocrito 45%');
      expect(cuerpo.estado).toBe('Procesando');
      expect(cuerpo.precio).toBe(35);
      expect(cuerpo.mascota).toEqual({ id: 10 });
      expect(cuerpo.veterinarioSolicitante).toEqual({ id: 3 });
      expect(cuerpo.fecha).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/);
      peticion.flush({ ok: true, data: null, mensaje: '' });

      expect(avisarExito).toHaveBeenCalledWith('Orden creada correctamente.');
      expect(component.formularioAbierto).toBe(false);
      flushRecarga();
    });

    it('avisa si no se pudo crear la orden', () => {
      component.nueva();
      component.guardar();

      httpMock.expectOne(req => req.url === `${api}/laboratorio` && req.method === 'POST').flush(
        { ok: false, data: null, mensaje: 'Muestra no recibida' }, { status: 400, statusText: 'Bad Request' });

      expect(avisarError).toHaveBeenCalledWith('Muestra no recibida');
      expect(component.formularioAbierto).toBe(true);
    });
  });

  describe('Edicion de ordenes', () => {
    it('precarga el formulario con los datos de la orden seleccionada', () => {
      component.editar(component.ordenes[2]);

      expect(component.formularioAbierto).toBe(true);
      expect(component.editandoId).toBe(3);
      expect(component.form.tipoPrueba).toBe('Cultivo');
      expect(component.form.resultado).toBe('Negativo');
      expect(component.form.estado).toBe('Entregado');
      expect(component.form.precio).toBe(40);
      expect(component.form.mascotaId).toBe(12);
    });

    it('actualiza la orden con PUT y no crea una nueva', () => {
      component.editar(component.ordenes[0]);
      component.form.resultado = 'Leucocitosis leve';
      component.guardar();

      httpMock.expectNone(req => req.url === `${api}/laboratorio` && req.method === 'POST');
      const peticion = httpMock.expectOne(req => req.url === `${api}/laboratorio/1` && req.method === 'PUT');
      expect(peticion.request.body.resultado).toBe('Leucocitosis leve');
      expect(peticion.request.body.tipoPrueba).toBe('Hemograma completo');
      peticion.flush({ ok: true, data: null, mensaje: '' });

      expect(avisarExito).toHaveBeenCalledWith('Orden actualizada correctamente.');
      flushRecarga();
    });
  });

  describe('Eliminacion de ordenes', () => {
    it('no envia nada si el usuario no confirma la eliminacion', () => {
      const confirmar = spyOn(window, 'confirm').and.returnValue(false);

      component.eliminar(component.ordenes[0]);

      expect(confirmar).toHaveBeenCalledWith('Eliminar orden Hemograma completo?');
      httpMock.expectNone(req => req.url === `${api}/laboratorio/1`);
      expect(component.ordenes.length).toBe(4);
    });

    it('elimina la orden y la quita del listado cuando se confirma', () => {
      spyOn(window, 'confirm').and.returnValue(true);

      component.eliminar(component.ordenes[0]);

      const peticion = httpMock.expectOne(req => req.url === `${api}/laboratorio/1` && req.method === 'DELETE');
      peticion.flush({ ok: true, data: null, mensaje: '' });

      expect(component.ordenes.length).toBe(3);
      expect(component.ordenes.find(o => o.id === 1)).toBeUndefined();
    });
  });

  describe('Lo que se muestra en pantalla', () => {
    it('muestra el precio con dos decimales y el estado de facturacion', () => {
      const texto = fixture.nativeElement.textContent as string;

      expect(texto).toContain('Laboratorio');
      expect(texto).toContain('$35.00');
      expect(texto).toContain('$55.00');
      expect(texto).toContain('Sin Facturar');
      expect(texto).toContain('Pagado');
      expect(texto).toContain('Pendiente');
    });

    it('sustituye el resultado que falta por un texto por defecto', () => {
      const texto = fixture.nativeElement.textContent as string;

      expect(texto).toContain('Resultado pendiente.');
    });

    it('muestra el enlace al archivo solo en las ordenes que lo tienen', () => {
      const enlaces = fixture.nativeElement.querySelectorAll('a.action-link') as NodeListOf<HTMLAnchorElement>;

      expect(enlaces.length).toBe(1);
      expect(enlaces[0].getAttribute('href')).toBe('http://localhost:8080/api/archivos/orden-2.pdf');
      expect(enlaces[0].textContent?.trim()).toBe('Abrir archivo');
    });

    it('muestra un contador por cada estado de las ordenes', () => {
      const valores = Array.from(fixture.nativeElement.querySelectorAll('.stat-value') as NodeListOf<HTMLElement>)
        .map(el => el.textContent?.trim());

      expect(valores).toEqual(['4', '1', '1', '1']);
    });
  });
});
