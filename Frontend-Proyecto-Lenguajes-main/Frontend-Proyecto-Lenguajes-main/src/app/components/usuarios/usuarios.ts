import { CommonModule } from '@angular/common';
import { Component, OnInit, HostListener } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { Usuario } from '../../core/modelos';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { IconoComponent } from '../../shared/components/icono/icono';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { NotificacionService } from '../../core/notificacion.service';
import { AutorizacionService } from '../../core/autorizacion.service';

/** Acciones de estado de cuenta que puede aplicar el administrador. */
type ModoEstadoCuenta = 'bloquear' | 'desbloquear' | 'desactivar' | 'reactivar';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, IconoComponent, CargadorComponent],
  animations: [fadeInUp],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.css'
})
export class UsuariosComponent implements OnInit {
  usuarios: Usuario[] = [];
  solicitudes: Usuario[] = [];
  cargando = false;
  busqueda = '';
  formularioAbierto = false;
  mensaje = '';
  rol = 'ROLE_VETERINARIO';
  rolesDisponibles = ['ROLE_ADMIN', 'ROLE_RECEPCIONISTA', 'ROLE_VETERINARIO', 'ROLE_CLIENTE'];
  esNuevo = true;
  form = this.formularioVacio();
  menuAbiertoId: number | null = null;
  menuEstilo: Record<string, string> = {};

  // ------------------------------------------------------------------
  // Estado de cuenta (solo ROLE_ADMIN)
  // ------------------------------------------------------------------
  modalEstadoAbierto = false;
  modoEstado: ModoEstadoCuenta = 'bloquear';
  usuarioEstado: Usuario | null = null;
  horasBloqueo = 24;
  motivoEstado = '';
  errorEstado = '';
  procesandoEstado = false;
  usuarioEliminarPendiente: Usuario | null = null;
  procesandoEliminacion = false;

  constructor(
    private api: ApiService,
    private notificaciones: NotificacionService,
    private autorizacion: AutorizacionService
  ) {}

  ngOnInit() { this.cargar(); }

  /** La gestion de estado de cuenta queda reservada al administrador. */
  get esAdmin() {
    return this.autorizacion.tieneRol(['ROLE_ADMIN']);
  }

  /** Abre o cierra el menu de opciones de una fila. */
  toggleMenu(id: number | null, event?: MouseEvent) {
    this.menuAbiertoId = this.menuAbiertoId === id ? null : id;
    if (this.menuAbiertoId === null) { this.menuEstilo = {}; return; }
    const rect = (event?.currentTarget as HTMLElement | null)?.getBoundingClientRect();
    if (!rect) { this.menuEstilo = {}; return; }
    const ancho = 208;
    const alto = 240;
    const top = window.innerHeight - rect.bottom < alto + 12
      ? Math.max(8, rect.top - alto - 4)
      : rect.bottom + 4;
    const left = Math.max(8, Math.min(window.innerWidth - ancho - 8, rect.right - ancho));
    this.menuEstilo = { position: 'fixed', top: `${top}px`, left: `${left}px`, width: `${ancho}px`, maxHeight: 'calc(100vh - 16px)', overflowY: 'auto', zIndex: '100' };
  }

  /** Cierra el menu de opciones al pulsar fuera de la tabla. */
  @HostListener('document:click')
  cerrarMenu() {
    this.menuAbiertoId = null;
    this.menuEstilo = {};
  }

