import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  AnalisisRotacion, ApiResponse, ChatConversacionDetalle, ChatConversacionResumen, Cirugia, Cliente, ConsultaMedica, Cupon, Dashboard, Factura,
  EtapaFlujoRecepcion, Hospitalizacion, Mascota, OrdenLaboratorio, Producto, SimulacionCupon, Usuario
} from './modelos';
import { environment } from '../../environments/environment';
import { NotificacionService } from './notificacion.service';
import { timeout } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private api = environment.apiUrl;

  constructor(private http: HttpClient, private notificaciones: NotificacionService) {}

  login(datos: { email: string; password: string }) {
    return this.http.post<ApiResponse<any>>(`${this.api}/auth/login`, datos);
  }
  verificarAutenticador(datos: { email: string; password: string; codigo: string }) { return this.http.post<ApiResponse<any>>(`${this.api}/auth/verificar-autenticador`, datos); }
  solicitarCodigoSegundoFactor(datos: { email: string; password: string }) { return this.http.post<ApiResponse<void>>(`${this.api}/auth/solicitar-codigo-segundo-factor`, datos); }
  verificarSegundoFactor(datos: { email: string; password: string; metodo: string; codigo: string }) { return this.http.post<ApiResponse<any>>(`${this.api}/auth/verificar-segundo-factor`, datos); }
  codigoRegistroPersonal() { return this.http.get<ApiResponse<{codigo: string; segundosRestantes: number}>>(`${this.api}/administracion/codigo-registro`); }
  configurarAutenticador() { return this.http.post<ApiResponse<{secreto: string; qrDataUri: string}>>(`${this.api}/cuenta/autenticador/configurar`, {}); }
  activarAutenticador(codigo: string) { return this.http.post<ApiResponse<void>>(`${this.api}/cuenta/autenticador/activar`, { codigo }); }
  desactivarAutenticador(datos: {password: string; codigo: string}) { return this.http.post<ApiResponse<void>>(`${this.api}/cuenta/autenticador/desactivar`, datos); }
  configurarSegundoFactorCorreo(password: string, activo: boolean) { return this.http.post<ApiResponse<void>>(`${this.api}/cuenta/segundo-factor/correo?activo=${activo}`, { password }); }
  generarCodigosRespaldo(password: string) { return this.http.post<ApiResponse<string[]>>(`${this.api}/cuenta/segundo-factor/codigos-respaldo`, { password }); }
  registrar(datos: any) { return this.http.post<ApiResponse<Usuario>>(`${this.api}/auth/registro`, datos); }
  verificar(datos: { email: string; codigo: string }) { return this.http.post<ApiResponse<any>>(`${this.api}/auth/verificar-2fa`, datos); }
  reenviarCodigo(email: string) { return this.http.post<ApiResponse<void>>(`${this.api}/auth/reenviar-codigo`, { email }); }
  solicitarRecuperacion(email: string) { return this.http.post<ApiResponse<void>>(`${this.api}/auth/solicitar-recuperacion`, { email }); }
  restablecerPassword(datos: { email: string; codigo: string; nuevaPassword: string }) { return this.http.post<ApiResponse<void>>(`${this.api}/auth/restablecer-password`, datos); }
  dashboard() { return this.http.get<ApiResponse<Dashboard>>(`${this.api}/dashboard`); }
  mascotas() { return this.http.get<ApiResponse<Mascota[]>>(`${this.api}/mascotas`); }
  crearMascota(datos: any, foto?: File | null) {
    const formData = new FormData();
    formData.append('mascota', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
    if (foto) formData.append('foto', foto);
    return this.http.post<ApiResponse<Mascota>>(`${this.api}/mascotas`, formData);
  }
  actualizarMascota(id: number, datos: any) {
    return this.http.put<ApiResponse<Mascota>>(`${this.api}/mascotas/${id}`, datos);
  }
  agregarHistorialMascota(id: number, datos: any, archivo?: File | null) {
    const formData = new FormData();
    formData.append('historial', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
    if (archivo) formData.append('archivo', archivo);
    return this.http.post<ApiResponse<any>>(`${this.api}/mascotas/${id}/historial`, formData);
  }
  eliminarArchivoMascota(mascotaId: number, historialId: number) {
    return this.http.delete<ApiResponse<void>>(`${this.api}/mascotas/${mascotaId}/historial/${historialId}/archivo`);
  }
  eliminarMascota(id: number) {
    return this.http.delete<ApiResponse<void>>(`${this.api}/mascotas/${id}`);
  }
  obtenerArchivo(url: string) {
    const destino = new URL(url, `${this.api}/`);
    const origenApi = new URL(this.api).origin;
    if (destino.origin !== origenApi || !destino.pathname.startsWith('/uploads/')) {
      throw new Error('La ruta del archivo no pertenece al almacenamiento de VetSphere.');
    }
    return this.http.get(destino.toString(), { responseType: 'blob' });
  }
  veterinarios() { return this.http.get<ApiResponse<Usuario[]>>(`${this.api}/veterinarios/publico`); }
  clientes() { return this.http.get<ApiResponse<Cliente[]>>(`${this.api}/clientes`); }
  crearCliente(datos: any) { return this.http.post<ApiResponse<Cliente>>(`${this.api}/clientes`, datos); }
  actualizarCliente(id: number, datos: any) { return this.http.put<ApiResponse<Cliente>>(`${this.api}/clientes/${id}`, datos); }
  consultas() { return this.http.get<ApiResponse<ConsultaMedica[]>>(`${this.api}/consultas`); }
  crearConsulta(datos: any) { return this.http.post<ApiResponse<any>>(`${this.api}/consultas`, datos); }
  actualizarConsulta(id: number, datos: any) { return this.http.put<ApiResponse<any>>(`${this.api}/consultas/${id}`, datos); }
  eliminarConsulta(id: number) { return this.http.delete<ApiResponse<void>>(`${this.api}/consultas/${id}`); }
  facturas() { return this.http.get<ApiResponse<Factura[]>>(`${this.api}/facturacion`); }
  crearFactura(datos: any) { return this.http.post<ApiResponse<Factura>>(`${this.api}/facturacion`, datos); }
  actualizarEstadoFactura(id: number, estadoPago: string, metodoPago?: string) { return this.http.put<ApiResponse<any>>(`${this.api}/facturacion/${id}/estado`, { estadoPago, metodoPago }); }
  productosCriticos() { return this.http.get<ApiResponse<Producto[]>>(`${this.api}/farmacia/criticos`); }
  productos() { return this.http.get<ApiResponse<Producto[]>>(`${this.api}/farmacia/inventario`); }
  crearProducto(datos: Producto) { return this.http.post<ApiResponse<Producto>>(`${this.api}/farmacia/inventario`, datos); }
  actualizarProducto(codigo: string, datos: Producto) { return this.http.put<ApiResponse<Producto>>(`${this.api}/farmacia/inventario/${codigo}`, datos); }
  actualizarStock(codigo: string, stock: number) { return this.http.put<ApiResponse<Producto>>(`${this.api}/farmacia/inventario/${codigo}/stock`, { stock }); }
  agregarStock(codigo: string, cantidad: number) { return this.http.post<ApiResponse<Producto>>(`${this.api}/farmacia/inventario/${codigo}/movimiento`, { cantidad }); }
  descontarStock(codigo: string, cantidad: number) { return this.http.post<ApiResponse<Producto>>(`${this.api}/farmacia/inventario/${codigo}/descontar`, { cantidad }); }
  soapInventario() { return this.http.get<ApiResponse<any[]>>(`${this.api}/integraciones/inventario`); }
  soapProducto(codigo: string) { return this.http.get<ApiResponse<any>>(`${this.api}/integraciones/inventario/${encodeURIComponent(codigo)}`); }
  soapAgregarStock(codigo: string, cantidad: number) { return this.http.post<ApiResponse<any>>(`${this.api}/integraciones/inventario/${encodeURIComponent(codigo)}/movimiento`, { cantidad }); }
  soapDescontarStock(codigo: string, cantidad: number) { return this.http.post<ApiResponse<any>>(`${this.api}/integraciones/inventario/${encodeURIComponent(codigo)}/descontar`, { cantidad }); }
  soapActualizarStock(codigo: string, stock: number) { return this.http.put<ApiResponse<any>>(`${this.api}/integraciones/inventario/${encodeURIComponent(codigo)}/stock`, { stock }); }

  // ------------------------------------------------------------------
  // Farmacia: rotacion de inventario y cupones de descuento
  // ------------------------------------------------------------------

  /**
   * Analiza que productos no se estan vendiendo. El servidor devuelve el
   * resumen (capital dormido, unidades vendidas) y el detalle por producto.
   */
  rotacionInventario(dias?: number, umbral?: number) {
    const params = new URLSearchParams();
    if (dias) params.set('dias', String(dias));
    if (umbral) params.set('umbral', String(umbral));
    const query = params.toString();
    return this.http.get<ApiResponse<AnalisisRotacion>>(
      `${this.api}/farmacia/rotacion${query ? `?${query}` : ''}`
    );
  }

  /** Genera cupones automaticos para los productos de baja rotacion. */
  generarCuponesBajaRotacion(datos: { diasSinVenta?: number; descuentoPorcentaje?: number; duracionDias?: number; descripcion?: string } = {}) {
    return this.http.post<ApiResponse<{ creados: number; omitidos: number; cupones: Cupon[]; detalleOmitidos: { codigo: string; motivo: string }[] }>>(
      `${this.api}/farmacia/cupones/auto`, datos
    );
  }

  cupones(soloVigentes = false) {
    return this.http.get<ApiResponse<Cupon[]>>(`${this.api}/farmacia/cupones${soloVigentes ? '?soloVigentes=true' : ''}`);
  }

  crearCupon(datos: Partial<Cupon>) {
    return this.http.post<ApiResponse<Cupon>>(`${this.api}/farmacia/cupones`, datos);
  }

  actualizarCupon(id: number, datos: Partial<Cupon>) {
    return this.http.put<ApiResponse<Cupon>>(`${this.api}/farmacia/cupones/${id}`, datos);
  }

  estadoCupon(id: number, activo: boolean) {
    return this.http.put<ApiResponse<Cupon>>(`${this.api}/farmacia/cupones/${id}/estado`, { activo });
  }

  eliminarCupon(id: number) {
    return this.http.delete<ApiResponse<void>>(`${this.api}/farmacia/cupones/${id}`);
  }

  /** Simula el descuento de un cupon sobre el carrito (mismo calculo que la factura). */
  validarCupon(datos: { codigo: string; concepto?: string; subtotal?: number; items: { descripcion?: string; cantidad: number; precioUnitario?: number; productoCodigo?: string; categoria?: string }[] }) {
    return this.http.post<ApiResponse<SimulacionCupon>>(`${this.api}/farmacia/cupones/validar`, datos);
  }
  cirugias() { return this.http.get<ApiResponse<Cirugia[]>>(`${this.api}/cirugias`); }
  crearCirugia(datos: any) { return this.http.post<ApiResponse<Cirugia>>(`${this.api}/cirugias`, datos); }
  actualizarCirugia(id: number, datos: any) { return this.http.put<ApiResponse<Cirugia>>(`${this.api}/cirugias/${id}`, datos); }
  eliminarCirugia(id: number) { return this.http.delete<ApiResponse<void>>(`${this.api}/cirugias/${id}`); }
  laboratorio() { return this.http.get<ApiResponse<OrdenLaboratorio[]>>(`${this.api}/laboratorio`); }
  crearOrdenLaboratorio(datos: any) { return this.http.post<ApiResponse<OrdenLaboratorio>>(`${this.api}/laboratorio`, datos); }
  actualizarOrdenLaboratorio(id: number, datos: any) { return this.http.put<ApiResponse<OrdenLaboratorio>>(`${this.api}/laboratorio/${id}`, datos); }
  eliminarOrdenLaboratorio(id: number) { return this.http.delete<ApiResponse<void>>(`${this.api}/laboratorio/${id}`); }
  hospitalizaciones() { return this.http.get<ApiResponse<Hospitalizacion[]>>(`${this.api}/hospitalizaciones`); }
  crearHospitalizacion(datos: any) { return this.http.post<ApiResponse<Hospitalizacion>>(`${this.api}/hospitalizaciones`, datos); }
  actualizarHospitalizacion(id: number, datos: any) { return this.http.put<ApiResponse<Hospitalizacion>>(`${this.api}/hospitalizaciones/${id}`, datos); }
  cuentaHospitalizacion(id: number) { return this.http.get<ApiResponse<any>>(`${this.api}/hospitalizaciones/${id}/cuenta`); }
  agregarConsumoHospitalizacion(id: number, productoCodigo: string, cantidad: number) { return this.http.post<ApiResponse<any>>(`${this.api}/hospitalizaciones/${id}/consumos`, { productoCodigo, cantidad }); }
  altaHospitalizacion(id: number) { return this.http.post<ApiResponse<any>>(`${this.api}/hospitalizaciones/${id}/alta`, {}); }
  eliminarHospitalizacion(id: number) { return this.http.delete<ApiResponse<void>>(`${this.api}/hospitalizaciones/${id}`); }
  datosPago() { return this.http.get<ApiResponse<any>>(`${this.api}/facturacion/datos-pago`); }
  administracion() { return this.http.get<ApiResponse<Record<string, number>>>(`${this.api}/administracion/indicadores`); }
  recepcion() { return this.http.get<ApiResponse<Record<string, number>>>(`${this.api}/recepcion/panel`); }
  flujoRecepcion() { return this.http.get<ApiResponse<EtapaFlujoRecepcion[]>>(`${this.api}/recepcion/flujo`); }
  crearEtapaFlujo(datos: Partial<EtapaFlujoRecepcion>) { return this.http.post<ApiResponse<EtapaFlujoRecepcion>>(`${this.api}/recepcion/flujo`, datos); }
  actualizarEtapaFlujo(id: number, datos: Partial<EtapaFlujoRecepcion>) { return this.http.put<ApiResponse<EtapaFlujoRecepcion>>(`${this.api}/recepcion/flujo/${id}`, datos); }
  eliminarEtapaFlujo(id: number) { return this.http.delete<ApiResponse<EtapaFlujoRecepcion>>(`${this.api}/recepcion/flujo/${id}`); }
  usuarios() { return this.http.get<ApiResponse<Usuario[]>>(`${this.api}/admin/usuarios`); }
  crearUsuario(datos: any) { return this.http.post<ApiResponse<Usuario>>(`${this.api}/admin/usuarios`, datos); }
  actualizarUsuario(id: number, datos: any) { return this.http.put<ApiResponse<Usuario>>(`${this.api}/admin/usuarios/${id}`, datos); }
  actualizarRolesUsuario(id: number, roles: string[]) { return this.http.put<ApiResponse<Usuario>>(`${this.api}/admin/usuarios/${id}/roles`, { roles }); }
  eliminarUsuario(id: number) { return this.http.delete<ApiResponse<void>>(`${this.api}/admin/usuarios/${id}`); }
  solicitudesPendientes() { return this.http.get<ApiResponse<Usuario[]>>(`${this.api}/admin/usuarios/solicitudes-pendientes`); }
  aprobarUsuario(id: number) { return this.http.put<ApiResponse<Usuario>>(`${this.api}/admin/usuarios/${id}/aprobar`, {}); }
  rechazarUsuario(id: number) { return this.http.put<ApiResponse<void>>(`${this.api}/admin/usuarios/${id}/rechazar`, {}); }
  citas() { return this.http.get<ApiResponse<any[]>>(`${this.api}/citas`); }
  crearCita(datos: any) { return this.http.post<ApiResponse<any>>(`${this.api}/citas`, datos); }
  actualizarCita(id: number, datos: any) { return this.http.put<ApiResponse<any>>(`${this.api}/citas/${id}`, datos); }
  actualizarVeterinario(id: number, datos: any) { return this.http.put<ApiResponse<Usuario>>(`${this.api}/equipo-veterinario/${id}`, datos); }

  // ------------------------------------------------------------------
  // Administracion de usuarios: bloqueo, desbloqueo y estado de la cuenta
  // ------------------------------------------------------------------

  /** Bloquea temporalmente la cuenta de un usuario (el admin indica el motivo). */
  bloquearUsuario(id: number, horas: number, motivo: string) {
    return this.http.put<ApiResponse<Usuario>>(`${this.api}/admin/usuarios/${id}/bloquear`, { horas, motivo });
  }

  /** Levanta el bloqueo de una cuenta. */
  desbloquearUsuario(id: number) {
    return this.http.put<ApiResponse<Usuario>>(`${this.api}/admin/usuarios/${id}/desbloquear`, {});
  }

  /** Desactiva la cuenta (impide el acceso; el historial se conserva). */
  desactivarUsuario(id: number, motivo: string) {
    return this.http.put<ApiResponse<Usuario>>(`${this.api}/admin/usuarios/${id}/desactivar`, { motivo });
  }

  /** Reactiva una cuenta desactivada. */
  reactivarUsuario(id: number, motivo?: string) {
    return this.http.put<ApiResponse<Usuario>>(`${this.api}/admin/usuarios/${id}/reactivar`, { motivo });
  }

  // ------------------------------------------------------------------
  // Baja y reactivacion de la cuenta por el propio titular
  // ------------------------------------------------------------------

  /** Paso 1: el backend envia un codigo de verificacion al correo. */
  solicitarDesactivacion(email: string) {
    return this.http.post<ApiResponse<void>>(`${this.api}/auth/desactivar-cuenta`, { email });
  }

  /** Paso 2: se confirma con el codigo recibido por correo. */
  confirmarDesactivacion(email: string, codigo: string) {
    return this.http.post<ApiResponse<void>>(`${this.api}/auth/confirmar-desactivacion`, { email, codigo });
  }

  solicitarReactivacion(email: string) {
    return this.http.post<ApiResponse<void>>(`${this.api}/auth/reactivar-cuenta`, { email });
  }

  confirmarReactivacion(email: string, codigo: string) {
    return this.http.post<ApiResponse<void>>(`${this.api}/auth/confirmar-reactivacion`, { email, codigo });
  }

  // ------------------------------------------------------------------
  // Mi cuenta
  // ------------------------------------------------------------------

  miPerfil() {
    return this.http.get<ApiResponse<{ id: number; nombres: string; apellidos: string; email: string; telefono: string; activo: boolean; emailVerificado: boolean; bloqueado: boolean; bloqueadoHasta: string | null; motivoBloqueo: string | null; intentosFallidos: number; ultimoAcceso: string | null; roles: string[] }>>(`${this.api}/cuenta/perfil`);
  }

  actualizarMiPerfil(datos: { nombres: string; apellidos: string; telefono: string }) {
    return this.http.put<ApiResponse<any>>(`${this.api}/cuenta/perfil`, datos);
  }

  cambiarPassword(passwordActual: string, nuevaPassword: string) {
    return this.http.put<ApiResponse<void>>(`${this.api}/cuenta/contrasena`, { passwordActual, nuevaPassword });
  }

  /** Descarga la factura en PDF generado por el servidor. */
  descargarFacturaPdf(id: number, numero: string) {
    this.descargar(`/facturas/${id}/pdf`, `factura-${(numero || id).toString().replace(/[^A-Za-z0-9_-]/g, '_')}.pdf`);
  }

  subirArchivo(archivo: File) {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post<ApiResponse<string>>(`${this.api}/archivos`, formData);
  }

  /** Consulta al asistente LLM (puede tardar hasta ~30 s en el backend). */
  chat(mensaje: string, conversacionId: number | null = null) {
    return this.http.post<ApiResponse<{ respuesta: string; consultoBaseDatos?: boolean; conversacionId: number }>>(
      `${this.api}/chat`,
      { pregunta: mensaje, conversacionId }
    ).pipe(timeout(60_000));
  }

  chatConversacionActual() { return this.http.get<ApiResponse<ChatConversacionDetalle | null>>(`${this.api}/chat/conversacion-actual`); }
  chatConversacionesAdmin() { return this.http.get<ApiResponse<ChatConversacionResumen[]>>(`${this.api}/chat/admin/conversaciones`); }
  chatConversacionAdmin(id: number) { return this.http.get<ApiResponse<ChatConversacionDetalle>>(`${this.api}/chat/admin/conversaciones/${id}`); }
  chatEliminarConversacionAdmin(id: number) { return this.http.delete<ApiResponse<void>>(`${this.api}/chat/admin/conversaciones/${id}`); }
  chatEliminarHistorialAdmin() { return this.http.delete<ApiResponse<number>>(`${this.api}/chat/admin/conversaciones`); }
  chatGuardarTurnoLocal(pregunta: string, respuesta: string, conversacionId: number | null) {
    return this.http.post<ApiResponse<number>>(`${this.api}/chat/local`, { pregunta, respuesta, conversacionId });
  }

  descargar(url: string, nombre: string) {
    this.http.get(`${this.api}${url}`, { responseType: 'blob' }).subscribe({
      next: blob => {
        const enlace = document.createElement('a');
        const objectUrl = URL.createObjectURL(blob);
        enlace.href = objectUrl;
        enlace.download = nombre;
        enlace.click();
        URL.revokeObjectURL(objectUrl);
      },
      error: (e: HttpErrorResponse) => {
        this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo descargar el archivo. Verifica permisos y conexión.');
      }
    });
  }

}
