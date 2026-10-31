import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { NotificacionService } from '../../core/notificacion.service';
import { IconoComponent } from '../../shared/components/icono/icono';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { fadeInUp } from '../../shared/animations/fade-in-up';

/** Perfil devuelto por GET /api/cuenta/perfil. */
interface PerfilUsuario {
  id: number;
  nombres: string;
  apellidos: string;
  email: string;
  telefono: string;
  activo: boolean;
  emailVerificado: boolean;
  bloqueado: boolean;
  bloqueadoHasta: string | null;
  motivoBloqueo: string | null;
  intentosFallidos: number;
  ultimoAcceso: string | null;
  roles: string[];
  autenticadorHabilitado?: boolean;
  segundoFactorCorreoHabilitado?: boolean;
  codigosRespaldoRestantes?: number;
}

@Component({
  selector: 'app-mi-cuenta',
  standalone: true,
  imports: [CommonModule, FormsModule, IconoComponent, CargadorComponent],
  animations: [fadeInUp],
  templateUrl: './mi-cuenta.html',
  styleUrl: './mi-cuenta.css'
})
export class MiCuentaComponent implements OnInit {
  perfil: PerfilUsuario | null = null;
  /** Roles del perfil ya formateados (se cachean para no recrear el array en cada ciclo). */
  rolesTexto: string[] = [];
  cargando = true;
  errorPerfil = '';
  nombresEdicion = '';
  apellidosEdicion = '';
  telefonoEdicion = '';
  guardandoPerfil = false;
  mensajePerfil = '';
  qrAutenticador = '';
  secretoAutenticador = '';
  codigoAutenticador = '';
  passwordAutenticador = '';
  errorAutenticador = '';
  passwordSegundoFactor = '';
  codigosRespaldoNuevos: string[] = [];

  // Cambio de contrasena
  passwordActual = '';
  nuevaPassword = '';
  confirmarPassword = '';
  errorPassword = '';
  mensajePassword = '';
  procesandoPassword = false;

  constructor(
    private api: ApiService,
    private notificaciones: NotificacionService,
  ) {}

  ngOnInit() {
    this.cargarPerfil();
  }

  cargarPerfil() {
    this.cargando = true;
    this.errorPerfil = '';
    this.api.miPerfil().subscribe({
      next: r => {
        this.perfil = r.data;
        this.rolesTexto = (r.data?.roles || []).map(rol => this.nombreRol(rol));
        this.nombresEdicion = r.data?.nombres || '';
        this.apellidosEdicion = r.data?.apellidos || '';
        this.telefonoEdicion = r.data?.telefono || '';
        this.cargando = false;
      },
      error: (err) => {
        this.cargando = false;
        this.errorPerfil = err?.error?.mensaje || 'No se pudo cargar tu perfil. Verifica tu sesion e intentalo de nuevo.';
      }
    });
  }

  guardarPerfil() {
    if (!this.nombresEdicion.trim() || !this.apellidosEdicion.trim()) {
      this.errorPerfil = 'Completa tus nombres y apellidos.';
      return;
    }
    this.guardandoPerfil = true;
    this.errorPerfil = '';
    this.mensajePerfil = '';
    this.api.actualizarMiPerfil({
      nombres: this.nombresEdicion.trim(),
      apellidos: this.apellidosEdicion.trim(),
      telefono: this.telefonoEdicion.trim()
    }).subscribe({
      next: r => {
        this.perfil = r.data;
        this.guardandoPerfil = false;
        this.mensajePerfil = 'Perfil actualizado.';
        this.notificaciones.mostrarExito(this.mensajePerfil);
      },
      error: err => {
        this.guardandoPerfil = false;
        this.errorPerfil = err?.error?.mensaje || 'No se pudo actualizar el perfil.';
      }
    });
  }

  configurarAutenticador() {
    this.errorAutenticador = '';
    this.api.configurarAutenticador().subscribe({
      next: r => { this.qrAutenticador = r.data.qrDataUri; this.secretoAutenticador = r.data.secreto; },
      error: e => this.errorAutenticador = e?.error?.mensaje || 'No se pudo preparar el autenticador.'
    });
  }

  activarAutenticador() {
    this.api.activarAutenticador(this.codigoAutenticador).subscribe({
      next: () => { this.qrAutenticador = ''; this.codigoAutenticador = ''; this.cargarPerfil(); this.notificaciones.mostrarExito('Verificación en dos pasos activada.'); },
      error: e => this.errorAutenticador = e?.error?.mensaje || 'Código incorrecto.'
    });
  }