  cargar() {
    this.cargando = true;
    this.api.usuarios().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => this.usuarios = r.data,
      error: (e) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudieron cargar los usuarios. Revisa que el backend esté activo.')
    });
    this.api.solicitudesPendientes().subscribe({
      next: r => this.solicitudes = r.data,
      error: () => this.solicitudes = []
    });
  }

  nuevo() {
    this.esNuevo = true;
    this.form = this.formularioVacio();
    this.rol = 'ROLE_VETERINARIO';
    this.formularioAbierto = true;
    this.mensaje = '';
  }

  editar(usuario: Usuario) {
    this.esNuevo = false;
    this.rol = this.rolPrincipal(usuario);
    this.form = {
      id: usuario.id,
      nombres: usuario.nombres,
      apellidos: usuario.apellidos,
      email: usuario.email,
      telefono: usuario.telefono || '',
      password: '',
      fotoUrl: usuario.fotoUrl || '',
      especialidad: usuario.especialidad || '',
      cedulaProfesional: usuario.cedulaProfesional || '',
      biografia: usuario.biografia || '',
      activo: usuario.activo ?? true
    } as any;
    this.formularioAbierto = true;
    this.mensaje = '';
  }

  guardar() {
    const payload = { ...this.form, roles: [this.rol] };
    if (this.esNuevo) {
      this.api.crearUsuario(payload).subscribe({
        next: () => {
          this.mensaje = 'Usuario creado correctamente.';
          this.notificaciones.mostrarExito('Usuario creado correctamente.');
          this.cargar();
          this.cerrar();
        },
        error: (err) => {
          this.notificaciones.mostrarError(err.error?.mensaje || 'Error al crear el usuario.');
        }
      });
    } else {
      this.api.actualizarUsuario((this.form as any).id, payload).subscribe({
        next: () => {
          this.mensaje = 'Usuario actualizado correctamente.';
          this.notificaciones.mostrarExito('Usuario actualizado correctamente.');
          this.cargar();
          this.cerrar();
        },
        error: (err) => {
          this.notificaciones.mostrarError(err.error?.mensaje || 'Error al actualizar el usuario.');
        }
      });
    }
  }

  aprobar(id: number) {
    this.api.aprobarUsuario(id).subscribe({
      next: () => {
        this.notificaciones.mostrarExito('Solicitud aprobada correctamente.');
        this.cargar();
      },
      error: (err) => this.notificaciones.mostrarError(err.error?.mensaje || 'No se pudo aprobar la solicitud.')
    });
  }

  eliminarUsuario(id: number) {
    this.usuarioEliminarPendiente = this.usuarios.find(usuario => usuario.id === id) || null;
  }

  confirmarEliminarUsuario() {
    const usuario = this.usuarioEliminarPendiente;
    if (!usuario || this.procesandoEliminacion) return;
    this.procesandoEliminacion = true;
    this.api.eliminarUsuario(usuario.id).pipe(finalize(() => this.procesandoEliminacion = false)).subscribe({
      next: () => {
        this.usuarioEliminarPendiente = null;
        this.usuarios = this.usuarios.filter(item => item.id !== usuario.id);
        this.notificaciones.mostrarExito(`${this.nombreCompleto(usuario)} se eliminó permanentemente. Los historiales y comprobantes se conservaron sin asociacion a la cuenta.`);
        this.cargar();
      },
      error: err => this.notificaciones.mostrarError(err.error?.mensaje || 'No se pudo eliminar el usuario.')
    });
  }

  cambiarRol(usuario: Usuario, rol: string) {
    this.api.actualizarRolesUsuario(usuario.id, [rol]).subscribe({
      next: r => {
        this.usuarios = this.usuarios.map(u => u.id === r.data.id ? r.data : u);
        this.notificaciones.mostrarExito(`Rol actualizado a ${rol}.`);
      },
      error: (e) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo actualizar el rol del usuario.')
    });
  }

  cerrar() {
    this.formularioAbierto = false;
    this.mensaje = '';
  }

  // ------------------------------------------------------------------
  // Estado de cuenta: bloqueo, desbloqueo, activacion y desactivacion
  // ------------------------------------------------------------------

  /** Abre el formulario de estado de cuenta para el usuario indicado. */
  abrirEstado(modo: ModoEstadoCuenta, usuario: Usuario) {
    this.modoEstado = modo;
    this.usuarioEstado = usuario;
    this.horasBloqueo = 24;
    this.motivoEstado = '';
    this.errorEstado = '';
    this.procesandoEstado = false;
    this.modalEstadoAbierto = true;
  }

  cerrarEstado() {
    this.modalEstadoAbierto = false;
    this.usuarioEstado = null;
    this.errorEstado = '';
    this.motivoEstado = '';
  }

  get tituloEstado(): string {
    switch (this.modoEstado) {
      case 'bloquear': return 'Bloquear cuenta';
      case 'desbloquear': return 'Desbloquear cuenta';
      case 'desactivar': return 'Desactivar cuenta';
      case 'reactivar': return 'Reactivar cuenta';
      default: return 'Estado de cuenta';
    }
  }

  get descripcionEstado(): string {
    switch (this.modoEstado) {
      case 'bloquear': return 'La cuenta no podra iniciar sesion durante el periodo indicado.';
      case 'desbloquear': return 'Se levantara el bloqueo vigente y la cuenta podra volver a iniciar sesion.';
      case 'desactivar': return 'La cuenta perdera el acceso y quedara marcada como desactivada (el historial se conserva).';
      case 'reactivar': return 'La cuenta volvera a estar activa y podra iniciar sesion nuevamente.';
      default: return '';
    }
  }

  get textoBotonEstado(): string {
    switch (this.modoEstado) {
      case 'bloquear': return 'Bloquear cuenta';
      case 'desbloquear': return 'Desbloquear ahora';
      case 'desactivar': return 'Desactivar cuenta';
      case 'reactivar': return 'Reactivar cuenta';
      default: return 'Confirmar';
    }
  }

  get claseBotonEstado(): string {
    return this.modoEstado === 'bloquear' || this.modoEstado === 'desactivar' ? 'boton-peligro' : 'boton-primario';
  }

  get motivoObligatorio(): boolean {
    return this.modoEstado === 'bloquear' || this.modoEstado === 'desactivar';
  }

  get muestraHoras(): boolean {
    return this.modoEstado === 'bloquear';
  }

  /** Valida y ejecuta la accion de estado de cuenta seleccionada. */
  confirmarEstado() {
    if (!this.usuarioEstado || this.procesandoEstado) return;
    const usuario = this.usuarioEstado;
    const motivo = this.motivoEstado.trim();
    this.errorEstado = '';

    if (this.modoEstado === 'bloquear') {
      const horas = Number(this.horasBloqueo);
      if (!Number.isFinite(horas) || !Number.isInteger(horas) || horas < 1 || horas > 720) {
        this.errorEstado = 'Las horas de bloqueo deben ser un numero entero entre 1 y 720.';
        return;
      }
      if (motivo.length < 5) {
        this.errorEstado = 'Describe el motivo del bloqueo (minimo 5 caracteres).';
        return;
      }
      if (motivo.length > 300) {
        this.errorEstado = 'El motivo no puede superar los 300 caracteres.';
        return;
      }
      this.procesandoEstado = true;
      this.api.bloquearUsuario(usuario.id, horas, motivo).subscribe({
        next: () => this.finalizarEstado(`Cuenta de ${this.nombreCompleto(usuario)} bloqueada por ${horas} h.`),
        error: (err) => this.fallarEstado(err, 'No se pudo bloquear la cuenta.')
      });
      return;
    }

    if (this.modoEstado === 'desbloquear') {
      this.procesandoEstado = true;
      this.api.desbloquearUsuario(usuario.id).subscribe({
        next: () => this.finalizarEstado(`Cuenta de ${this.nombreCompleto(usuario)} desbloqueada.`),
        error: (err) => this.fallarEstado(err, 'No se pudo desbloquear la cuenta.')
      });
      return;
    }

    if (this.modoEstado === 'desactivar') {
      if (motivo.length < 5) {
        this.errorEstado = 'Describe el motivo de la desactivacion (minimo 5 caracteres).';
        return;
      }
      if (motivo.length > 300) {
        this.errorEstado = 'El motivo no puede superar los 300 caracteres.';
        return;
      }
      this.procesandoEstado = true;
      this.api.desactivarUsuario(usuario.id, motivo).subscribe({
        next: () => this.finalizarEstado(`Cuenta de ${this.nombreCompleto(usuario)} desactivada.`),
        error: (err) => this.fallarEstado(err, 'No se pudo desactivar la cuenta.')
      });
      return;
    }

    // Reactivar (el motivo es opcional, pero se limita a 300 caracteres).
    if (motivo.length > 300) {
      this.errorEstado = 'El motivo no puede superar los 300 caracteres.';
      return;
    }
    this.procesandoEstado = true;
    this.api.reactivarUsuario(usuario.id, motivo || undefined).subscribe({
      next: () => this.finalizarEstado(`Cuenta de ${this.nombreCompleto(usuario)} reactivada.`),
      error: (err) => this.fallarEstado(err, 'No se pudo reactivar la cuenta.')
    });
  }

  private finalizarEstado(mensaje: string) {
    this.procesandoEstado = false;
    this.notificaciones.mostrarExito(mensaje);
    this.cerrarEstado();
    this.cargar();
  }

  private fallarEstado(err: any, porDefecto: string) {
    this.procesandoEstado = false;
    this.errorEstado = err?.error?.mensaje || porDefecto;
    this.notificaciones.mostrarError(this.errorEstado);
  }

  /** Indica si el usuario tiene un bloqueo vigente en este momento. */
  bloqueoVigente(usuario: Usuario): boolean {
    if (usuario.bloqueadoAhora === true) return true;
    if (!usuario.bloqueadoHasta) return false;
    const fecha = new Date(usuario.bloqueadoHasta);
    if (isNaN(fecha.getTime())) return false;
    return fecha.getTime() > Date.now();
  }

  /** Texto del badge principal de estado de la cuenta. */
  textoEstado(usuario: Usuario): string {
    if (this.bloqueoVigente(usuario)) return `Bloqueado hasta ${this.formatearFecha(usuario.bloqueadoHasta)}`;
    if (usuario.activo === false) return 'Desactivado';
    return 'Activo';
  }

  /** Clase CSS del badge principal de estado de la cuenta. */
  claseEstado(usuario: Usuario): string {
    if (this.bloqueoVigente(usuario)) return 'badge-danger';
    if (usuario.activo === false) return 'badge-warn';
    return 'badge-ok';
  }

  /** Formatea una fecha ISO de forma segura (devuelve '' si no es valida). */
  formatearFecha(valor?: string | null): string {
    if (!valor) return 'sin fecha';
    const fecha = new Date(valor);
    if (isNaN(fecha.getTime())) return 'sin fecha';
    const dosDigitos = (n: number) => n.toString().padStart(2, '0');
    return `${dosDigitos(fecha.getDate())}/${dosDigitos(fecha.getMonth() + 1)}/${fecha.getFullYear()} ${dosDigitos(fecha.getHours())}:${dosDigitos(fecha.getMinutes())}`;
  }

  nombreCompleto(usuario: Usuario): string {
    return `${usuario.nombres || ''} ${usuario.apellidos || ''}`.trim() || usuario.email;
  }

  formularioVacio() {
    return {
      nombres: '',
      apellidos: '',
      email: '',
      telefono: '',
      password: '',
      fotoUrl: '',
      especialidad: '',
      cedulaProfesional: '',
      biografia: '',
      activo: true
    };
  }

  rolSeleccionado = 'TODOS';

  rolPrincipal(usuario: Usuario) {
    return usuario.roles?.[0]?.nombre || 'Sin rol';
  }

  porRol(rol: string) {
    return this.usuarios.filter(u => u.roles?.some(r => r.nombre === rol)).length;
  }

  get filtrados() {
    const q = this.busqueda.toLowerCase();
    let res = this.usuarios;
    if (this.rolSeleccionado !== 'TODOS') {
      res = res.filter(u => u.roles?.some(r => r.nombre === this.rolSeleccionado));
    }
    return res.filter(u => `${u.nombres} ${u.apellidos} ${u.email} ${u.telefono} ${this.rolPrincipal(u)}`.toLowerCase().includes(q));
  }
}
