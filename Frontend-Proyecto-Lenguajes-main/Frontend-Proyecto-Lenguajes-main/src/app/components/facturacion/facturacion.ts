import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { Factura, Producto, SimulacionCupon } from '../../core/modelos';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { IconoComponent } from '../../shared/components/icono/icono';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { NotificacionService } from '../../core/notificacion.service';
import { AutorizacionService } from '../../core/autorizacion.service';
import * as QRCode from 'qrcode';

/**
 * Linea de factura en edicion.
 * Informar `productoCodigo` obliga al backend a tomar el precio del catalogo,
 * por lo que nunca se envia un precio manipulado para productos de farmacia.
 */
interface LineaFactura {
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  productoCodigo?: string;
  categoria?: string;
}

/** Datos del formulario de emision (los importes los calcula el servidor). */
interface FormularioFactura {
  clienteId: number | null;
  mascotaId: number | null;
  concepto: string;
  estadoPago: string;
  metodoPago: string;
  cuponCodigo: string;
  fecha: string;
}

@Component({
  selector: 'app-facturacion',
  standalone: true,
  imports: [CommonModule, FormsModule, IconoComponent, CargadorComponent],
  animations: [fadeInUp],
  templateUrl: './facturacion.html',
  styleUrl: './facturacion.css'
})
export class FacturacionComponent implements OnInit {
  facturas: Factura[] = [];
  busquedaFactura = '';
  estadoFiltro = '';
  fechaDesde = '';
  fechaHasta = '';
  cargando = false;
  facturaSeleccionada?: Factura;
  facturaPago?: Factura;
  /** Factura recien emitida: conserva los importes devueltos por el servidor. */
  ultimaFactura?: Factura;
  estadoPago = 'Pendiente';
  metodoPago = 'Efectivo';
  mensajePago = '';
  fechaEmision = new Date();

  clientes: any[] = [];
  mascotas: any[] = [];
  todasConsultas: any[] = [];
  todasCirugias: any[] = [];
  todosLabs: any[] = [];
  todasHosp: any[] = [];
  productos: Producto[] = [];

  mostrarFormFactura = false;
  tipoFacturacion: 'automatica' | 'directa' = 'automatica';
  mascotasFiltradas: any[] = [];
  serviciosPaciente: any[] = [];

  // Formulario de emision
  nuevaFactura: FormularioFactura = this.facturaVacia();
  lineas: LineaFactura[] = [];
  productoCatalogo = '';
  cantidadCatalogo = 1;
  conceptoLibre = '';
  cantidadLibre = 1;
  precioLibre = 0;

  // Cupon de descuento
  simulacion?: SimulacionCupon;
  errorCupon = '';
  simulando = false;

  emitiendo = false;
  datosTransferencia: any = null;
  qrPagoUrl = '';

  constructor(
    private api: ApiService,
    private notificaciones: NotificacionService,
    public autorizacion: AutorizacionService
  ) {}

  ngOnInit() {
    this.cargarFacturas();
    this.api.datosPago().subscribe({
      next: r => { this.datosTransferencia = r.data; if (this.facturaSeleccionada) this.generarQrPago(this.facturaSeleccionada); },
      error: () => this.datosTransferencia = null
    });
  }

  // ------------------------------------------------------------------
  // Listado
  // ------------------------------------------------------------------

