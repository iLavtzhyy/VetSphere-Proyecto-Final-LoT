import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import {
  AnalisisRotacion, Cliente, Cupon, EstadoRotacion, Hospitalizacion, Mascota, Producto, RotacionProducto, SimulacionCupon
} from '../../core/modelos';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { IconoComponent } from '../../shared/components/icono/icono';
import { AutorizacionService } from '../../core/autorizacion.service';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { NotificacionService } from '../../core/notificacion.service';
import { environment } from '../../../environments/environment';

type PestaniaFarmacia = 'inventario' | 'rotacion' | 'cupones';

/** Datos del formulario de cupon (el resto lo calcula el servidor). */
interface FormularioCupon {
  id?: number;
  codigo: string;
  descripcion: string;
  tipoDescuento: string;
  valor: number;
  productoCodigo: string;
  categoria: string;
  minimoSubtotal: number | null;
  fechaInicio: string;
  fechaFin: string;
  usosMaximos: number | null;
  activo: boolean;
}

@Component({
  selector: 'app-farmacia',
  standalone: true,
  imports: [CommonModule, FormsModule, IconoComponent, CargadorComponent],
  animations: [fadeInUp],
  templateUrl: './farmacia.html',
  styleUrl: './farmacia.css'
})
export class FarmaciaComponent implements OnInit {
  // ------------------------------------------------------------------
  // Estado general
  // ------------------------------------------------------------------
  pestaniaActiva: PestaniaFarmacia = 'inventario';
  productos: Producto[] = [];
  cargando = false;
  busqueda = '';
  categoriaActiva = 'Todas';
  formularioAbierto = false;
  modoStock = false;
  editandoCodigo?: string;
  productoSeleccionado?: Producto;
  cantidadMovimiento = 0;
  stockExacto: number | null = null;
  mensaje = '';
  form: Producto = this.formularioVacio();

  // ------------------------------------------------------------------
  // Analisis de rotacion de inventario
  // ------------------------------------------------------------------
  rotacion?: AnalisisRotacion;
  cargandoRotacion = false;
  diasVentana = 30;
  diasUmbral = 30;
  generandoPromociones = false;
  filtroRotacion: 'TODOS' | EstadoRotacion = 'TODOS';

  // ------------------------------------------------------------------
  // Cupones de descuento
  // ------------------------------------------------------------------
  cupones: Cupon[] = [];
  cargandoCupones = false;
  formularioCuponAbierto = false;
  editandoCuponId?: number;
  formCupon: FormularioCupon = this.formularioCuponVacio();

  // ------------------------------------------------------------------
  // Venta de farmacia
  // ------------------------------------------------------------------
  mostrarFacturacionModal = false;
  productoAFacturar?: Producto;
  clientes: Cliente[] = [];
  mascotas: Mascota[] = [];
  mascotasFiltradas: Mascota[] = [];
  facturacionClienteId: number | null = null;
  facturacionMascotaId: number | null = null;
  hospitalizaciones: Hospitalizacion[] = [];
  facturarAEstancia = false;
  hospitalizacionId: number | null = null;
  facturacionCantidad = 1;
  facturacionEstadoPago = 'Pendiente';
  facturacionCuponCodigo = '';
  simulacion?: SimulacionCupon;
  simulando = false;
  ultimaFactura?: { numero: string; total: number; descuento: number; cupon: string | null };

  // ------------------------------------------------------------------
  // Integracion SOAP
  // ------------------------------------------------------------------
  soapAbierto = false;
  soapProbando = false;
  soapMensaje = '';
  soapProducto?: any;
  soapCodigo = '';
  soapStock = 0;
  soapCantidad = 1;

  get wsdlUrl() {
    return `${environment.apiUrl.replace(/\/api$/, '')}/soap/productos?wsdl`;
  }

  get esAdmin() {
    return this.autorizacion.tieneRol(['ROLE_ADMIN']);
  }

