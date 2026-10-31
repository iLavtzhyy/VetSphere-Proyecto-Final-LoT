import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { IconoComponent } from '../../shared/components/icono/icono';
import { SesionInactividadService } from '../../core/sesion-inactividad.service';
import { AutorizacionService } from '../../core/autorizacion.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IconoComponent],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  email = '';
  password = '';
  error = '';
  recuperacionAbierta = false;
  pasoRecuperacion = 1;
  recuperacionEmail = '';
  codigoRecuperacion = '';
  nuevaPassword = '';
  mensajeRecuperacion = '';
  mensajeRecuperacionOk = false;
  enviandoRecuperacion = false;
  enviandoLogin = false;
  /** Aviso destacado cuando el backend responde con HTTP 423 (cuenta bloqueada). */
  cuentaBloqueada = false;
  mensajeBloqueo = '';
  detalleBloqueo = '';
  autenticadorPendiente = false;
  codigoAutenticador = '';
  metodosSegundoFactor: string[] = [];
  metodoSegundoFactor = 'AUTENTICADOR';
  mensajeSegundoFactor = '';

  constructor(private api: ApiService, private router: Router, private sesionInactividad: SesionInactividadService, private autorizacion: AutorizacionService) {
    const mensajeSesion = this.sesionInactividad.consumirMensaje();
    if (mensajeSesion) this.error = mensajeSesion;
  }

  entrar() {
    if (this.enviandoLogin) return;
    this.error = '';
    const correo = this.email.trim().toLowerCase();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) {
      this.error = 'Ingresa un correo válido.';
      return;
    }
    if (!this.password) {
      this.error = 'Ingresa tu contraseña.';
      return;
    }
    this.cuentaBloqueada = false;
    this.mensajeBloqueo = '';
    this.detalleBloqueo = '';
    this.enviandoLogin = true;
    this.api.login({ email: correo, password: this.password }).subscribe({
      next: r => {
        this.enviandoLogin = false;
        if (r.data?.requiereAutenticador) {
          this.autenticadorPendiente = true;
          this.metodosSegundoFactor = r.data?.metodosSegundoFactor?.length ? r.data.metodosSegundoFactor : ['AUTENTICADOR'];
          this.metodoSegundoFactor = this.metodosSegundoFactor[0];
          this.codigoAutenticador = '';
          this.mensajeSegundoFactor = this.metodoSegundoFactor === 'CORREO' ? 'Enviamos un código a tu correo.' : '';
          return;
        }
        this.finalizarSesion(r.data);
      },
      error: (e: HttpErrorResponse) => {
        this.enviandoLogin = false;
        if (e.status === 423) {
          this.cuentaBloqueada = true;
          this.error = '';
          this.mensajeBloqueo = 'La cuenta está bloqueada temporalmente. Inténtalo más tarde.';
          this.detalleBloqueo = '';
          return;
        }
        this.error = 'Correo o contraseña incorrectos.';
      }
    });
  }

  verificarAutenticador() {
    const respaldo = this.metodoSegundoFactor === 'RESPALDO';
    if (respaldo ? !/^[A-Za-z0-9-]{8,20}$/.test(this.codigoAutenticador.trim()) : !/^\d{6}$/.test(this.codigoAutenticador)) {
      this.error = respaldo ? 'Ingresa uno de tus códigos de respaldo.' : 'Ingresa el código de 6 dígitos.';
      return;
    }
    this.enviandoLogin = true;
    this.error = '';
    this.api.verificarSegundoFactor({ email: this.email.trim().toLowerCase(), password: this.password,
      metodo: this.metodoSegundoFactor, codigo: this.codigoAutenticador.trim() }).subscribe({
      next: r => { this.enviandoLogin = false; this.finalizarSesion(r.data); },
      error: () => { this.enviandoLogin = false; this.error = 'Código incorrecto o vencido.'; }
    });
  }

  solicitarCodigoSegundoFactor() {
    this.error = '';
    this.mensajeSegundoFactor = '';
    this.enviandoLogin = true;
    this.api.solicitarCodigoSegundoFactor({ email: this.email.trim().toLowerCase(), password: this.password }).subscribe({
      next: () => { this.enviandoLogin = false; this.mensajeSegundoFactor = 'Si tus datos son válidos, enviamos un código a tu correo.'; },
      error: () => { this.enviandoLogin = false; this.error = 'No se pudo enviar el código. Comprueba tus credenciales e inténtalo más tarde.'; }
    });
  }

  private finalizarSesion(datos: any) {
        this.autorizacion.guardarSesion(datos);
        this.sesionInactividad.registrarActividad();
        const rutaInicio = this.autorizacion.tieneRol(['ROLE_CLIENTE']) ? '/mascotas' : '/dashboard';
        this.router.navigateByUrl(rutaInicio);
  }

  abrirRecuperacion() {
    this.recuperacionAbierta = true;
    this.pasoRecuperacion = 1;
    this.mensajeRecuperacion = '';
    this.recuperacionEmail = this.email;
  }

  cerrarRecuperacion() {
    this.recuperacionAbierta = false;
    this.mensajeRecuperacion = '';
  }

  enviarRecuperacion() {
    if (this.enviandoRecuperacion) return;
    const email = this.recuperacionEmail.trim().toLowerCase();
    this.mensajeRecuperacion = '';
    this.mensajeRecuperacionOk = false;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      this.mensajeRecuperacion = 'Ingresa un correo válido.';
      return;
    }
    this.recuperacionEmail = email;
    this.enviandoRecuperacion = true;
    this.api.solicitarRecuperacion(email).subscribe({
      next: r => {
        this.enviandoRecuperacion = false;
        if (r && r.ok === false) {
          this.mensajeRecuperacionOk = false;
          this.mensajeRecuperacion = r.mensaje || 'No se pudo procesar la solicitud.';
          return;
        }
        this.mensajeRecuperacionOk = true;
        this.mensajeRecuperacion = 'Solicitud procesada. Si el correo está registrado, recibirás un código de recuperación.';
        this.pasoRecuperacion = 2;
      },
      error: (err: HttpErrorResponse) => {
        this.enviandoRecuperacion = false;
        this.mensajeRecuperacionOk = false;
        this.mensajeRecuperacion = err.error?.mensaje
          || (err.status === 0
            ? 'No hay conexión con el servidor. Comprueba que el backend esté activo.'
            : 'No se pudo enviar el código. Inténtalo de nuevo más tarde.');
      }
    });
  }

  restablecer() {
    this.api.restablecerPassword({ email: this.recuperacionEmail, codigo: this.codigoRecuperacion, nuevaPassword: this.nuevaPassword }).subscribe({
      next: () => {
        this.mensajeRecuperacionOk = true;
        this.mensajeRecuperacion = 'Contrasena actualizada. Ya puedes iniciar sesion.';
        this.email = this.recuperacionEmail;
        this.password = '';
        setTimeout(() => this.cerrarRecuperacion(), 800);
      },
      error: () => {
        this.mensajeRecuperacionOk = false;
        this.mensajeRecuperacion = 'Codigo invalido o expirado.';
      }
    });
  }
}
