import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ConsultasComponent } from './consultas';
import { environment } from '../../../environments/environment';
import { NotificacionService } from '../../core/notificacion.service';
import { ConsultaMedica, Factura, Mascota, Usuario } from '../../core/modelos';

/**
 * Pruebas de la pantalla de consultas medicas.
 *
 * Se comprueba el comportamiento real del componente: la carga de los cuatro
 * endpoints del modulo (consultas, facturacion, mascotas y veterinarios), los
 * contadores por tipo, el filtrado del listado, el estado de pago de la
 * factura vinculada y el envio del alta y de la edicion.
 */
describe('ConsultasComponent', () => {
  let component: ConsultasComponent;
  let fixture: ComponentFixture<ConsultasComponent>;
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

  const consulta = (over: Partial<ConsultaMedica> = {}): ConsultaMedica => ({
    id: 1,
    mascota: 'Toby',
    cliente: 'Ana Molina',
    veterinario: 'Luis Perez',
    tipo: 'Rutina',
    fecha: '2026-05-28T09:45:00',
    diagnostico: 'Dermatitis leve',
    tratamiento: 'Antiparasitario trimestral',
    pesoKg: 12.5,
    temperatura: 38.4,
    precio: 45,
    facturaId: 1,
    ...over
  });

  const factura = (over: Partial<Factura> = {}): Factura => ({
    id: 1,
    numero: 'FAC-2026-000001',
    cliente: 'Ana Molina',
    mascota: 'Toby',
    concepto: 'Consulta medica',
    total: 45,
    estadoPago: 'Pagado',
    fecha: '2026-05-28T09:45:00',
    ...over
  });

  const CONSULTAS: ConsultaMedica[] = [
    consulta(),
    consulta({
      id: 2, mascota: 'Luna', cliente: 'Carlos Ruiz', tipo: 'Emergencia',
      diagnostico: 'Intoxicacion', tratamiento: 'Lavado de estomago', precio: 120, facturaId: 2
    }),
    consulta({
      id: 3, mascota: 'Rex', cliente: 'Marta Diaz', tipo: 'Vacunacion',
      diagnostico: 'Vacuna anual', precio: 30, facturaId: undefined
    }),
    consulta({
      id: 4, mascota: 'Firulais', cliente: 'Ana Molina', tipo: 'Cirugia',
      diagnostico: 'Castracion', precio: 300
    })
  ];

  const FACTURAS: Factura[] = [
    factura(),
    factura({
      id: 2, numero: 'FAC-2026-000002', cliente: 'Carlos Ruiz', mascota: 'Luna',
      concepto: 'Emergencia', total: 120, estadoPago: 'Pendiente'
    })
  ];

  /** Responde a las cuatro peticiones que dispara ngOnInit. */
  const flushInicial = () => {
    httpMock.expectOne(req => req.url === `${api}/consultas`).flush({ ok: true, data: CONSULTAS, mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({ ok: true, data: FACTURAS, mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/mascotas`).flush({ ok: true, data: [mascota()], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/veterinarios/publico`).flush({ ok: true, data: [veterinario()], mensaje: '' });
    fixture.detectChanges();
  };

  /** Responde a la recarga que dispara cargar() tras guardar o tras pagar. */
  const flushRecarga = () => {
    httpMock.expectOne(req => req.url === `${api}/consultas`).flush({ ok: true, data: CONSULTAS, mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({ ok: true, data: FACTURAS, mensaje: '' });
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConsultasComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    fixture = TestBed.createComponent(ConsultasComponent);
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
    it('carga consultas, facturas, mascotas y veterinarios del backend', () => {
      expect(component.consultas.length).toBe(4);
      expect(component.facturas.length).toBe(2);
      expect(component.mascotas.length).toBe(1);
      expect(component.veterinarios.length).toBe(1);
      expect(component.cargando).toBe(false);
    });

    it('avisa con el mensaje por defecto si el backend no responde', () => {
      component.cargar();
      httpMock.expectOne(req => req.url === `${api}/consultas`).flush(
        null, { status: 500, statusText: 'Error' });
      httpMock.expectOne(req => req.url === `${api}/facturacion`).flush(
        null, { status: 500, statusText: 'Error' });

      expect(avisarError).toHaveBeenCalledWith('No se pudieron cargar las consultas. Revisa que el backend esté activo.');
      expect(component.cargando).toBe(false);
      expect(component.consultas.length).toBe(4);
    });

    it('muestra el mensaje que devuelve el servidor cuando la carga falla', () => {
      component.cargar();
      httpMock.expectOne(req => req.url === `${api}/consultas`).flush(
        { ok: false, data: null, mensaje: 'Base de datos no disponible' }, { status: 503, statusText: 'Unavailable' });
      httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({ ok: true, data: [], mensaje: '' });

      expect(avisarError).toHaveBeenCalledWith('Base de datos no disponible');
      expect(component.cargando).toBe(false);
    });
  });

  describe('Contadores y filtrado', () => {
    it('cuenta las consultas por tipo sin distinguir mayusculas', () => {
      expect(component.contar('Rutina')).toBe(1);
      expect(component.contar('Emergencia')).toBe(1);
      expect(component.contar('Vacunacion')).toBe(1);
      expect(component.contar('Cirugia')).toBe(1);
      expect(component.contar('Control')).toBe(0);
      expect(component.contar('rutina')).toBe(1);
    });

    it('asigna la insignia segun el tipo de consulta', () => {
      expect(component.badgeTipo('Emergencia')).toBe('badge-danger');
      expect(component.badgeTipo('Cirugia')).toBe('badge-warn');
      expect(component.badgeTipo('Vacunacion')).toBe('badge-ok');
      expect(component.badgeTipo('Rutina')).toBe('badge-info');
      expect(component.badgeTipo('')).toBe('badge-info');
    });

    it('filtra por mascota, cliente, veterinario o diagnostico', () => {
      component.busqueda = 'toby';
      expect(component.filtradas.length).toBe(1);
      expect(component.filtradas[0].id).toBe(1);

      component.busqueda = 'ana';
      expect(component.filtradas.length).toBe(2);

      component.busqueda = 'dermatitis';
      expect(component.filtradas.length).toBe(1);

      component.busqueda = 'inexistente';
      expect(component.filtradas.length).toBe(0);
    });
  });

  describe('Factura vinculada', () => {
    it('devuelve Pendiente si la consulta no tiene factura o la factura no existe', () => {
      expect(component.obtenerEstadoPago(undefined)).toBe('Pendiente');
      expect(component.obtenerEstadoPago(999)).toBe('Pendiente');
    });

    it('devuelve el estado de pago real de la factura vinculada', () => {
      expect(component.obtenerEstadoPago(1)).toBe('Pagado');
      expect(component.obtenerEstadoPago(2)).toBe('Pendiente');
    });

    it('no marca como pagada una consulta sin factura asociada', () => {
      component.pagarFactura(undefined);
      httpMock.expectNone(req => req.url === `${api}/facturacion/1/estado`);
    });

    it('marca la factura como Pagado y recarga el listado', () => {
      component.pagarFactura(2);

      const peticion = httpMock.expectOne(req => req.url === `${api}/facturacion/2/estado` && req.method === 'PUT');
      expect(peticion.request.body.estadoPago).toBe('Pagado');
      expect(peticion.request.body.metodoPago).toBeUndefined();
      peticion.flush({ ok: true, data: null, mensaje: '' });

      flushRecarga();
      expect(component.cargando).toBe(false);
    });

    it('avisa si no se pudo marcar la factura como pagada', () => {
      component.pagarFactura(2);
      httpMock.expectOne(req => req.url === `${api}/facturacion/2/estado`).flush(
        { ok: false, data: null, mensaje: 'Factura bloqueada' }, { status: 409, statusText: 'Conflict' });

      expect(avisarError).toHaveBeenCalledWith('Factura bloqueada');
    });
  });

  describe('Alta de consultas', () => {
    it('prepara el formulario con la primera mascota y el primer veterinario', () => {
      component.nuevaConsulta();

      expect(component.formularioAbierto).toBe(true);
      expect(component.editandoId).toBeUndefined();
      expect(component.form.tipo).toBe('Rutina');
      expect(component.form.motivo).toBe('');
      expect(component.form.temperatura).toBe(38);
      expect(component.form.frecuenciaCardiaca).toBe(90);
      expect(component.form.frecuenciaRespiratoria).toBe(24);
      expect(component.form.mascotaId).toBe(10);
      expect(component.form.veterinarioId).toBe(3);
    });

    it('registra la consulta con la fecha actual, el tipo y los signos vitales', () => {
      component.nuevaConsulta();
      component.form.tipo = 'Control';
      component.form.motivo = 'Control anual';
      component.form.diagnostico = 'Alergia estacional';
      component.form.tratamiento = 'Antihistaminico';
      component.form.temperatura = 39.1;
      component.form.frecuenciaCardiaca = 112;
      component.form.frecuenciaRespiratoria = 30;

      component.guardar();

      const peticion = httpMock.expectOne(req => req.url === `${api}/consultas` && req.method === 'POST');
      const cuerpo = peticion.request.body;
      expect(cuerpo.tipo).toBe('Control');
      expect(cuerpo.tipoEvento).toBe('Control');
      expect(cuerpo.motivo).toBe('Control anual');
      expect(cuerpo.diagnostico).toBe('Alergia estacional');
      expect(cuerpo.tratamiento).toBe('Antihistaminico');
      expect(cuerpo.temperatura).toBe(39.1);
      expect(cuerpo.frecuenciaCardiaca).toBe(112);
      expect(cuerpo.frecuenciaRespiratoria).toBe(30);
      expect(cuerpo.mascota).toEqual({ id: 10 });
      expect(cuerpo.veterinario).toEqual({ id: 3 });
      expect(cuerpo.fecha).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/);

      peticion.flush({ ok: true, data: null, mensaje: '' });

      expect(component.mensaje).toBe('Consulta registrada correctamente.');
      expect(avisarExito).toHaveBeenCalledWith('Consulta registrada correctamente.');
      flushRecarga();
    });

    it('avisa si el backend no pudo registrar la consulta', () => {
      component.nuevaConsulta();
      component.guardar();

      httpMock.expectOne(req => req.url === `${api}/consultas` && req.method === 'POST').flush(
        { ok: false, data: null, mensaje: 'Mascota no encontrada' }, { status: 400, statusText: 'Bad Request' });

      expect(avisarError).toHaveBeenCalledWith('Mascota no encontrada');
      expect(component.mensaje).toBe('');
    });
  });

  describe('Edicion de consultas', () => {
    it('precarga el formulario con los datos de la consulta seleccionada', () => {
      component.editar(component.consultas[0]);

      expect(component.formularioAbierto).toBe(true);
      expect(component.editandoId).toBe(1);
      expect(component.form.tipo).toBe('Rutina');
      expect(component.form.diagnostico).toBe('Dermatitis leve');
      expect(component.form.tratamiento).toBe('Antiparasitario trimestral');
      expect(component.form.temperatura).toBe(38.4);
      expect(component.form.frecuenciaCardiaca).toBe(0);
      expect(component.form.frecuenciaRespiratoria).toBe(0);
    });

    it('actualiza la consulta con PUT y no crea una nueva', () => {
      component.editar(component.consultas[0]);
      component.form.diagnostico = 'Dermatitis tratada';
      component.guardar();

      httpMock.expectNone(req => req.url === `${api}/consultas` && req.method === 'POST');
      const peticion = httpMock.expectOne(req => req.url === `${api}/consultas/1` && req.method === 'PUT');
      expect(peticion.request.body.diagnostico).toBe('Dermatitis tratada');
      expect(peticion.request.body.tipo).toBe('Rutina');
      peticion.flush({ ok: true, data: null, mensaje: '' });

      expect(component.mensaje).toBe('Consulta actualizada correctamente.');
      expect(avisarExito).toHaveBeenCalledWith('Consulta actualizada correctamente.');
      flushRecarga();
    });
  });

  describe('Eliminacion de consultas', () => {
    it('no envia nada si el usuario no confirma la eliminacion', () => {
      const confirmar = spyOn(window, 'confirm').and.returnValue(false);

      component.eliminar(component.consultas[0]);

      expect(confirmar).toHaveBeenCalledWith('Eliminar consulta de Toby?');
      httpMock.expectNone(req => req.url === `${api}/consultas/1`);
      expect(component.consultas.length).toBe(4);
    });

    it('elimina la consulta y la quita del listado cuando se confirma', () => {
      spyOn(window, 'confirm').and.returnValue(true);

      component.eliminar(component.consultas[1]);

      const peticion = httpMock.expectOne(req => req.url === `${api}/consultas/2` && req.method === 'DELETE');
      peticion.flush({ ok: true, data: null, mensaje: '' });

      expect(component.consultas.length).toBe(3);
      expect(component.consultas.find(c => c.id === 2)).toBeUndefined();
    });

    it('avisa si no se pudo eliminar la consulta', () => {
      spyOn(window, 'confirm').and.returnValue(true);

      component.eliminar(component.consultas[1]);
      httpMock.expectOne(req => req.url === `${api}/consultas/2` && req.method === 'DELETE').flush(
        { ok: false, data: null, mensaje: 'Consulta con historial' }, { status: 409, statusText: 'Conflict' });

      expect(avisarError).toHaveBeenCalledWith('Consulta con historial');
      expect(component.consultas.length).toBe(4);
    });
  });

  describe('Lo que se muestra en pantalla', () => {
    it('pinta los datos de cada consulta y su estado de facturacion', () => {
      const texto = fixture.nativeElement.textContent as string;

      expect(texto).toContain('Consultas medicas');
      expect(texto).toContain('Toby - Ana Molina');
      expect(texto).toContain('12.5 kg - 38.4 C');
      expect(texto).toContain('$45');
      expect(texto).toContain('Sin Facturar');
      expect(texto).toContain('Pagado');
      expect(texto).toContain('Pendiente');
    });

    it('muestra un contador por cada tipo de consulta', () => {
      const valores = Array.from(fixture.nativeElement.querySelectorAll('.stat-value') as NodeListOf<HTMLElement>)
        .map(el => el.textContent?.trim());

      expect(valores).toEqual(['1', '1', '0', '1', '1']);
    });

    it('cierra el formulario y borra el mensaje al cancelarlo', () => {
      component.nuevaConsulta();
      component.mensaje = 'Consulta registrada correctamente.';
      fixture.detectChanges();

      component.cerrarFormulario();

      expect(component.formularioAbierto).toBe(false);
      expect(component.mensaje).toBe('');
    });
  });
});
