import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { HospitalizacionComponent } from './hospitalizacion';
import { environment } from '../../../environments/environment';
import { NotificacionService } from '../../core/notificacion.service';
import { Factura, Hospitalizacion, Mascota, Usuario } from '../../core/modelos';

/**
 * Pruebas de la pantalla de hospitalizacion.
 *
 * Se comprueba la carga de los endpoints del modulo (hospitalizaciones,
 * facturacion, mascotas y veterinarios), los contadores por estado, el
 * filtrado del listado, el truncado de la fecha de alta, el precio mostrado
 * y el envio del alta, la edicion y la eliminacion de un ingreso.
 */
describe('HospitalizacionComponent', () => {
  let component: HospitalizacionComponent;
  let fixture: ComponentFixture<HospitalizacionComponent>;
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

  const hospitalizacion = (over: Partial<Hospitalizacion> = {}): Hospitalizacion => ({
    id: 1,
    ingreso: '2026-05-28T09:45:00',
    jaula: 'H-01',
    motivo: 'Postoperatorio de castracion',
    planCuidados: 'Antibiotico cada 12 horas',
    estado: 'Internado',
    precio: 45,
    facturaId: 1,
    ...over
  });

  const factura = (over: Partial<Factura> = {}): Factura => ({
    id: 1,
    numero: 'FAC-2026-000001',
    cliente: 'Ana Molina',
    mascota: 'Toby',
    concepto: 'Hospitalizacion',
    total: 45,
    estadoPago: 'Pagado',
    fecha: '2026-05-28T09:45:00',
    ...over
  });

  const HOSPITALIZACIONES: Hospitalizacion[] = [
    hospitalizacion({ mascota: mascota(), veterinarioResponsable: veterinario() }),
    hospitalizacion({
      id: 2, ingreso: '2026-05-30T08:00:00', alta: '2026-06-02T10:00:00', jaula: 'H-02',
      motivo: 'Gastroenteritis', planCuidados: 'Suero 250ml', estado: 'Alta', precio: 60, facturaId: 2,
      mascota: mascota({ id: 11, nombre: 'Luna' })
    }),
    hospitalizacion({
      id: 3, ingreso: '2026-06-01T12:00:00', jaula: 'H-03', motivo: 'Trauma por atropello',
      planCuidados: '', estado: 'Observacion', precio: 75,
      mascota: mascota({ id: 12, nombre: 'Rex' })
    }),
    hospitalizacion({
      id: 4, ingreso: '2026-06-03T09:00:00', jaula: 'H-04', motivo: 'Shock hipovolemico',
      planCuidados: 'Monitorizacion continua', estado: 'Critico', precio: 90, facturaId: undefined
    })
  ];

  const FACTURAS: Factura[] = [
    factura(),
    factura({
      id: 2, numero: 'FAC-2026-000002', cliente: 'Carlos Ruiz', mascota: 'Luna',
      concepto: 'Hospitalizacion', total: 60, estadoPago: 'Pendiente'
    })
  ];

  /** Responde a las peticiones auxiliares que dispara ngOnInit. */
  const flushInicial = () => {
    httpMock.expectOne(req => req.url === `${api}/hospitalizaciones`).flush({ ok: true, data: HOSPITALIZACIONES, mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({ ok: true, data: FACTURAS, mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/mascotas`).flush({ ok: true, data: [mascota()], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/veterinarios/publico`).flush({ ok: true, data: [veterinario()], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/farmacia/inventario`).flush({ ok: true, data: [], mensaje: '' });
    fixture.detectChanges();
  };

  /** Responde a la recarga que dispara cargar() tras guardar o tras pagar. */
  const flushRecarga = () => {
    httpMock.expectOne(req => req.url === `${api}/hospitalizaciones`).flush({ ok: true, data: HOSPITALIZACIONES, mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({ ok: true, data: FACTURAS, mensaje: '' });
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()],
      imports: [HospitalizacionComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(HospitalizacionComponent);
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
    it('carga hospitalizaciones, facturas, mascotas y veterinarios del backend', () => {
      expect(component.hospitalizaciones.length).toBe(4);
      expect(component.facturas.length).toBe(2);
      expect(component.mascotas.length).toBe(1);
      expect(component.veterinarios.length).toBe(1);
      expect(component.cargando).toBe(false);
    });

    it('avisa si no se pudieron cargar las hospitalizaciones', () => {
      component.cargar();
      httpMock.expectOne(req => req.url === `${api}/hospitalizaciones`).flush(
        null, { status: 500, statusText: 'Error' });
      httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({ ok: true, data: [], mensaje: '' });

      expect(avisarError).toHaveBeenCalledWith('No se pudieron cargar las hospitalizaciones. Revisa que el backend esté activo.');
      expect(component.cargando).toBe(false);
    });
  });

  describe('Contadores y estados', () => {
    it('cuenta los ingresos por estado', () => {
      expect(component.contar('Internado')).toBe(1);
      expect(component.contar('Observacion')).toBe(1);
      expect(component.contar('Alta')).toBe(1);
      expect(component.contar('Critico')).toBe(1);
    });

    it('asigna la insignia segun el estado del ingreso', () => {
      expect(component.badgeEstado('Alta')).toBe('badge-ok');
      expect(component.badgeEstado('Critico')).toBe('badge-danger');
      expect(component.badgeEstado('Observacion')).toBe('badge-info');
      expect(component.badgeEstado('Internado')).toBe('badge-warn');
      expect(component.badgeEstado(undefined)).toBe('badge-warn');
    });

    it('muestra el nombre del responsable o Sin responsable si no hay ninguno', () => {
      expect(component.nombreUsuario(veterinario())).toBe('Luis Perez');
      expect(component.nombreUsuario(undefined)).toBe('Sin responsable');
    });
  });

  describe('Filtrado del listado', () => {
    it('filtra por mascota, jaula, motivo o estado', () => {
      component.busqueda = 'toby';
      expect(component.filtradas.length).toBe(1);
      expect(component.filtradas[0].id).toBe(1);

      component.busqueda = 'h-03';
      expect(component.filtradas.length).toBe(1);
      expect(component.filtradas[0].id).toBe(3);

      component.busqueda = 'atropello';
      expect(component.filtradas.length).toBe(1);

      component.busqueda = 'critico';
      expect(component.filtradas.length).toBe(1);
      expect(component.filtradas[0].id).toBe(4);

      component.busqueda = 'ingreso inexistente';
      expect(component.filtradas.length).toBe(0);
    });
  });

  describe('Factura vinculada', () => {
    it('devuelve Pendiente si el ingreso no tiene factura o la factura no existe', () => {
      expect(component.obtenerEstadoPago(undefined)).toBe('Pendiente');
      expect(component.obtenerEstadoPago(999)).toBe('Pendiente');
    });

    it('devuelve el estado de pago real de la factura vinculada', () => {
      expect(component.obtenerEstadoPago(1)).toBe('Pagado');
      expect(component.obtenerEstadoPago(2)).toBe('Pendiente');
    });

    it('no marca como pagada una hospitalizacion sin factura asociada', () => {
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

  describe('Alta de ingresos', () => {
    it('prepara el formulario con la primera mascota y el primer veterinario', () => {
      component.nuevoIngreso();

      expect(component.formularioAbierto).toBe(true);
      expect(component.editandoId).toBeUndefined();
      expect(component.form.jaula).toBe('H-01');
      expect(component.form.estado).toBe('Internado');
      expect(component.form.precio).toBe(45);
      expect(component.form.motivo).toBe('');
      expect(component.form.planCuidados).toBe('');
      expect(component.form.alta).toBe('');
      expect(component.form.mascotaId).toBe(10);
      expect(component.form.veterinarioId).toBe(3);
    });

    it('registra el ingreso con la fecha actual y los ids de mascota y veterinario', () => {
      component.nuevoIngreso();
      component.form.jaula = 'H-07';
      component.form.motivo = 'Dificultad respiratoria';
      component.form.planCuidados = 'Oxigeno 2L';
      component.form.estado = 'Critico';
      component.form.precio = 80;

      component.guardar();

      const peticion = httpMock.expectOne(req => req.url === `${api}/hospitalizaciones` && req.method === 'POST');
      const cuerpo = peticion.request.body;
      expect(cuerpo.jaula).toBe('H-07');
      expect(cuerpo.motivo).toBe('Dificultad respiratoria');
      expect(cuerpo.planCuidados).toBe('Oxigeno 2L');
      expect(cuerpo.estado).toBe('Critico');
      expect(cuerpo.precio).toBe(80);
      expect(cuerpo.alta).toBeNull();
      expect(cuerpo.mascota).toEqual({ id: 10 });
      expect(cuerpo.veterinarioResponsable).toEqual({ id: 3 });
      expect(cuerpo.ingreso).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/);
      peticion.flush({ ok: true, data: null, mensaje: '' });

      expect(avisarExito).toHaveBeenCalledWith('Ingreso registrado correctamente.');
      expect(component.formularioAbierto).toBe(false);
      flushRecarga();
    });

    it('avisa si no se pudo registrar el ingreso', () => {
      component.nuevoIngreso();
      component.guardar();

      httpMock.expectOne(req => req.url === `${api}/hospitalizaciones` && req.method === 'POST').flush(
        { ok: false, data: null, mensaje: 'No hay jaulas libres' }, { status: 409, statusText: 'Conflict' });

      expect(avisarError).toHaveBeenCalledWith('No hay jaulas libres');
      expect(component.formularioAbierto).toBe(true);
    });
  });

  describe('Edicion de ingresos', () => {
    it('precarga el formulario con los datos del ingreso seleccionado', () => {
      component.editar(component.hospitalizaciones[1]);

      expect(component.formularioAbierto).toBe(true);
      expect(component.editandoId).toBe(2);
      expect(component.form.jaula).toBe('H-02');
      expect(component.form.motivo).toBe('Gastroenteritis');
      expect(component.form.planCuidados).toBe('Suero 250ml');
      expect(component.form.estado).toBe('Alta');
      expect(component.form.precio).toBe(60);
      expect(component.form.mascotaId).toBe(11);
    });

    it('trunca la fecha de alta a los minutos que acepta el formulario', () => {
      component.editar(component.hospitalizaciones[1]);
      expect(component.form.alta).toBe('2026-06-02T10:00');

      component.editar(component.hospitalizaciones[0]);
      expect(component.form.alta).toBe('');
    });

    it('actualiza el ingreso con PUT y no crea uno nuevo', () => {
      component.editar(component.hospitalizaciones[0]);
      component.form.planCuidados = 'Antibiotico cada 8 horas';
      component.guardar();

      httpMock.expectNone(req => req.url === `${api}/hospitalizaciones` && req.method === 'POST');
      const peticion = httpMock.expectOne(req => req.url === `${api}/hospitalizaciones/1` && req.method === 'PUT');
      expect(peticion.request.body.planCuidados).toBe('Antibiotico cada 8 horas');
      expect(peticion.request.body.jaula).toBe('H-01');
      expect(peticion.request.body.alta).toBeNull();
      peticion.flush({ ok: true, data: null, mensaje: '' });

      expect(avisarExito).toHaveBeenCalledWith('Hospitalización actualizada correctamente.');
      flushRecarga();
    });
  });

  describe('Eliminacion de ingresos', () => {
    it('no envia nada si el usuario no confirma la eliminacion', () => {
      const confirmar = spyOn(window, 'confirm').and.returnValue(false);

      component.eliminar(component.hospitalizaciones[0]);

      expect(confirmar).toHaveBeenCalledWith('Eliminar hospitalizacion de Toby?');
      httpMock.expectNone(req => req.url === `${api}/hospitalizaciones/1`);
      expect(component.hospitalizaciones.length).toBe(4);
    });

    it('elimina el ingreso y lo quita del listado cuando se confirma', () => {
      spyOn(window, 'confirm').and.returnValue(true);

      component.eliminar(component.hospitalizaciones[0]);

      const peticion = httpMock.expectOne(req => req.url === `${api}/hospitalizaciones/1` && req.method === 'DELETE');
      peticion.flush({ ok: true, data: null, mensaje: '' });

      expect(component.hospitalizaciones.length).toBe(3);
      expect(component.hospitalizaciones.find(h => h.id === 1)).toBeUndefined();
    });

    it('usa el texto por defecto cuando el ingreso no tiene mascota', () => {
      const confirmar = spyOn(window, 'confirm').and.returnValue(false);

      component.eliminar(component.hospitalizaciones[3]);

      expect(confirmar).toHaveBeenCalledWith('Eliminar hospitalizacion de paciente?');
    });
  });

  describe('Lo que se muestra en pantalla', () => {
    it('muestra el precio con dos decimales y el estado de facturacion', () => {
      const texto = fixture.nativeElement.textContent as string;

      expect(texto).toContain('Hospitalizacion');
      expect(texto).toContain('$ 45.00');
      expect(texto).toContain('$ 60.00');
      // Algunas hospitalizaciones no tienen facturaId, otras si; ambos casos se muestran en el listado
      expect(texto).toContain('Sin Facturar');
      expect(texto).toContain('Pagado');
      expect(texto).toContain('Pendiente');
    });

    it('sustituye los datos que faltan por textos por defecto', () => {
      const texto = fixture.nativeElement.textContent as string;

      expect(texto).toContain('Paciente sin asignar');
      expect(texto).toContain('Sin plan registrado.');
      expect(texto).toContain('Responsable: Luis Perez');
      expect(texto).toContain('Responsable: Sin responsable');
    });

    it('muestra un contador por cada estado de hospitalizacion', () => {
      const valores = Array.from(fixture.nativeElement.querySelectorAll('.stat-value') as NodeListOf<HTMLElement>)
        .map(el => el.textContent?.trim());

      expect(valores).toEqual(['4', '1', '1', '1']);
    });
  });
});
