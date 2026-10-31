import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { FacturacionComponent } from './facturacion';
import { environment } from '../../../environments/environment';
import { AutorizacionService } from '../../core/autorizacion.service';
import { NotificacionService } from '../../core/notificacion.service';
import { Factura, Producto } from '../../core/modelos';

/**
 * Pruebas de la pantalla de facturacion.
 *
 * El punto clave es que TODOS los importes (subtotal, IVA, descuento y total)
 * son los que devuelve el servidor: el frontend no recalcula nada y no inventa
 * la numeracion de la factura.
 */
describe('FacturacionComponent', () => {
  let component: FacturacionComponent;
  let fixture: ComponentFixture<FacturacionComponent>;
  let httpMock: HttpTestingController;
  let notificaciones: NotificacionService;
  let avisarError: jasmine.Spy;

  const api = environment.apiUrl;

  const facturaServidor = (over: Partial<Factura> = {}): Factura => ({
    id: 7,
    numero: 'FAC-2026-000007',
    cliente: 'Ana Molina',
    mascota: 'Toby',
    concepto: 'Otitis y farmacia',
    subtotal: 72,
    impuestos: 8.64,
    descuento: 0,
    cuponCodigo: undefined,
    total: 80.64,
    estadoPago: 'Pendiente',
    fecha: '2026-05-28T09:45:00',
    ...over
  });

  /** Peticiones que dispara `abrirNuevaFactura()` para cargar los datos auxiliares. */
  const flushDatosAuxiliares = () => {
    httpMock.expectOne(req => req.url === `${api}/clientes`).flush({ ok: true, data: [], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/mascotas`).flush({ ok: true, data: [], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/consultas`).flush({ ok: true, data: [], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/cirugias`).flush({ ok: true, data: [], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/laboratorio`).flush({ ok: true, data: [], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/hospitalizaciones`).flush({ ok: true, data: [], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/farmacia/inventario`).flush({ ok: true, data: [], mensaje: '' });
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FacturacionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    fixture = TestBed.createComponent(FacturacionComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    notificaciones = TestBed.inject(NotificacionService);
    avisarError = spyOn(notificaciones, 'mostrarError');
    fixture.detectChanges();

    httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({
      ok: true,
      data: [facturaServidor()],
      mensaje: ''
    });
    httpMock.expectOne(req => req.url === `${api}/facturacion/datos-pago`).flush({
      ok: true,
      data: { configurado: false },
      mensaje: ''
    });
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  describe('Listado', () => {
    it('muestra el IVA que devuelve el servidor (nunca $0.00)', () => {
      expect(component.facturas.length).toBe(1);
      expect(component.impuestosDe(component.facturas[0])).toBe(8.64);
      expect(component.subtotalDe(component.facturas[0])).toBe(72);
      expect(component.totalImpuestos).toBe(8.64);
      expect(component.totalSubtotal).toBe(72);
      expect(component.total).toBe(80.64);
    });

    it('cuenta las facturas pagadas y pendientes', () => {
      expect(component.pendientes).toBe(1);
      expect(component.pagadas).toBe(0);
    });

    it('filtra comprobantes por texto, estado y rango de fechas, y limpia los filtros', () => {
      component.facturas = [
        facturaServidor(),
        facturaServidor({ id: 8, numero: 'FAC-2026-000008', cliente: 'Luis Reyes', mascota: 'Milo', estadoPago: 'Pagado', fecha: '2026-06-02T10:00:00' })
      ];
      component.busquedaFactura = 'milo';
      component.estadoFiltro = 'Pagado';
      component.fechaDesde = '2026-06-01';
      expect(component.facturasFiltradas.map(f => f.id)).toEqual([8]);

      component.limpiarFiltrosFacturas();
      expect(component.facturasFiltradas.length).toBe(2);
      expect(component.busquedaFactura).toBe('');
    });

    it('ordena los comprobantes más recientes primero', () => {
      (component as any).cargarFacturas();
      httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({
        ok: true,
        data: [
          facturaServidor({ id: 7, fecha: '2026-05-28T09:45:00' }),
          facturaServidor({ id: 8, fecha: '2026-06-02T10:00:00' })
        ],
        mensaje: ''
      });
      expect(component.facturasFiltradas.map(f => f.id)).toEqual([8, 7]);
    });

    it('traduce el estado de pago a una clase de insignia', () => {
      expect(component.estadoClase(facturaServidor({ estadoPago: 'Pagado' }))).toBe('badge-ok');
      expect(component.estadoClase(facturaServidor({ estadoPago: 'Anulado' }))).toBe('badge-danger');
      expect(component.estadoClase(facturaServidor({ estadoPago: 'Pendiente' }))).toBe('badge-warn');
    });

    it('no inventa la numeracion de la factura', () => {
      expect(component.numeroPendiente).toBe('Se asigna al emitir (servidor)');
    });
  });

  describe('Emision', () => {
    beforeEach(() => {
      component.abrirNuevaFactura();
      flushDatosAuxiliares();
    });

    it('exige un concepto antes de emitir', () => {
      component.tipoFacturacion = 'directa';
      component.nuevaFactura.concepto = '   ';
      component.guardarNuevaFactura();

      expect(avisarError).toHaveBeenCalled();
      expect(avisarError.calls.mostRecent().args[0]).toContain('concepto');
      httpMock.expectNone(req => req.url === `${api}/facturacion` && req.method === 'POST');
    });

    it('no envia los importes: los recalcula el servidor', () => {
      component.tipoFacturacion = 'directa';
      component.nuevaFactura.concepto = 'Venta de prueba';
      component.lineas = [{ descripcion: 'Cefalexina 250mg', cantidad: 2, precioUnitario: 6.25, productoCodigo: 'MED-001' }];
      component.guardarNuevaFactura();

      const peticion = httpMock.expectOne(req => req.url === `${api}/facturacion` && req.method === 'POST');
      const cuerpo = peticion.request.body;
      expect(cuerpo.subtotal).toBeUndefined();
      expect(cuerpo.impuestos).toBeUndefined();
      expect(cuerpo.total).toBeUndefined();
      expect(cuerpo.numero).toBeUndefined();
      expect(cuerpo.items[0].productoCodigo).toBe('MED-001');

      peticion.flush({
        ok: true,
        data: facturaServidor({ id: 11, numero: 'FAC-2026-000011', total: 15.68 }),
        mensaje: ''
      });

      expect(component.ultimaFactura?.numero).toBe('FAC-2026-000011');
      expect(component.ultimaFactura?.total).toBe(15.68);

      // Tras emitir se recarga el listado de facturas.
      httpMock.expectOne(req => req.url === `${api}/facturacion`).flush({ ok: true, data: [], mensaje: '' });
    });

    it('exige validar el cupon antes de emitir la factura', () => {
      component.tipoFacturacion = 'directa';
      component.nuevaFactura.concepto = 'Venta con cupon';
      component.nuevaFactura.cuponCodigo = 'PROMO10';
      component.lineas = [{ descripcion: 'Servicio', cantidad: 1, precioUnitario: 100 }];
      component.guardarNuevaFactura();

      expect(avisarError).toHaveBeenCalled();
      expect(avisarError.calls.mostRecent().args[0]).toContain('cupon');
      httpMock.expectNone(req => req.url === `${api}/facturacion` && req.method === 'POST');
    });

    it('envia el cupon validado y descarta el que no aplica', () => {
      component.tipoFacturacion = 'directa';
      component.nuevaFactura.concepto = 'Venta con cupon';
      component.nuevaFactura.cuponCodigo = 'PROMO10';
      component.lineas = [{ descripcion: 'Servicio', cantidad: 1, precioUnitario: 100 }];

      component.aplicarCupon();
      const peticion = httpMock.expectOne(req => req.url === `${api}/farmacia/cupones/validar`);
      expect(peticion.request.body.codigo).toBe('PROMO10');
      peticion.flush({
        ok: true,
        data: { codigo: 'PROMO10', aplicado: false, bruto: 100, descuento: 0, subtotal: 100, impuestos: 12, total: 112 },
        mensaje: ''
      });

      // El cupon que no aplica se descarta y se avisa al usuario.
      expect(component.simulacion).toBeUndefined();
      expect(component.errorCupon).toContain('no genera descuento');
      expect(component.nuevaFactura.cuponCodigo).toBe('');
    });

    it('no permite validar un cupon sin lineas en la factura', () => {
      component.nuevaFactura.cuponCodigo = 'PROMO10';
      component.aplicarCupon();

      expect(component.errorCupon).toContain('linea');
      httpMock.expectNone(req => req.url === `${api}/farmacia/cupones/validar`);
    });
  });

  describe('Lineas de la factura', () => {
    beforeEach(() => {
      component.abrirNuevaFactura();
      flushDatosAuxiliares();
      component.tipoFacturacion = 'directa';

      component.productos = [{
        codigo: 'MED-001', nombre: 'Cefalexina 250mg', categoria: 'Antibiotico',
        stock: 10, stockCritico: 5, precio: 6.25, prioridadAlta: false
      } as Producto];
    });

    it('agrega el producto del catalogo con su codigo (el precio lo pone el servidor)', () => {
      component.productoCatalogo = 'MED-001';
      component.cantidadCatalogo = 2;
      component.agregarProducto();

      expect(component.lineas.length).toBe(1);
      expect(component.lineas[0].productoCodigo).toBe('MED-001');
      expect(component.lineas[0].cantidad).toBe(2);
      expect(component.subtotalEstimado).toBe(12.5);
    });

    it('rechaza una cantidad mayor que el stock disponible', () => {
      component.productoCatalogo = 'MED-001';
      component.cantidadCatalogo = 99;
      component.agregarProducto();

      expect(component.lineas.length).toBe(0);
      expect(avisarError).toHaveBeenCalled();
    });

    it('rechaza una cantidad cero o negativa', () => {
      component.productoCatalogo = 'MED-001';
      component.cantidadCatalogo = 0;
      component.agregarProducto();

      expect(component.lineas.length).toBe(0);
      expect(avisarError).toHaveBeenCalled();
    });

    it('exige descripcion y precio en los conceptos libres', () => {
      component.conceptoLibre = '   ';
      component.precioLibre = 10;
      component.agregarConceptoLibre();
      expect(component.lineas.length).toBe(0);

      component.conceptoLibre = 'Consulta';
      component.precioLibre = 0;
      component.agregarConceptoLibre();
      expect(component.lineas.length).toBe(0);

      component.precioLibre = 45;
      component.agregarConceptoLibre();
      expect(component.lineas.length).toBe(1);
      expect(component.lineas[0].categoria).toBe('Concepto');
    });
  });

  describe('Descarga del PDF', () => {
    it('el rol CLIENTE no puede descargar el PDF de la factura', () => {
      TestBed.inject(AutorizacionService).guardarSesion({ token: 'x', roles: ['ROLE_CLIENTE'] });
      expect(component.esCliente).toBe(true);

      component.descargarPdf(component.facturas[0]);
      httpMock.expectNone(req => req.url.includes('/pdf'));
      expect(avisarError).toHaveBeenCalled();
    });

    it('el personal autorizado descarga el PDF generado por el backend', () => {
      TestBed.inject(AutorizacionService).guardarSesion({ token: 'x', roles: ['ROLE_ADMIN'] });
      expect(component.esCliente).toBe(false);

      component.descargarPdf(component.facturas[0]);
      const peticion = httpMock.expectOne(req => req.url.includes('/pdf'));
      expect(peticion.request.method).toBe('GET');
      expect(peticion.request.url).toContain('/facturas/7/pdf');
      peticion.flush(new Blob(['%PDF-1.4'], { type: 'application/pdf' }));
    });
  });

  describe('Comprobante imprimible', () => {
    it('escapa el contenido para no permitir inyeccion de HTML', () => {
      const html = (component as any).crearComprobanteHtml(facturaServidor({
        concepto: '<script>alert("xss")</script>',
        cliente: 'Ana <b>Molina</b>'
      }));

      expect(html).not.toContain('<script>alert');
      expect(html).toContain('&lt;script&gt;');
      expect(html).toContain('Ana &lt;b&gt;Molina&lt;/b&gt;');
    });
  });
});
