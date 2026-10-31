export interface ApiResponse<T> { ok: boolean; mensaje: string; data: T; }

export interface ChatMensajeVista { rol: 'user' | 'assistant'; contenido: string; fecha: string; }
export interface ChatConversacionDetalle {
  id: number; usuarioEmail: string; nombreUsuario: string; titulo: string; actualizadoEn: string;
  mensajes: ChatMensajeVista[];
}
export interface ChatConversacionResumen {
  id: number; usuarioEmail: string; nombreUsuario: string; titulo: string; actualizadoEn: string; cantidadMensajes: number;
}

export interface EtapaFlujoRecepcion {
  id: number;
  nombre: string;
  estadoCita: string;
  descripcion: string | null;
  color: 'cyan' | 'emerald' | 'amber' | 'rose' | 'violet' | 'blue' | 'slate';
  posicion: number;
  activa: boolean;
}
export interface Rol { id: number; nombre: string; }
export interface Usuario {
  id: number; nombres: string; apellidos: string; email: string; telefono: string;
  fotoUrl?: string; especialidad?: string; cedulaProfesional?: string; biografia?: string; roles: Rol[];
  activo?: boolean;
  /** Estado de verificacion del correo. */
  emailVerificado?: boolean;
  /** Bloqueo automatico o manual vigente. */
  bloqueadoHasta?: string | null;
  bloqueadoAhora?: boolean;
  motivoBloqueo?: string | null;
  /** Intentos de acceso fallidos acumulados. */
  intentosFallidos?: number;
  /** Fecha de la ultima desactivacion de la cuenta. */
  desactivadoEn?: string | null;
  ultimoAcceso?: string | null;
}
export interface Mascota {
  id: number; nombre: string; especie: string; raza: string; sexo: string; color: string; pesoKg: number;
  fechaNacimiento: string; fotoUrl?: string; estado: string;
  /** El propietario puede venir vacio en fichas de baja o historico importado. */
  propietario: Usuario | null;
  historiales: HistorialClinico[] | null; cirugias: Cirugia[] | null;
}
export interface HistorialClinico {
  id: number; fecha: string; tipoEvento: string; motivo: string; diagnostico: string; tratamiento: string;
  vacunas?: string; archivosUrl?: string; temperatura: number; frecuenciaCardiaca: number; frecuenciaRespiratoria: number;
  precio?: number; facturaId?: number;
}
export interface Cirugia {
  id: number; fechaProgramada: string; procedimiento: string; tipoAnestesia: string; cirujanoPrincipal?: string; estadoPostoperatorio: string; estado: string; constantesVitales: string; notas?: string; mascota?: Mascota; cirujano?: Usuario; precio?: number; facturaId?: number;
}
export interface Producto { id?: number; codigo: string; nombre: string; categoria: string; stock: number; stockCritico: number; precio: number; prioridadAlta: boolean; descuento?: number | null; activo?: boolean; lote?: string; fechaVencimiento?: string | null; }
export interface Dashboard {
  totalClientes: number;
  totalMascotas: number;
  mascotasInternadas: number;
  citasHoy: number;
  citasMes: number;
  cirugiasHoy: number;
  totalConsultas: number;
  totalCirugias: number;
  totalVacunas: number;
  totalLaboratorio: number;
  totalProductos: number;
  totalEmergencias: number;
  citasUltimos7Dias: number[];
  totalPerros: number;
  totalGatos: number;
  cirugiasRecientes: { procedimiento: string; mascotaNombre: string; estado: string }[];
  stockCritico: Producto[];
}
export interface Cliente extends Usuario { ciudad?: string; documento?: string; mascotas?: Mascota[]; totalGastado?: number; estado?: string; fechaRegistro?: string; }
export interface ConsultaMedica {
  id: number; mascota: string; cliente: string; veterinario: string; tipo: string; fecha: string;
  diagnostico: string; tratamiento: string; pesoKg: number; temperatura: number; precio: number; facturaId?: number;
}
export interface Factura {
  id: number; numero: string; cliente: string; mascota: string; concepto: string; total: number; estadoPago: string; fecha: string;
  metodoPago?: string;
  /** Base imponible (sin impuestos). */
  subtotal?: number;
  /** IVA aplicado por el servidor (15%). */
  impuestos?: number;
  /** Suma de los descuentos aplicados. */
  descuento?: number;
  /** Codigo del cupon utilizado, si hubo. */
  cuponCodigo?: string;
  observaciones?: string;
}

