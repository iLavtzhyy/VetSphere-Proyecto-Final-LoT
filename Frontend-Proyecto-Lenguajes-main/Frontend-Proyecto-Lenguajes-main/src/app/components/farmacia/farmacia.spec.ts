import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { FarmaciaComponent } from './farmacia';
import { environment } from '../../../environments/environment';
import { AnalisisRotacion, Producto } from '../../core/modelos';
import { NotificacionService } from '../../core/notificacion.service';

/**
 * Pruebas del modulo de farmacia:
 * inventario (stock critico, filtros), analisis de rotacion y cupones.
 *
 * Se comprueba sobre todo que la vista NO recalcula los importes: los totales
 * con cupon salen de la simulacion que calcula el servidor.
 */
describe('FarmaciaComponent', () => {
  let component: FarmaciaComponent;
  let fixture: ComponentFixture<FarmaciaComponent>;
  let httpMock: HttpTestingController;
  let notificaciones: NotificacionService;
  let avisarError: jasmine.Spy;

  const api = environment.apiUrl;

  const producto = (over: Partial<Producto> = {}): Producto => ({
    codigo: 'MED-001',
    nombre: 'Cefalexina 250mg',
    categoria: 'Antibiotico',
    stock: 10,
    stockCritico: 5,
    precio: 6.25,
    prioridadAlta: false,
    ...over
  });

  const analisis = (): AnalisisRotacion => ({
    resumen: {
      diasVentana: 30,
      diasUmbral: 30,
      totalProductos: 3,
      productosSinRotacion: 1,
      productosBajaRotacion: 1,
      productosSaludables: 1,
      unidadesVendidas: 4,
      ingresos: 25,
      capitalInventario: 300,
      capitalDormido: 180
    },
    productos: [
      {
        id: 1, codigo: 'MED-001', nombre: 'Cefalexina 250mg', categoria: 'Antibiotico',
        stock: 10, precio: 6.25, descuentoProducto: 0, unidadesVendidas: 4, unidadesTotales: 4,
        ingresos: 25, ultimaVenta: '2026-10-01T10:00:00', diasSinVenta: 3, diasCobertura: 45,
        valorInventario: 62.5, estado: 'ROTACION_SALUDABLE', descuentoSugerido: 0, cuponActivo: false
      },
      {
        id: 2, codigo: 'MED-002', nombre: 'Meloxicam', categoria: 'Analgesia',
        stock: 18, precio: 8, descuentoProducto: 0, unidadesVendidas: 0, unidadesTotales: 0,
        ingresos: 0, ultimaVenta: null, diasSinVenta: null, diasCobertura: -1,
        valorInventario: 144, estado: 'SIN_ROTACION', descuentoSugerido: 20, cuponActivo: false
      },
      {
        id: 3, codigo: 'MED-003', nombre: 'Atropina', categoria: 'Emergencia',
        stock: 25, precio: 9.75, descuentoProducto: 0, unidadesVendidas: 0, unidadesTotales: 2,
        ingresos: 0, ultimaVenta: '2026-07-01T10:00:00', diasSinVenta: 45, diasCobertura: -1,
        valorInventario: 243.75, estado: 'BAJA_ROTACION', descuentoSugerido: 15, cuponActivo: true
      }
    ]
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FarmaciaComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    fixture = TestBed.createComponent(FarmaciaComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    notificaciones = TestBed.inject(NotificacionService);
    avisarError = spyOn(notificaciones, 'mostrarError');
    fixture.detectChanges();

    // Peticiones iniciales del ngOnInit.
    httpMock.expectOne(req => req.url === `${api}/farmacia/inventario`).flush({ ok: true, data: [], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/clientes`).flush({ ok: true, data: [], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/mascotas`).flush({ ok: true, data: [], mensaje: '' });
    httpMock.expectOne(req => req.url === `${api}/hospitalizaciones`).flush({ ok: true, data: [], mensaje: '' });
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  describe('Inventario', () => {
    beforeEach(() => {
      component.productos = [
        producto(),
        producto({ codigo: 'MED-002', nombre: 'Meloxicam', categoria: 'Analgesia', stock: 3, stockCritico: 5, precio: 8 }),
        producto({ codigo: 'MED-003', nombre: 'Atropina', categoria: 'Antibiotico', stock: 20, stockCritico: 5, precio: 9.75 })
      ];
    });

    it('cuenta como criticos los productos con stock igual o menor al stock critico', () => {
      expect(component.criticos).toBe(1);
    });

    it('calcula el stock total y el valor del inventario', () => {
      expect(component.stockTotal).toBe(33);
      expect(component.valorInventario).toBeCloseTo(10 * 6.25 + 3 * 8 + 20 * 9.75, 2);
    });

    it('lista las categorias reales sin la opcion Todas y en orden alfabetico', () => {
      expect(component.categorias).toEqual(['Todas', 'Analgesia', 'Antibiotico']);
      expect(component.categoriasReales).toEqual(['Analgesia', 'Antibiotico']);
    });

    it('filtra por texto y por categoria', () => {
      component.busqueda = 'melox';
      expect(component.filtrados.map(p => p.codigo)).toEqual(['MED-002']);

      component.busqueda = '';
      component.categoriaActiva = 'Antibiotico';
      expect(component.filtrados.map(p => p.codigo)).toEqual(['MED-001', 'MED-003']);
    });

    it('agrupa las categorias con su resumen de inventario', () => {
      const resumen = component.resumenCategorias;
      expect(resumen.length).toBe(2);
      expect(resumen.find(r => r.categoria === 'Antibiotico')?.criticos).toBe(0);
      expect(resumen.find(r => r.categoria === 'Analgesia')?.productos).toBe(1);
    });
  });

  describe('Analisis de rotacion', () => {
    it('carga el analisis con la ventana y el umbral solicitados', () => {
      component.diasVentana = 60;
      component.diasUmbral = 45;
      component.cargarRotacion();

      const peticion = httpMock.expectOne(req => req.url.includes('/farmacia/rotacion'));
      expect(peticion.request.url).toContain('dias=60');
      expect(peticion.request.url).toContain('umbral=45');
      peticion.flush({ ok: true, data: analisis(), mensaje: '' });

      expect(component.rotacion?.resumen.capitalDormido).toBe(180);
      expect(component.rotacion?.productos.length).toBe(3);
    });

    it('filtra los productos que necesitan promocion', () => {
      component.rotacion = analisis();

      component.filtroRotacion = 'BAJA_ROTACION';
      expect(component.productosEnAlerta.map(p => p.codigo)).toEqual(['MED-003']);

      component.filtroRotacion = 'SIN_ROTACION';
      expect(component.productosEnAlerta.map(p => p.codigo)).toEqual(['MED-002']);

      component.filtroRotacion = 'TODOS';
      expect(component.productosEnAlerta.length).toBe(3);
    });

    it('traduce el estado de rotacion a una etiqueta y a una clase', () => {
      expect(component.etiquetaEstado('SIN_ROTACION')).toBe('Sin rotacion');
      expect(component.etiquetaEstado('BAJA_ROTACION')).toBe('Baja rotacion');
      expect(component.etiquetaEstado('ROTACION_SALUDABLE')).toBe('Rotacion saludable');
      expect(component.claseEstado('SIN_ROTACION')).toBe('badge-danger');
      expect(component.claseEstado('BAJA_ROTACION')).toBe('badge-warn');
      expect(component.claseEstado('ROTACION_SALUDABLE')).toBe('badge-ok');
    });

    it('explica los dias sin venta', () => {
      expect(component.textoDiasSinVenta(null)).toBe('Nunca vendio');
      expect(component.textoDiasSinVenta(0)).toBe('Hoy');
      expect(component.textoDiasSinVenta(1)).toBe('Ayer');
      expect(component.textoDiasSinVenta(10)).toBe('Hace 10 dias');
      expect(component.textoDiasSinVenta(60)).toBe('Hace 2 meses');
    });
  });

  describe('Cupones', () => {
    it('no deja crear un cupon sin codigo', () => {
      component.nuevoCupon();
      component.formCupon.codigo = '  ';
      component.guardarCupon();

      expect(component.mensaje).toContain('codigo');
      httpMock.expectNone(req => req.url === `${api}/farmacia/cupones` && req.method === 'POST');
    });

    it('rechaza un descuento porcentual mayor al 100%', () => {
      component.nuevoCupon();
      component.formCupon.codigo = 'PROMO200';
      component.formCupon.tipoDescuento = 'PORCENTAJE';
      component.formCupon.valor = 200;
      component.guardarCupon();

      expect(component.mensaje).toContain('100%');
      httpMock.expectNone(req => req.url === `${api}/farmacia/cupones` && req.method === 'POST');
    });

    it('explica el alcance y el descuento de un cupon', () => {
      const base = {
        id: 1, codigo: 'PROMO10', tipoDescuento: 'PORCENTAJE', valor: 10, usosActuales: 0,
        activo: true, origen: 'MANUAL', fechaInicio: '2026-01-01T00:00:00', fechaFin: '2026-12-31T00:00:00'
      } as any;
      expect(component.textoDescuentoCupon(base)).toBe('-10.00%');
      expect(component.textoAlcance(base)).toBe('Todos los productos');

      const fijo = { ...base, tipoDescuento: 'VALOR_FIJO', valor: 5 };
      expect(component.textoDescuentoCupon(fijo)).toBe('-$ 5.00');

      const porProducto = { ...base, producto: { id: 2, codigo: 'MED-002', nombre: 'Meloxicam' } };
      expect(component.textoAlcance(porProducto)).toBe('Producto: Meloxicam');

      const porCategoria = { ...base, categoria: 'Analgesia' };
      expect(component.textoAlcance(porCategoria)).toBe('Categoria: Analgesia');
    });
  });

  describe('Venta de farmacia con cupon', () => {
    beforeEach(() => {
      component.abrirFacturar(producto({ stock: 10, precio: 20 }));
    });

    it('muestra la simulacion que devuelve el servidor, no un calculo local', () => {
      component.facturacionCuponCodigo = 'PROMO10';
      component.facturacionCantidad = 2;
      component.simularCupon();

      const peticion = httpMock.expectOne(req => req.url === `${api}/farmacia/cupones/validar`);
      expect(peticion.request.method).toBe('POST');
      peticion.flush({
        ok: true,
        data: { codigo: 'PROMO10', aplicado: true, bruto: 40, descuento: 4, subtotal: 36, impuestos: 4.32, total: 40.32 },
        mensaje: ''
      });

      const vista = component.vistaPrevia;
      expect(vista.bruto).toBe(40);
      expect(vista.descuento).toBe(4);
      expect(vista.subtotal).toBe(36);
      expect(vista.impuestos).toBe(4.32);
      expect(vista.total).toBe(40.32);
    });

    it('exige cliente y mascota antes de emitir la factura', () => {
      component.confirmarFacturacion();
      expect(avisarError).toHaveBeenCalled();
      expect(avisarError.calls.mostRecent().args[0]).toContain('cliente');
      httpMock.expectNone(req => req.url === `${api}/facturacion` && req.method === 'POST');
    });

    it('rechaza una cantidad superior al stock disponible', () => {
      component.facturacionClienteId = 1;
      component.facturacionMascotaId = 2;
      component.facturacionCantidad = 99;
      component.confirmarFacturacion();

      expect(avisarError).toHaveBeenCalled();
      httpMock.expectNone(req => req.url === `${api}/facturacion` && req.method === 'POST');
    });

    it('envia el cupon al crear la factura y muestra el numero que devuelve el servidor', () => {
      component.facturacionClienteId = 1;
      component.facturacionMascotaId = 2;
      component.facturacionCantidad = 2;
      component.facturacionCuponCodigo = 'PROMO10';
      component.confirmarFacturacion();

      const peticion = httpMock.expectOne(req => req.url === `${api}/facturacion` && req.method === 'POST');
      const cuerpo = peticion.request.body;
      expect(cuerpo.cuponCodigo).toBe('PROMO10');
      expect(cuerpo.items[0].productoCodigo).toBe('MED-001');
      // El frontend NO envia los importes: los calcula el servidor.
      expect(cuerpo.subtotal).toBeUndefined();
      expect(cuerpo.total).toBeUndefined();

      peticion.flush({
        ok: true,
        data: { id: 9, numero: 'FAC-2026-000009', total: 40.32, descuento: 4, cuponCodigo: 'PROMO10' },
        mensaje: ''
      });

      expect(component.ultimaFactura?.numero).toBe('FAC-2026-000009');
      expect(component.ultimaFactura?.descuento).toBe(4);
      expect(component.ultimaFactura?.cupon).toBe('PROMO10');

      // Tras emitir se recarga el inventario para mostrar el stock actualizado.
      httpMock.expectOne(req => req.url === `${api}/farmacia/inventario`).flush({ ok: true, data: [], mensaje: '' });
    });
  });
});