  constructor(private api: ApiService, public autorizacion: AutorizacionService, private notificaciones: NotificacionService) {}

  ngOnInit() {
    this.cargar();
    this.api.clientes().subscribe({
      next: r => this.clientes = r.data,
      error: () => this.clientes = []
    });
    this.api.mascotas().subscribe({
      next: r => this.mascotas = r.data,
      error: () => this.mascotas = []
    });
    this.api.hospitalizaciones().subscribe({
      next: r => this.hospitalizaciones = r.data || [],
      error: () => this.hospitalizaciones = []
    });
  }

  /** Cambia de pestana y carga los datos que necesite. */
  cambiarPestania(pestania: PestaniaFarmacia) {
    this.pestaniaActiva = pestania;
    this.mensaje = '';
    if (pestania === 'rotacion' && !this.rotacion) {
      this.cargarRotacion();
    }
    if (pestania === 'cupones' && this.cupones.length === 0) {
      this.cargarCupones();
    }
  }

  // ------------------------------------------------------------------
  // Inventario
  // ------------------------------------------------------------------

  cargar() {
    this.cargando = true;
    this.api.productos().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => this.productos = r.data,
      error: () => {
        this.productos = [];
        this.mensaje = 'No se pudo cargar farmacia. Revisa que el backend este encendido y que tu rol tenga permiso.';
      }
    });
  }

  nuevoProducto() {
    this.form = this.formularioVacio();
    this.editandoCodigo = undefined;
    this.modoStock = false;
    this.mensaje = '';
    this.formularioAbierto = true;
  }

  editarProducto(producto: Producto) {
    this.form = { ...producto };
    this.editandoCodigo = producto.codigo;
    this.modoStock = false;
    this.mensaje = '';
    this.formularioAbierto = true;
  }

  abrirStock(producto: Producto) {
    this.productoSeleccionado = producto;
    this.cantidadMovimiento = 0;
    this.stockExacto = null;
    this.modoStock = true;
    this.mensaje = '';
    this.formularioAbierto = true;
  }

  guardarProducto() {
    const datos = {
      ...this.form,
      stock: Number(this.form.stock || 0),
      stockCritico: Number(this.form.stockCritico || 0),
      precio: Number(this.form.precio || 0),
      descuento: this.form.descuento === null || this.form.descuento === undefined
        ? null
        : Number(this.form.descuento)
    };
    const peticion = this.editandoCodigo ? this.api.actualizarProducto(this.editandoCodigo, datos) : this.api.crearProducto(datos);
    peticion.subscribe({
      next: () => {
        this.mensaje = 'Producto guardado correctamente.';
        this.cargar();
        this.cerrarFormulario();
      },
      error: e => this.mensaje = e?.error?.mensaje || 'No se pudo guardar el producto.'
    });
  }

  guardarStock() {
    if (!this.productoSeleccionado) return;
    const codigo = this.productoSeleccionado.codigo;
    if (this.stockExacto !== null && this.stockExacto < 0) {
      this.mensaje = 'El stock no puede ser negativo.';
      return;
    }
    if ((this.stockExacto === null || this.stockExacto === undefined) && this.cantidadMovimiento <= 0) {
      this.mensaje = 'Indica una cantidad mayor que cero para sumar al inventario.';
      return;
    }
    const peticion = this.stockExacto !== null && this.stockExacto !== undefined
      ? this.api.actualizarStock(codigo, Number(this.stockExacto))
      : this.api.agregarStock(codigo, Number(this.cantidadMovimiento));
    peticion.subscribe({
      next: r => {
        this.productoSeleccionado = r.data;
        this.mensaje = 'Stock actualizado correctamente.';
        this.cargar();
        this.cerrarFormulario();
      },
      error: e => this.mensaje = e?.error?.mensaje || 'No se pudo actualizar el stock. Verifica permiso de recepcion/admin.'
    });
  }

  cerrarFormulario() {
    this.formularioAbierto = false;
    this.mensaje = '';
  }

  // ------------------------------------------------------------------
  // Rotacion de inventario
  // ------------------------------------------------------------------

  cargarRotacion() {
    this.cargandoRotacion = true;
    this.api.rotacionInventario(this.diasVentana, this.diasUmbral)
      .pipe(finalize(() => this.cargandoRotacion = false))
      .subscribe({
        next: r => this.rotacion = r.data,
        error: e => this.mensaje = e?.error?.mensaje || 'No se pudo analizar la rotacion de inventario.'
      });
  }

  /** Filtro rapido: solo los productos que necesitan promocion. */
  get productosEnAlerta(): RotacionProducto[] {
    const todos = this.rotacion?.productos ?? [];
    if (this.filtroRotacion === 'TODOS') return todos;
    return todos.filter(p => p.estado === this.filtroRotacion);
  }

  get resumenRotacion() {
    return this.rotacion?.resumen;
  }

  /** Etiqueta legible del estado de rotacion. */
  etiquetaEstado(estado: EstadoRotacion): string {
    switch (estado) {
      case 'SIN_ROTACION': return 'Sin rotacion';
      case 'BAJA_ROTACION': return 'Baja rotacion';
      default: return 'Rotacion saludable';
    }
  }

  claseEstado(estado: EstadoRotacion): string {
    switch (estado) {
      case 'SIN_ROTACION': return 'badge-danger';
      case 'BAJA_ROTACION': return 'badge-warn';
      default: return 'badge-ok';
    }
  }

  /** Dias desde la ultima venta (texto legible). */
  textoDiasSinVenta(dias: number | null): string {
    if (dias === null || dias === undefined) return 'Nunca vendio';
    if (dias === 0) return 'Hoy';
    if (dias === 1) return 'Ayer';
    if (dias < 30) return `Hace ${dias} dias`;
    const meses = Math.floor(dias / 30);
    return `Hace ${meses} ${meses === 1 ? 'mes' : 'meses'}`;
  }

  /**
   * Pide al servidor que genere promociones para los productos de baja
   * rotacion. El descuento de cada cupon lo calcula el backend a partir de los
   * dias que el producto lleva sin venderse.
   */
  generarPromociones() {
    this.generandoPromociones = true;
    this.api.generarCuponesBajaRotacion({
      diasSinVenta: this.diasUmbral,
      duracionDias: 30
    }).pipe(finalize(() => this.generandoPromociones = false)).subscribe({
      next: r => {
        const d = r.data;
        this.mensaje = `Se generaron ${d.creados} promocion(es) y se omitieron ${d.omitidos}.`;
        this.notificaciones.mostrarExito(this.mensaje);
        this.cargarRotacion();
        this.cargarCupones();
      },
      error: e => this.mensaje = e?.error?.mensaje || 'No se pudieron generar las promociones.'
    });
  }

  // ------------------------------------------------------------------
  // Cupones
  // ------------------------------------------------------------------

  cargarCupones() {
    this.cargandoCupones = true;
    this.api.cupones().pipe(finalize(() => this.cargandoCupones = false)).subscribe({
      next: r => this.cupones = r.data,
      error: e => this.mensaje = e?.error?.mensaje || 'No se pudieron cargar los cupones.'
    });
  }

  nuevoCupon() {
    this.formCupon = this.formularioCuponVacio();
    this.editandoCuponId = undefined;
    this.formularioCuponAbierto = true;
    this.mensaje = '';
  }

  editarCupon(cupon: Cupon) {
    this.formCupon = {
      id: cupon.id,
      codigo: cupon.codigo,
      descripcion: cupon.descripcion || '',
      tipoDescuento: cupon.tipoDescuento,
      valor: Number(cupon.valor),
      productoCodigo: cupon.producto?.codigo || '',
      categoria: cupon.categoria || '',
      minimoSubtotal: cupon.minimoSubtotal ?? null,
      fechaInicio: cupon.fechaInicio ? cupon.fechaInicio.slice(0, 16) : '',
      fechaFin: cupon.fechaFin ? cupon.fechaFin.slice(0, 16) : '',
      usosMaximos: cupon.usosMaximos ?? null,
      activo: cupon.activo
    };
    this.editandoCuponId = cupon.id;
    this.formularioCuponAbierto = true;
    this.mensaje = '';
  }

  guardarCupon() {
    if (!this.formCupon.codigo.trim() && !this.editandoCuponId) {
      this.mensaje = 'Escribe un codigo para el cupon (o dejalo vacio para generarlo automaticamente).';
      return;
    }
    if (this.formCupon.valor === null || Number(this.formCupon.valor) < 0) {
      this.mensaje = 'El valor del descuento no puede ser negativo.';
      return;
    }
    if (this.formCupon.tipoDescuento === 'PORCENTAJE' && Number(this.formCupon.valor) > 100) {
      this.mensaje = 'El descuento porcentual no puede superar el 100%.';
      return;
    }
    const datos: any = {
      codigo: this.formCupon.codigo.trim() || null,
      descripcion: this.formCupon.descripcion,
      tipoDescuento: this.formCupon.tipoDescuento,
      valor: Number(this.formCupon.valor),
      productoCodigo: this.formCupon.productoCodigo.trim() || null,
      categoria: this.formCupon.categoria.trim() || null,
      minimoSubtotal: this.formCupon.minimoSubtotal === null || this.formCupon.minimoSubtotal === ('' as any)
        ? null
        : Number(this.formCupon.minimoSubtotal),
      fechaInicio: this.formCupon.fechaInicio ? new Date(this.formCupon.fechaInicio).toISOString().slice(0, 19) : null,
      fechaFin: this.formCouponFechaFin(),
      usosMaximos: this.formCupon.usosMaximos === null ? null : Number(this.formCupon.usosMaximos),
      activo: this.formCupon.activo
    };
    const peticion = this.editandoCuponId
      ? this.api.actualizarCupon(this.editandoCuponId, datos)
      : this.api.crearCupon(datos);
    peticion.subscribe({
      next: () => {
        this.mensaje = 'Cupon guardado correctamente.';
        this.notificaciones.mostrarExito(this.mensaje);
        this.formularioCuponAbierto = false;
        this.cargarCupones();
      },
      error: e => this.mensaje = e?.error?.mensaje || 'No se pudo guardar el cupon.'
    });
  }

  private formCouponFechaFin(): string | null {
    if (!this.formCupon.fechaFin) return null;
    return new Date(this.formCupon.fechaFin).toISOString().slice(0, 19);
  }

  alternarCupon(cupon: Cupon) {
    this.api.estadoCupon(cupon.id, !cupon.activo).subscribe({
      next: () => {
        this.mensaje = `Cupon ${cupon.codigo} ${cupon.activo ? 'desactivado' : 'activado'}.`;
        this.cargarCupones();
      },
      error: e => this.mensaje = e?.error?.mensaje || 'No se pudo cambiar el estado del cupon.'
    });
  }

  eliminarCupon(cupon: Cupon) {
    if (!confirm(`Eliminar el cupon ${cupon.codigo}?`)) return;
    this.api.eliminarCupon(cupon.id).subscribe({
      next: () => {
        this.mensaje = `Cupon ${cupon.codigo} eliminado.`;
        this.cargarCupones();
      },
      error: e => this.mensaje = e?.error?.mensaje || 'No se pudo eliminar el cupon.'
    });
  }

  /** Texto del descuento aplicado por un cupon. */
  textoDescuentoCupon(cupon: Cupon): string {
    if (cupon.tipoDescuento === 'VALOR_FIJO') {
      return `-$ ${Number(cupon.valor).toFixed(2)}`;
    }
    return `-${Number(cupon.valor).toFixed(2)}%`;
  }

  /** Alcance del cupon: producto, categoria o global. */
  textoAlcance(cupon: Cupon): string {
    if (cupon.producto?.nombre) return `Producto: ${cupon.producto.nombre}`;
    if (cupon.categoria) return `Categoria: ${cupon.categoria}`;
    return 'Todos los productos';
  }

  // ------------------------------------------------------------------
  // Venta de farmacia
  // ------------------------------------------------------------------

  abrirFacturar(producto: Producto) {
    this.productoAFacturar = producto;
    this.facturacionClienteId = null;
    this.facturacionMascotaId = null;
    this.facturacionCantidad = 1;
    this.facturacionEstadoPago = 'Pendiente';
    this.facturacionCuponCodigo = '';
    this.simulacion = undefined;
    this.facturarAEstancia = false;
    this.hospitalizacionId = null;
    this.ultimaFactura = undefined;
    this.mascotasFiltradas = [];
    this.mostrarFacturacionModal = true;
    this.mensaje = '';
  }

  get estanciasActivas() {
    return this.hospitalizaciones.filter(h => !h.facturaId && !h.alta && !/alta/i.test(h.estado || ''));
  }

  agregarAEstancia() {
    if (!this.hospitalizacionId || !this.productoAFacturar || this.facturacionCantidad < 1) return;
    this.simulando = true;
    this.api.agregarConsumoHospitalizacion(this.hospitalizacionId, this.productoAFacturar.codigo, this.facturacionCantidad)
      .pipe(finalize(() => this.simulando = false)).subscribe({
        next: () => {
          this.notificaciones.mostrarExito('Producto agregado a la cuenta de hospitalización. Se cobrará al dar el alta.');
          this.mostrarFacturacionModal = false;
          this.cargar();
        },
        error: e => this.mensaje = e?.error?.mensaje || 'No se pudo agregar el producto a la cuenta de hospitalización.'
      });
  }

  onFacturacionClienteChange() {
    this.facturacionMascotaId = null;
    if (this.facturacionClienteId) {
      this.mascotasFiltradas = this.mascotas.filter(m => m.propietario?.id === this.facturacionClienteId);
    } else {
      this.mascotasFiltradas = [];
    }
  }

  /** Lineas del carrito actual (el precio real lo resuelve el servidor). */
  private lineasCarrito() {
    if (!this.productoAFacturar) return [];
    return [{
      descripcion: this.productoAFacturar.nombre,
      cantidad: this.facturacionCantidad,
      precioUnitario: Number(this.productoAFacturar.precio),
      productoCodigo: this.productoAFacturar.codigo,
      categoria: this.productoAFacturar.categoria
    }];
  }

  /**
   * Pide al servidor el resultado de aplicar el cupon. La simulacion usa el
   * mismo calculo que la factura, de modo que el importe mostrado es real.
   */
  simularCupon() {
    if (!this.productoAFacturar) return;
    if (!this.facturacionCuponCodigo.trim()) {
      this.simulacion = undefined;
      return;
    }
    this.simulando = true;
    this.api.validarCupon({
      codigo: this.facturacionCuponCodigo.trim(),
      concepto: `Compra Farmacia: ${this.productoAFacturar.nombre}`,
      items: this.lineasCarrito()
    }).pipe(finalize(() => this.simulando = false)).subscribe({
      next: r => {
        this.simulacion = r.data;
        this.notificaciones.mostrarExito(`Cupon ${r.data.codigo} aplicado: -$ ${Number(r.data.descuento).toFixed(2)}`);
      },
      error: e => {
        this.simulacion = undefined;
        this.notificaciones.mostrarError(e?.error?.mensaje || 'El cupon no se pudo aplicar.');
      }
    });
  }

  /** Totales mostrados mientras se confirma: los del servidor si hay cupon simulado. */
  get vistaPrevia() {
    const producto = this.productoAFacturar;
    if (!producto) return { bruto: 0, descuento: 0, subtotal: 0, impuestos: 0, total: 0 };
    const bruto = Number(producto.precio) * this.facturacionCantidad;
    if (this.simulacion?.aplicado) {
      return {
        bruto: Number(this.simulacion.bruto),
        descuento: Number(this.simulacion.descuento),
        subtotal: Number(this.simulacion.subtotal),
        impuestos: Number(this.simulacion.impuestos),
        total: Number(this.simulacion.total)
      };
    }
    const subtotal = Number(bruto.toFixed(2));
    const impuestos = Number((subtotal * 0.15).toFixed(2));
    return { bruto: subtotal, descuento: 0, subtotal, impuestos, total: Number((subtotal + impuestos).toFixed(2)) };
  }

  /**
   * Emite la factura. El backend recalcula los importes con los precios del
   * catalogo, aplica el cupon y descuenta el stock en la misma transaccion.
   */
  confirmarFacturacion() {
    if (!this.productoAFacturar) return;
    if (!this.facturacionClienteId || !this.facturacionMascotaId) {
      this.notificaciones.mostrarError('Por favor selecciona un cliente y una mascota.');
      return;
    }
    if (this.facturacionCantidad <= 0 || this.facturacionCantidad > this.productoAFacturar.stock) {
      this.notificaciones.mostrarError('Cantidad invalida o superior al stock disponible.');
      return;
    }
    const payload = {
      concepto: `Compra Farmacia: ${this.productoAFacturar.nombre} (x${this.facturacionCantidad})`,
      estadoPago: this.facturacionEstadoPago,
      cliente: { id: this.facturacionClienteId },
      mascota: { id: this.facturacionMascotaId },
      items: this.lineasCarrito(),
      cuponCodigo: this.facturacionCuponCodigo.trim() || null
    };
    this.api.crearFactura(payload).subscribe({
      next: r => {
        const factura = r.data;
        this.ultimaFactura = {
          numero: factura.numero,
          total: Number(factura.total),
          descuento: Number(factura.descuento || 0),
          cupon: factura.cuponCodigo || null
        };
        this.mensaje = 'Producto facturado y stock actualizado correctamente.';
        this.notificaciones.mostrarExito(this.mensaje);
        this.mostrarFacturacionModal = false;
        this.cargar();
        if (this.rotacion) {
          this.cargarRotacion();
        }
      },
      error: e => this.mensaje = e?.error?.mensaje || e?.error?.message || 'No se pudo emitir la factura. Revisa el stock y los permisos.'
    });
  }

  // ------------------------------------------------------------------
  // Integracion SOAP
  // ------------------------------------------------------------------

  probarSoap() {
    const codigo = this.soapCodigo || this.productos[0]?.codigo;
    if (!codigo) { this.soapMensaje = 'No hay productos para consultar.'; return; }
    this.soapCodigo = codigo;
    this.soapProbando = true;
    this.soapMensaje = 'Consultando el inventario SOAP protegido...';
    this.api.soapProducto(codigo).subscribe({
      next: r => { this.soapProducto = r.data; this.soapStock = Number(r.data.stock || 0); this.soapMensaje = `Conexion SOAP correcta. ${r.data.nombre}: ${r.data.stock} unidades.`; this.soapProbando = false; },
      error: e => { this.soapMensaje = e?.error?.mensaje || 'No se pudo conectar al servicio SOAP. Revisa que el backend este activo.'; this.soapProbando = false; }
    });
  }

  actualizarStockSoap(modo: 'sumar' | 'descontar' | 'exacto') {
    if (!this.soapProducto || !this.soapCodigo) { this.probarSoap(); return; }
    if (modo === 'exacto' && (!Number.isInteger(this.soapStock) || this.soapStock < 0)) { this.soapMensaje = 'El stock debe ser un numero entero igual o mayor que cero.'; return; }
    if (modo !== 'exacto' && (!Number.isInteger(this.soapCantidad) || this.soapCantidad < 1)) { this.soapMensaje = 'Indica una cantidad entera mayor que cero.'; return; }
    const peticion = modo === 'exacto'
      ? this.api.soapActualizarStock(this.soapCodigo, this.soapStock)
      : modo === 'sumar'
        ? this.api.soapAgregarStock(this.soapCodigo, this.soapCantidad)
        : this.api.soapDescontarStock(this.soapCodigo, this.soapCantidad);
    this.soapProbando = true;
    peticion.subscribe({
      next: r => { this.soapProducto = { ...this.soapProducto, stock: r.data.stockActual }; this.soapStock = Number(r.data.stockActual); this.soapMensaje = `${r.data.mensaje || 'Cambio realizado por SOAP.'} Stock nuevo: ${r.data.stockActual}.`; this.soapProbando = false; this.cargar(); },
      error: e => { this.soapMensaje = e?.error?.mensaje || 'No se pudo actualizar el inventario por SOAP.'; this.soapProbando = false; }
    });
  }

  // ------------------------------------------------------------------
  // Utilidades
  // ------------------------------------------------------------------

  formularioVacio(): Producto {
    return { codigo: '', nombre: '', categoria: 'Medicamentos', stock: 0, stockCritico: 5, precio: 0, prioridadAlta: false, descuento: 0 };
  }

  formularioCuponVacio(): FormularioCupon {
    const inicio = new Date();
    const fin = new Date();
    fin.setDate(fin.getDate() + 30);
    return {
      codigo: '',
      descripcion: '',
      tipoDescuento: 'PORCENTAJE',
      valor: 10,
      productoCodigo: '',
      categoria: '',
      minimoSubtotal: null,
      fechaInicio: inicio.toISOString().slice(0, 16),
      fechaFin: fin.toISOString().slice(0, 16),
      usosMaximos: null,
      activo: true
    };
  }

  get criticos() { return this.productos.filter(p => p.stock <= p.stockCritico).length; }
  get stockTotal() { return this.productos.reduce((s, p) => s + Number(p.stock || 0), 0); }
  get valorInventario() { return this.productos.reduce((s, p) => s + Number(p.precio || 0) * Number(p.stock || 0), 0); }
  get categorias() {
    return ['Todas', ...Array.from(new Set(this.productos.map(p => p.categoria || 'Sin categoria'))).sort()];
  }
  /** Categorias reales (sin la opcion "Todas") para los selectores. */
  get categoriasReales() {
    return this.categorias.filter(c => c !== 'Todas');
  }
  get resumenCategorias() {
    return this.categorias.filter(c => c !== 'Todas').map(categoria => {
      const productos = this.productos.filter(p => (p.categoria || 'Sin categoria') === categoria);
      return {
        categoria,
        productos: productos.length,
        stock: productos.reduce((s, p) => s + Number(p.stock || 0), 0),
        criticos: productos.filter(p => p.stock <= p.stockCritico).length,
        valor: productos.reduce((s, p) => s + Number(p.precio || 0) * Number(p.stock || 0), 0)
      };
    });
  }
  get filtrados() {
    const q = this.busqueda.toLowerCase();
    return this.productos.filter(p => {
      const coincideTexto = `${p.codigo} ${p.nombre} ${p.categoria}`.toLowerCase().includes(q);
      const coincideCategoria = this.categoriaActiva === 'Todas' || (p.categoria || 'Sin categoria') === this.categoriaActiva;
      return coincideTexto && coincideCategoria;
    });
  }
  get gruposFiltrados() {
    return this.categorias
      .filter(categoria => categoria !== 'Todas')
      .map(categoria => ({
        categoria,
        productos: this.filtrados.filter(p => (p.categoria || 'Sin categoria') === categoria)
      }))
      .filter(grupo => grupo.productos.length);
  }
}