/** Linea persistida de una factura (productos y servicios facturados). */
export interface FacturaDetalle {
  id: number; facturaId?: number; productoId?: number | null; codigo?: string;
  descripcion: string; cantidad: number; precioUnitario: number; descuento: number; subtotal: number;
}

/** Cupon de descuento de la farmacia. */
export interface Cupon {
  id: number;
  codigo: string;
  descripcion?: string;
  /** PORCENTAJE o VALOR_FIJO. */
  tipoDescuento: string;
  valor: number;
  /** Producto objetivo (null = cupon por categoria o global). */
  producto?: { id: number; codigo: string; nombre: string } | null;
  productoId?: number | null;
  productoNombre?: string | null;
  categoria?: string | null;
  minimoSubtotal?: number | null;
  fechaInicio: string;
  fechaFin: string;
  usosMaximos?: number | null;
  usosActuales: number;
  activo: boolean;
  /** MANUAL o BAJA_ROTACION. */
  origen: string;
  creadoPor?: string | null;
  fechaCreacion: string;
}

/** Estado de rotacion de un producto de la farmacia. */
export type EstadoRotacion = 'SIN_ROTACION' | 'BAJA_ROTACION' | 'ROTACION_SALUDABLE';

/** Fila del analisis de rotacion de inventario. */
export interface RotacionProducto {
  id: number; codigo: string; nombre: string; categoria: string;
  stock: number; stockCritico?: number; precio: number;
  /** Descuento permanente del producto (porcentaje). */
  descuentoProducto: number;
  /** Unidades vendidas en la ventana analizada. */
  unidadesVendidas: number;
  /** Unidades vendidas historicamente. */
  unidadesTotales: number;
  /** Ingresos generados en la ventana analizada. */
  ingresos: number;
  ultimaVenta?: string | null;
  /** Dias transcurridos desde la ultima venta (null = nunca vendio). */
  diasSinVenta: number | null;
  /** Dias de cobertura del stock al ritmo de venta actual (-1 = sin ventas en la ventana). */
  diasCobertura: number | null;
  /** Valor del stock inmovilizado. */
  valorInventario: number;
  estado: EstadoRotacion;
  /** Descuento recomendado para impulsarlo (porcentaje). */
  descuentoSugerido: number;
  /** true si ya tiene una promocion vigente. */
  cuponActivo: boolean;
}

/** Resumen del analisis de rotacion. */
export interface ResumenRotacion {
  diasVentana: number; diasUmbral: number;
  totalProductos: number;
  productosSinRotacion: number;
  productosBajaRotacion: number;
  productosSaludables: number;
  unidadesVendidas: number;
  ingresos: number;
  capitalInventario: number;
  capitalDormido: number;
}

/** Respuesta completa del analisis de rotacion. */
export interface AnalisisRotacion {
  resumen: ResumenRotacion;
  productos: RotacionProducto[];
}

/** Resultado de simular un cupon sobre un carrito de venta. */
export interface SimulacionCupon {
  codigo: string | null;
  aplicado: boolean;
  bruto: number;
  descuento: number;
  subtotal: number;
  impuestos: number;
  total: number;
}
export interface OrdenLaboratorio {
  id: number; fecha: string; tipoPrueba: string; resultado: string; estado: string; archivoResultadoUrl?: string; mascota?: Mascota; veterinarioSolicitante?: Usuario; precio?: number; facturaId?: number;
}
export interface Hospitalizacion {
  id: number; ingreso: string; alta?: string; jaula: string; motivo: string; planCuidados: string; estado: string; mascota?: Mascota; veterinarioResponsable?: Usuario; precio?: number; facturaId?: number;
}