  private cargarFacturas() {
    this.cargando = true;
    this.api.facturas().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => this.facturas = (r.data || [])
        .map(f => this.normalizarFactura(f))
        .sort((a, b) => this.fechaOrden(b.fecha) - this.fechaOrden(a.fecha)),
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudieron cargar las facturas. Revisa que el backend este activo.')
    });
  }

  /**
   * Normaliza la respuesta del servidor a la vista de la pantalla.
   * El listado devuelve `cliente`/`mascota` como texto y la creacion como
   * objetos; aqui se unifican para no inventar datos.
   */
  private normalizarFactura(factura: any): Factura {
    const total = Number(factura?.total || 0);
    const impuestos = factura?.impuestos === null || factura?.impuestos === undefined
      ? undefined
      : Number(factura.impuestos);
    const descuento = factura?.descuento === null || factura?.descuento === undefined
      ? undefined
      : Number(factura.descuento);
    return {
      id: Number(factura?.id),
      numero: factura?.numero || '',
      fecha: factura?.fecha || '',
      cliente: this.textoCliente(factura?.cliente),
      mascota: this.textoMascota(factura?.mascota),
      concepto: factura?.concepto || '',
      subtotal: factura?.subtotal === null || factura?.subtotal === undefined
        ? undefined
        : Number(factura.subtotal),
      impuestos,
      descuento,
      cuponCodigo: factura?.cuponCodigo || undefined,
      observaciones: factura?.observaciones || undefined,
      metodoPago: factura?.metodoPago || undefined,
      total,
      estadoPago: factura?.estadoPago || 'Pendiente'
    };
  }

  private textoCliente(cliente: any): string {
    if (!cliente) return 'Sin cliente';
    if (typeof cliente === 'string') return cliente;
    const nombre = `${cliente.nombres || ''} ${cliente.apellidos || ''}`.trim();
    return nombre || 'Sin cliente';
  }

  private textoMascota(mascota: any): string {
    if (!mascota) return 'Sin mascota';
    if (typeof mascota === 'string') return mascota;
    return mascota.nombre || 'Sin mascota';
  }

  get total() { return this.facturas.reduce((s, f) => s + Number(f.total || 0), 0); }
  get totalSubtotal() { return this.facturas.reduce((s, f) => s + this.subtotalDe(f), 0); }
  get totalDescuento() { return this.facturas.reduce((s, f) => s + this.descuentoDe(f), 0); }
  get totalImpuestos() { return this.facturas.reduce((s, f) => s + this.impuestosDe(f), 0); }
  get pendientes() { return this.facturas.filter(f => f.estadoPago === 'Pendiente').length; }
  get pagadas() { return this.facturas.filter(f => f.estadoPago === 'Pagado').length; }

  get facturasFiltradas(): Factura[] {
    const termino = this.busquedaFactura.trim().toLocaleLowerCase('es');
    return this.facturas.filter(f => {
      const fecha = this.fechaOrden(f.fecha);
      const texto = [f.numero, f.cliente, f.mascota, f.concepto, f.estadoPago, f.metodoPago,
        f.fecha ? new Date(f.fecha).toLocaleDateString('es-EC') : '']
        .filter(Boolean).join(' ').toLocaleLowerCase('es');
      const coincideTexto = !termino || texto.includes(termino);
      const coincideEstado = !this.estadoFiltro || f.estadoPago === this.estadoFiltro;
      const desde = this.fechaDesde ? new Date(`${this.fechaDesde}T00:00:00`).getTime() : -Infinity;
      const hasta = this.fechaHasta ? new Date(`${this.fechaHasta}T23:59:59.999`).getTime() : Infinity;
      return coincideTexto && coincideEstado && fecha >= desde && fecha <= hasta;
    });
  }

  limpiarFiltrosFacturas() {
    this.busquedaFactura = '';
    this.estadoFiltro = '';
    this.fechaDesde = '';
    this.fechaHasta = '';
  }

  get subtotalFiltrado() { return this.facturasFiltradas.reduce((s, f) => s + this.subtotalDe(f), 0); }
  get descuentoFiltrado() { return this.facturasFiltradas.reduce((s, f) => s + this.descuentoDe(f), 0); }
  get impuestosFiltrado() { return this.facturasFiltradas.reduce((s, f) => s + this.impuestosDe(f), 0); }
  get totalFiltrado() { return this.facturasFiltradas.reduce((s, f) => s + Number(f.total || 0), 0); }

  private fechaOrden(fecha?: string): number {
    if (!fecha) return 0;
    const valor = new Date(fecha).getTime();
    return Number.isFinite(valor) ? valor : 0;
  }

  /** El rol CLIENTE no puede descargar el PDF generado por el backend. */
  get esCliente(): boolean {
    return this.autorizacion.tieneRol(['ROLE_CLIENTE']);
  }

  /** Subtotal mostrado (dato del servidor; fallback solo para registros antiguos). */
  subtotalDe(factura: Factura): number {
    if (factura.subtotal !== undefined && factura.subtotal !== null) return Number(factura.subtotal);
    return Number(factura.total || 0) - this.impuestosDe(factura) - this.descuentoDe(factura);
  }

  /** IVA (15%) mostrado: siempre el valor devuelto por el servidor. */
  impuestosDe(factura: Factura): number {
    return factura.impuestos === undefined || factura.impuestos === null ? 0 : Number(factura.impuestos);
  }

  /** Descuento mostrado: siempre el valor devuelto por el servidor. */
  descuentoDe(factura: Factura): number {
    return factura.descuento === undefined || factura.descuento === null ? 0 : Number(factura.descuento);
  }

  estadoClase(factura: Factura) {
    if (factura.estadoPago === 'Pagado') return 'badge-ok';
    if (factura.estadoPago === 'Anulado') return 'badge-danger';
    return 'badge-warn';
  }

  seleccionar(factura: Factura) {
    this.facturaSeleccionada = factura;
    this.generarQrPago(factura);
  }

  private generarQrPago(factura: Factura) {
    const pago = this.datosTransferencia;
    if (!pago?.configurado || factura.estadoPago === 'Pagado' || factura.estadoPago === 'Anulado') {
      this.qrPagoUrl = '';
      return;
    }
    const contenido = [
      'VetSphere - Datos para transferencia',
      `${pago.banco} - ${pago.tipoCuenta}`,
      `Cuenta: ${pago.numeroCuenta}`,
      `Titular: ${pago.titular}`,
      `Cedula/RUC: ${pago.identificacion}`,
      `Factura: ${factura.numero}`,
      `Monto USD: ${Number(factura.total).toFixed(2)}`
    ].join('\n');
    QRCode.toDataURL(contenido, { width: 220, margin: 1, errorCorrectionLevel: 'M' })
      .then(url => this.qrPagoUrl = url)
      .catch(() => this.qrPagoUrl = '');
  }

  cerrarVistaPrevia() {
    this.facturaSeleccionada = undefined;
  }

  // ------------------------------------------------------------------
  // Formulario de emision
  // ------------------------------------------------------------------

  private facturaVacia(): FormularioFactura {
    return {
      clienteId: null,
      mascotaId: null,
      concepto: '',
      estadoPago: 'Pendiente',
      metodoPago: 'Efectivo',
      cuponCodigo: '',
      fecha: this.fechaLocalActual()
    };
  }

  /** Fecha local en formato ISO corto (el servidor la toma tal cual). */
  private fechaLocalActual(): string {
    const ahora = new Date();
    const mes = String(ahora.getMonth() + 1).padStart(2, '0');
    const dia = String(ahora.getDate()).padStart(2, '0');
    const horas = String(ahora.getHours()).padStart(2, '0');
    const minutos = String(ahora.getMinutes()).padStart(2, '0');
    return `${ahora.getFullYear()}-${mes}-${dia}T${horas}:${minutos}`;
  }

  /** El numero de factura lo asigna el servidor: el cliente no lo inventa. */
  get numeroPendiente(): string {
    return 'Se asigna al emitir (servidor)';
  }

  abrirNuevaFactura() {
    this.nuevaFactura = this.facturaVacia();
    this.tipoFacturacion = 'automatica';
    this.serviciosPaciente = [];
    this.mascotasFiltradas = [];
    this.lineas = [];
    this.productoCatalogo = '';
    this.cantidadCatalogo = 1;
    this.conceptoLibre = '';
    this.cantidadLibre = 1;
    this.precioLibre = 0;
    this.simulacion = undefined;
    this.errorCupon = '';

    this.api.clientes().subscribe({
      next: r => this.clientes = r.data,
      error: () => this.clientes = []
    });
    this.api.mascotas().subscribe({
      next: r => this.mascotas = r.data,
      error: () => this.mascotas = []
    });
    this.api.consultas().subscribe({
      next: r => this.todasConsultas = r.data,
      error: () => this.todasConsultas = []
    });
    this.api.cirugias().subscribe({
      next: r => this.todasCirugias = r.data,
      error: () => this.todasCirugias = []
    });
    this.api.laboratorio().subscribe({
      next: r => this.todosLabs = r.data,
      error: () => this.todosLabs = []
    });
    this.api.hospitalizaciones().subscribe({
      next: r => this.todasHosp = r.data,
      error: () => this.todasHosp = []
    });
    this.api.productos().subscribe({
      next: r => this.productos = r.data || [],
      error: () => this.productos = []
    });

    this.mostrarFormFactura = true;
  }

  cerrarNuevaFactura() {
    this.mostrarFormFactura = false;
  }

  cambiarTipoFacturacion(tipo: 'automatica' | 'directa') {
    this.tipoFacturacion = tipo;
    this.simulacion = undefined;
    this.errorCupon = '';
  }

  onClienteChange() {
    this.nuevaFactura.mascotaId = null;
    this.serviciosPaciente = [];
    this.mascotasFiltradas = this.nuevaFactura.clienteId
      ? this.mascotas.filter(m => m.propietario?.id === this.nuevaFactura.clienteId)
      : [];
    this.actualizarConceptoServicios();
  }

  onMascotaChange() {
    this.serviciosPaciente = [];
    if (!this.nuevaFactura.mascotaId) {
      this.actualizarConceptoServicios();
      return;
    }
    const mascotaId = this.nuevaFactura.mascotaId;
    const petObj = this.mascotas.find(m => m.id === mascotaId);
    if (!petObj) return;

    if (petObj.historiales) {
      petObj.historiales.forEach((h: any) => {
        if (!h.facturaId) {
          this.serviciosPaciente.push({
            tipo: 'Consulta',
            id: h.id,
            descripcion: `${h.tipoEvento || 'Consulta'} - ${h.motivo}`,
            fecha: h.fecha,
            precio: h.precio || 45.00,
            seleccionado: false
          });
        }
      });
    }

    if (petObj.cirugias) {
      petObj.cirugias.forEach((c: any) => {
        if (!c.facturaId) {
          this.serviciosPaciente.push({
            tipo: 'Cirugía',
            id: c.id,
            descripcion: c.procedimiento,
            fecha: c.fechaProgramada,
            precio: c.precio || 280.00,
            seleccionado: false
          });
        }
      });
    }

    const labOrders = this.todosLabs.filter(o => o.mascota?.id === mascotaId);
    labOrders.forEach((l: any) => {
      if (!l.facturaId) {
        this.serviciosPaciente.push({
          tipo: 'Laboratorio',
          id: l.id,
          descripcion: l.tipoPrueba,
          fecha: l.fecha,
          precio: l.precio || 35.00,
          seleccionado: false
        });
      }
    });

    const hospOrders = this.todasHosp.filter(h => h.mascota?.id === mascotaId);
    hospOrders.forEach((hp: any) => {
      if (!hp.facturaId) {
        this.serviciosPaciente.push({
          tipo: 'Hospitalización',
          id: hp.id,
          descripcion: hp.motivo,
          fecha: hp.ingreso,
          precio: hp.precio || 45.00,
          seleccionado: false
        });
      }
    });

    this.actualizarConceptoServicios();
  }

  /** El concepto automatico refleja los servicios seleccionados. */
  actualizarConceptoServicios() {
    if (this.tipoFacturacion !== 'automatica') return;
    const seleccionados = this.serviciosPaciente.filter(s => s.seleccionado);
    if (seleccionados.length > 0) {
      this.nuevaFactura.concepto = seleccionados.map(s => `${s.tipo}: ${s.descripcion}`).join(' + ');
    }
    this.simulacion = undefined;
  }

  /** Servicios clinicos marcados para facturar. */
  get serviciosSeleccionados(): any[] {
    return this.serviciosPaciente.filter(s => s.seleccionado);
  }

  /** Productos del catalogo que se pueden facturar (con precio y stock). */
  get productosVendibles(): Producto[] {
    return this.productos.filter(p => Number(p.precio) > 0 && Number(p.stock) > 0);
  }

  /** Agrega un producto de farmacia a la factura (el precio lo pone el servidor). */
  agregarProducto() {
    const producto = this.productos.find(p => p.codigo === this.productoCatalogo);
    if (!producto) {
      this.notificaciones.mostrarError('Selecciona un producto del catalogo.');
      return;
    }
    const cantidad = Math.floor(Number(this.cantidadCatalogo || 0));
    if (cantidad <= 0) {
      this.notificaciones.mostrarError('La cantidad debe ser mayor que cero.');
      return;
    }
    if (cantidad > Number(producto.stock)) {
      this.notificaciones.mostrarError(`Stock insuficiente de ${producto.codigo}: disponibles ${producto.stock}.`);
      return;
    }
    this.lineas.push({
      descripcion: producto.nombre,
      cantidad,
      precioUnitario: Number(producto.precio),
      productoCodigo: producto.codigo,
      categoria: producto.categoria
    });
    this.productoCatalogo = '';
    this.cantidadCatalogo = 1;
    this.simulacion = undefined;
  }

  /** Agrega un concepto libre (sin producto de catalogo). */
  agregarConceptoLibre() {
    const descripcion = this.conceptoLibre.trim();
    const cantidad = Math.floor(Number(this.cantidadLibre || 0));
    const precio = Number(this.precioLibre || 0);
    if (!descripcion) {
      this.notificaciones.mostrarError('Escribe la descripcion del concepto.');
      return;
    }
    if (cantidad <= 0) {
      this.notificaciones.mostrarError('La cantidad debe ser mayor que cero.');
      return;
    }
    if (precio <= 0) {
      this.notificaciones.mostrarError('El precio unitario debe ser mayor que cero.');
      return;
    }
    this.lineas.push({
      descripcion,
      cantidad,
      precioUnitario: precio,
      categoria: 'Concepto'
    });
    this.conceptoLibre = '';
    this.cantidadLibre = 1;
    this.precioLibre = 0;
    this.simulacion = undefined;
  }

  quitarLinea(linea: LineaFactura) {
    const indice = this.lineas.indexOf(linea);
    if (indice >= 0) this.lineas.splice(indice, 1);
    this.simulacion = undefined;
  }

  /** Lineas que se envian al servidor (servicios automaticos o venta directa). */
  private lineasFactura(): LineaFactura[] {
    if (this.tipoFacturacion === 'automatica') {
      return this.serviciosSeleccionados.map(s => ({
        descripcion: s.descripcion,
        cantidad: 1,
        precioUnitario: Number(s.precio || 0),
        categoria: 'Servicio'
      }));
    }
    return this.lineas.map(l => ({ ...l }));
  }

  /** Subtotal estimado (solo vista previa: el importe real lo calcula el servidor). */
  get subtotalEstimado(): number {
    return this.lineasFactura().reduce((suma, l) => suma + Number(l.precioUnitario || 0) * Number(l.cantidad || 0), 0);
  }

  // ------------------------------------------------------------------
  // Cupon de descuento
  // ------------------------------------------------------------------

  /** Limpia la simulacion cuando el usuario edita el codigo del cupon. */
  limpiarCupon() {
    this.simulacion = undefined;
    this.errorCupon = '';
  }

  /**
   * Pide al servidor la simulacion del cupon. Usa el mismo calculo que la
   * factura, por lo que el importe mostrado es el que se cobra.
   */
  aplicarCupon() {
    const codigo = this.nuevaFactura.cuponCodigo.trim();
    if (!codigo) {
      this.simulacion = undefined;
      this.errorCupon = '';
      return;
    }
    if (this.lineasFactura().length === 0) {
      this.simulacion = undefined;
      this.errorCupon = 'Agrega al menos una linea a la factura antes de validar el cupon.';
      this.notificaciones.mostrarError(this.errorCupon);
      return;
    }
    this.simulando = true;
    this.errorCupon = '';
    this.api.validarCupon({
      codigo,
      concepto: this.nuevaFactura.concepto.trim() || undefined,
      subtotal: Number(this.subtotalEstimado.toFixed(2)),
      items: this.lineasFactura()
    }).pipe(finalize(() => this.simulando = false)).subscribe({
      next: r => {
        if (r.data?.aplicado) {
          this.simulacion = r.data;
          this.notificaciones.mostrarExito(`Cupon ${r.data.codigo} aplicado: -$ ${Number(r.data.descuento).toFixed(2)}`);
        } else {
          this.simulacion = undefined;
          this.errorCupon = 'El cupon no genera descuento sobre esta factura.';
          this.nuevaFactura.cuponCodigo = '';
          this.notificaciones.mostrarError(this.errorCupon);
        }
      },
      error: (e: HttpErrorResponse) => {
        this.simulacion = undefined;
        this.errorCupon = e.error?.mensaje || 'No se pudo validar el cupon.';
        this.nuevaFactura.cuponCodigo = '';
        this.notificaciones.mostrarError(this.errorCupon);
      }
    });
  }

  // ------------------------------------------------------------------
  // Emision
  // ------------------------------------------------------------------

  guardarNuevaFactura() {
    const concepto = this.nuevaFactura.concepto.trim();
    if (!concepto) {
      this.notificaciones.mostrarError('El concepto de la factura es obligatorio.');
      return;
    }
    if (concepto.length > 500) {
      this.notificaciones.mostrarError('El concepto no puede superar los 500 caracteres.');
      return;
    }

    const seleccionados = this.serviciosSeleccionados;
    if (this.tipoFacturacion === 'automatica') {
      if (!this.nuevaFactura.clienteId || !this.nuevaFactura.mascotaId) {
        this.notificaciones.mostrarError('Por favor, selecciona un cliente y una mascota.');
        return;
      }
      if (seleccionados.length === 0) {
        this.notificaciones.mostrarError('Selecciona al menos un servicio clinico por facturar.');
        return;
      }
    } else if (this.lineas.length === 0) {
      this.notificaciones.mostrarError('Agrega al menos una linea a la factura.');
      return;
    }

    if (this.nuevaFactura.cuponCodigo.trim() && !this.simulacion?.aplicado) {
      this.notificaciones.mostrarError('Valida el cupon antes de emitir la factura.');
      return;
    }

    // El backend recalcula subtotal, IVA, descuento y total: no se envian.
    const payload: any = {
      fecha: this.fechaIsoLocal(this.nuevaFactura.fecha),
      concepto,
      estadoPago: this.nuevaFactura.estadoPago,
      metodoPago: this.nuevaFactura.estadoPago === 'Pagado' ? this.nuevaFactura.metodoPago : null,
      cliente: this.nuevaFactura.clienteId ? { id: this.nuevaFactura.clienteId } : null,
      mascota: this.nuevaFactura.mascotaId ? { id: this.nuevaFactura.mascotaId } : null,
      items: this.lineasFactura(),
      cuponCodigo: this.simulacion?.aplicado ? (this.simulacion.codigo || this.nuevaFactura.cuponCodigo.trim()) : null
    };

    if (this.tipoFacturacion === 'automatica') {
      payload.consultaIds = seleccionados.filter(s => s.tipo === 'Consulta').map(s => s.id);
      payload.cirugiaIds = seleccionados.filter(s => s.tipo === 'Cirugía').map(s => s.id);
      payload.laboratorioIds = seleccionados.filter(s => s.tipo === 'Laboratorio').map(s => s.id);
      payload.hospitalizacionIds = seleccionados.filter(s => s.tipo === 'Hospitalización').map(s => s.id);
    }

    this.emitiendo = true;
    this.api.crearFactura(payload).pipe(finalize(() => this.emitiendo = false)).subscribe({
      next: r => {
        const factura = this.normalizarFactura(r?.data);
        this.ultimaFactura = factura;
        this.facturaSeleccionada = factura;
        this.notificaciones.mostrarExito(`Factura ${factura.numero} emitida por $ ${factura.total.toFixed(2)}.`);
        this.mostrarFormFactura = false;
        this.cargarFacturas();
      },
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo crear la factura.')
    });
  }

  /** Normaliza la fecha local del formulario al formato ISO que espera el backend. */
  private fechaIsoLocal(valor: string): string {
    if (!valor) return this.fechaLocalActual();
    return valor.length === 16 ? `${valor}:00` : valor;
  }

  // ------------------------------------------------------------------
  // Estado de pago
  // ------------------------------------------------------------------

  abrirPago(factura: Factura) {
    this.facturaPago = factura;
    this.estadoPago = factura.estadoPago || 'Pendiente';
    this.metodoPago = factura.metodoPago || 'Efectivo';
    this.mensajePago = '';
  }

  cerrarPago() {
    this.facturaPago = undefined;
  }

  guardarPago() {
    if (!this.facturaPago) return;
    this.api.actualizarEstadoFactura(this.facturaPago.id, this.estadoPago, this.estadoPago === 'Pagado' ? this.metodoPago : '').subscribe({
      next: r => {
        const actualizada: Factura = {
          ...this.facturaPago!,
          estadoPago: r.data?.estadoPago || this.estadoPago,
          metodoPago: r.data?.metodoPago || ''
        };
        this.facturas = this.facturas.map(f => f.id === actualizada.id ? actualizada : f);
        if (this.facturaSeleccionada?.id === actualizada.id) this.facturaSeleccionada = actualizada;
        if (this.ultimaFactura?.id === actualizada.id) this.ultimaFactura = actualizada;
        this.mensajePago = 'Estado de pago actualizado.';
        this.notificaciones.mostrarExito('Estado de pago actualizado.');
        this.cerrarPago();
      },
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo actualizar el estado de pago.')
    });
  }

  // ------------------------------------------------------------------
  // Descargas
  // ------------------------------------------------------------------

  /** Descarga el PDF generado por el backend (no disponible para el rol CLIENTE). */
  descargarPdf(factura: Factura) {
    if (this.esCliente) {
      this.notificaciones.mostrarError('Tu rol no tiene permiso para descargar el PDF de la factura.');
      return;
    }
    if (!factura?.id) {
      this.notificaciones.mostrarError('La factura aun no tiene numero de servidor.');
      return;
    }
    this.api.descargarFacturaPdf(factura.id, factura.numero);
  }

  descargarComprobante(factura: Factura) {
    const contenido = this.crearComprobanteHtml(factura);
    const blob = new Blob([contenido], { type: 'text/html;charset=utf-8' });
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(blob);
    enlace.download = `comprobante-${factura.numero}.html`;
    enlace.click();
    URL.revokeObjectURL(enlace.href);
  }

  imprimirComprobante(factura: Factura) {
    const ventana = window.open('', '_blank', 'width=920,height=720');
    if (!ventana) return;
    ventana.document.write(this.crearComprobanteHtml(factura, true));
    ventana.document.close();
  }

  private crearComprobanteHtml(factura: Factura, imprimir = false) {
    const fechaFactura = this.formatearFecha(factura.fecha);
    // Todos los importes provienen del servidor.
    const subtotal = this.subtotalDe(factura);
    const descuento = this.descuentoDe(factura);
    const iva = this.impuestosDe(factura);
    const total = Number(factura.total || 0);
    const estado = factura.estadoPago === 'Pagado' ? 'PAGO CONFIRMADO' : (factura.estadoPago === 'Anulado' ? 'FACTURA ANULADA' : 'PENDIENTE DE PAGO');
    const imprimirScript = imprimir ? '<script>window.onload = () => setTimeout(() => window.print(), 350);</script>' : '';
    const metodoPagoDiv = factura.metodoPago ? `<div class="box"><div class="label">Método de Pago</div><div class="value">${this.escapar(factura.metodoPago)}</div></div>` : '';
    const cuponDiv = factura.cuponCodigo ? `<div class="box"><div class="label">Cupón</div><div class="value">${this.escapar(factura.cuponCodigo)}</div></div>` : '';
    const descuentoLinea = descuento > 0
      ? `<div class="line"><span>Descuento${factura.cuponCodigo ? ` (${this.escapar(factura.cuponCodigo)})` : ''}</span><strong>-$${descuento.toFixed(2)}</strong></div>`
      : '';

    return `<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <title>Comprobante ${this.escapar(factura.numero)}</title>
  <style>
    * { box-sizing: border-box; }
    body { margin: 0; background: #eef5f8; color: #0f172a; font-family: Arial, Helvetica, sans-serif; }
    .page { max-width: 860px; margin: 34px auto; padding: 24px; }
    .receipt { overflow: hidden; border: 1px solid #d9e7ee; border-radius: 18px; background: #fff; box-shadow: 0 24px 70px rgba(15, 23, 42, .14); }
    .hero { display: flex; justify-content: space-between; gap: 24px; padding: 30px; color: white; background: linear-gradient(135deg, #075985, #0891b2 58%, #14b8a6); }
    .brand { display: flex; gap: 14px; align-items: center; }
    .logo { display: grid; width: 58px; height: 58px; place-items: center; border-radius: 16px; background: #fff; overflow: hidden; padding: 5px; }
    .logo img { width: 100%; height: 100%; object-fit: contain; }
    .brand h1 { margin: 0; font-size: 26px; line-height: 1; }
    .brand p, .hero-meta p { margin: 5px 0 0; opacity: .88; font-size: 13px; }
    .hero-meta { text-align: right; }
    .hero-meta strong { display: block; font-size: 18px; margin-top: 4px; }
    .status { display: inline-flex; margin-top: 12px; padding: 8px 12px; border-radius: 999px; background: rgba(255,255,255,.2); font-size: 12px; font-weight: 900; }
    .content { padding: 30px; }
    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
    .box { border: 1px solid #e2edf3; border-radius: 14px; padding: 18px; background: #f8fbfd; }
    .label { color: #64748b; font-size: 11px; font-weight: 900; letter-spacing: .06em; text-transform: uppercase; }
    .value { margin-top: 7px; color: #0f172a; font-size: 16px; font-weight: 900; }
    table { width: 100%; margin-top: 24px; border-collapse: collapse; overflow: hidden; border-radius: 14px; }
    th { padding: 14px; background: #0f172a; color: #e0f2fe; text-align: left; font-size: 11px; text-transform: uppercase; letter-spacing: .06em; }
    td { padding: 16px 14px; border-bottom: 1px solid #e2e8f0; color: #334155; }
    .amount { text-align: right; font-weight: 900; color: #0f766e; }
    .totals { display: flex; justify-content: flex-end; margin-top: 22px; }
    .totals-card { width: 320px; border: 1px solid #d9e7ee; border-radius: 16px; padding: 18px; background: #f8fbfd; }
    .line { display: flex; justify-content: space-between; padding: 8px 0; color: #475569; }
    .line.total { margin-top: 8px; border-top: 1px solid #cbd5e1; color: #0f172a; font-size: 22px; font-weight: 900; }
    .footer { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; padding: 0 30px 30px; }
    .note { border-radius: 14px; background: #ecfeff; padding: 16px; color: #155e75; font-size: 13px; line-height: 1.55; }
    .signature { display: flex; align-items: flex-end; justify-content: center; border-bottom: 1px solid #94a3b8; color: #64748b; font-size: 12px; min-height: 74px; }
    @media print { body { background: white; } .page { margin: 0; max-width: none; } .receipt { box-shadow: none; } }
  </style>
</head>
<body>
  <main class="page">
    <section class="receipt">
      <header class="hero">
        <div class="brand">
          <div class="logo"><img src="${window.location.origin}/assets/logo-vetsphere-icon.png" alt="Logo VetSphere"></div>
          <div>
            <h1>VetSphere</h1>
            <p>Clinica veterinaria integral</p>
            <p>Av. Cristóbal Colón E10-45 y Av. 12 de Octubre, Quito (La Floresta)</p>
          </div>
        </div>
        <div class="hero-meta">
          <p>Comprobante de pago</p>
          <strong>${this.escapar(factura.numero)}</strong>
          <span class="status">${estado}</span>
        </div>
      </header>

      <div class="content">
        <div class="grid">
          <div class="box"><div class="label">Cliente</div><div class="value">${this.escapar(factura.cliente)}</div></div>
          <div class="box"><div class="label">Fecha</div><div class="value">${fechaFactura}</div></div>
          <div class="box"><div class="label">Paciente</div><div class="value">${this.escapar(factura.mascota)}</div></div>
          <div class="box"><div class="label">Estado</div><div class="value">${this.escapar(factura.estadoPago)}</div></div>
          ${metodoPagoDiv}
          ${cuponDiv}
        </div>

        <table>
          <thead><tr><th>Detalle</th><th>Paciente</th><th class="amount">Valor</th></tr></thead>
          <tbody>
            <tr>
              <td>${this.escapar(factura.concepto)}</td>
              <td>${this.escapar(factura.mascota)}</td>
              <td class="amount">$${subtotal.toFixed(2)}</td>
            </tr>
          </tbody>
        </table>

        <div class="totals">
          <div class="totals-card">
            <div class="line"><span>Subtotal</span><strong>$${subtotal.toFixed(2)}</strong></div>
            ${descuentoLinea}
            <div class="line"><span>IVA (15%)</span><strong>$${iva.toFixed(2)}</strong></div>
            <div class="line total"><span>Total</span><strong>$${total.toFixed(2)}</strong></div>
          </div>
        </div>
      </div>

      <footer class="footer">
        <div class="note">Gracias por confiar en VetSphere. Conserva este comprobante como respaldo del servicio clinico registrado en el historial de tu mascota.</div>
        <div class="signature">Firma autorizada VetSphere</div>
      </footer>
    </section>
  </main>
  ${imprimirScript}
</body>
</html>`;
  }

  private formatearFecha(fecha?: string) {
    if (!fecha) return this.fechaEmision.toLocaleDateString('es-EC');
    return new Date(fecha).toLocaleDateString('es-EC', { year: 'numeric', month: 'long', day: '2-digit' });
  }

  private escapar(valor?: string | number) {
    return String(valor ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
}