  desactivarAutenticador() {
    this.api.desactivarAutenticador({ password: this.passwordAutenticador, codigo: this.codigoAutenticador }).subscribe({
      next: () => { this.passwordAutenticador = ''; this.codigoAutenticador = ''; this.cargarPerfil(); this.notificaciones.mostrarExito('Verificación en dos pasos desactivada.'); },
      error: e => this.errorAutenticador = e?.error?.mensaje || 'Contraseña o código incorrectos.'
    });
  }

  configurarSegundoFactorCorreo(activo: boolean) {
    this.errorAutenticador = '';
    this.api.configurarSegundoFactorCorreo(this.passwordSegundoFactor, activo).subscribe({
      next: () => {
        this.passwordSegundoFactor = '';
        this.cargarPerfil();
        this.notificaciones.mostrarExito(activo ? 'Verificación por correo activada.' : 'Verificación por correo desactivada.');
      },
      error: e => this.errorAutenticador = e?.error?.mensaje || 'No se pudo actualizar la verificación por correo.'
    });
  }

  generarCodigosRespaldo() {
    this.errorAutenticador = '';
    this.codigosRespaldoNuevos = [];
    this.api.generarCodigosRespaldo(this.passwordSegundoFactor).subscribe({
      next: r => {
        this.passwordSegundoFactor = '';
        this.codigosRespaldoNuevos = r.data || [];
        this.cargarPerfil();
      },
      error: e => this.errorAutenticador = e?.error?.mensaje || 'No se pudieron generar los códigos de respaldo.'
    });
  }

  descargarCodigosRespaldo() {
    if (!this.codigosRespaldoNuevos.length) return;
    const generado = new Date().toLocaleString('es-EC');
    const contenido = [
      'VetSphere — Códigos de respaldo',
      'Cuenta: ' + (this.perfil?.email || ''),
      'Generados: ' + generado,
      '',
      'Cada código se puede usar una sola vez. Guárdalos en un lugar seguro.',
      '',
      ...this.codigosRespaldoNuevos
    ].join('\r\n');
    const archivo = new Blob(['\uFEFF', contenido], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(archivo);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = 'VetSphere_codigos_respaldo_' + new Date().toISOString().slice(0, 10) + '.txt';
    enlace.style.display = 'none';
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  // ------------------------------------------------------------------
  // Cambio de contrasena
  // ------------------------------------------------------------------

  cambiarPassword() {
    this.errorPassword = '';
    this.mensajePassword = '';
    if (!this.passwordActual) {
      this.errorPassword = 'Ingresa tu contrasena actual.';
      return;
    }
    if (this.nuevaPassword.length < 8) {
      this.errorPassword = 'La nueva contrasena debe tener al menos 8 caracteres.';
      return;
    }
    if (this.nuevaPassword !== this.confirmarPassword) {
      this.errorPassword = 'La nueva contrasena y su confirmacion no coinciden.';
      return;
    }
    if (this.passwordActual === this.nuevaPassword) {
      this.errorPassword = 'La nueva contrasena debe ser distinta de la actual.';
      return;
    }
    this.procesandoPassword = true;
    this.api.cambiarPassword(this.passwordActual, this.nuevaPassword).subscribe({
      next: () => {
        this.procesandoPassword = false;
        this.passwordActual = '';
        this.nuevaPassword = '';
        this.confirmarPassword = '';
        this.mensajePassword = 'Contrasena actualizada correctamente.';
        this.notificaciones.mostrarExito('Contrasena actualizada correctamente.');
      },
      error: (err) => {
        this.procesandoPassword = false;
        this.errorPassword = err?.error?.mensaje || 'No se pudo actualizar la contrasena. Revisa la contrasena actual.';
      }
    });
  }

  // ------------------------------------------------------------------
  // Utilidades de presentacion
  // ------------------------------------------------------------------

  get nombreCompleto(): string {
    if (!this.perfil) return 'Mi cuenta';
    return `${this.perfil.nombres || ''} ${this.perfil.apellidos || ''}`.trim() || this.perfil.email;
  }

  get rolesLegibles(): string[] {
    return this.rolesTexto;
  }

  nombreRol(rol: string): string {
    const limpio = (rol || '').replace(/^ROLE_/, '');
    if (!limpio) return 'Sin rol';
    return limpio.charAt(0).toUpperCase() + limpio.slice(1).toLowerCase();
  }

  /** Formatea una fecha ISO de forma segura. */
  formatearFecha(valor?: string | null): string {
    if (!valor) return 'Sin registro';
    const fecha = new Date(valor);
    if (isNaN(fecha.getTime())) return 'Sin registro';
    const dosDigitos = (n: number) => n.toString().padStart(2, '0');
    return `${dosDigitos(fecha.getDate())}/${dosDigitos(fecha.getMonth() + 1)}/${fecha.getFullYear()} ${dosDigitos(fecha.getHours())}:${dosDigitos(fecha.getMinutes())}`;
  }

}
