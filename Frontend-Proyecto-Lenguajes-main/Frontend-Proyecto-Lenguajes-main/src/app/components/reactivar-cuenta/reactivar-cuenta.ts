import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { NotificacionService } from '../../core/notificacion.service';
import { IconoComponent } from '../../shared/components/icono/icono';
import { fadeInUp } from '../../shared/animations/fade-in-up';

/**
 * Pantalla publica para reactivar una cuenta desactivada.
 *
 * El titular de una cuenta desactivada ya no puede iniciar sesion, por lo que
 * necesita una ruta accesible sin token: se solicita un codigo por correo y
 * se confirma con el mismo. Las respuestas son neutras (no revelan si el
 * correo esta registrado) y el codigo es de un solo uso.
 */
@Component({
  selector: 'app-reactivar-cuenta',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IconoComponent],
  animations: [fadeInUp],
  templateUrl: './reactivar-cuenta.html',
  styleUrl: './reactivar-cuenta.css'
})
export class ReactivarCuentaComponent {
  email = '';
  codigo = '';
  /** 1 = solicitar codigo, 2 = confirmar codigo, 3 = cuenta reactivada. */
  paso = 1;
  mensaje = '';
  error = '';
  procesando = false;

  constructor(private api: ApiService, private notificaciones: NotificacionService) {}

  /** Paso 1: el backend envia un codigo de reactivacion al correo. */
  solicitarCodigo() {
    const email = this.email.trim().toLowerCase();
    if (!email) {
      this.error = 'Ingresa el correo con el que te registraste.';
      return;
    }
    this.error = '';
    this.mensaje = '';
    this.procesando = true;
    this.api.solicitarReactivacion(email).subscribe({
      next: () => {
        this.procesando = false;
        this.paso = 2;
        this.mensaje = 'Si el correo esta registrado, te enviamos un codigo de reactivacion.';
        this.notificaciones.mostrarInfo('Solicitud de reactivacion enviada.');
      },
      error: (err) => {
        this.procesando = false;
        this.error = err?.error?.mensaje || 'No se pudo enviar el codigo de reactivacion.';
      }
    });
  }

  /** Paso 2: se confirma con el codigo recibido por correo. */
  confirmar() {
    const email = this.email.trim().toLowerCase();
    const codigo = this.codigo.trim();
    if (!email) {
      this.error = 'Ingresa el correo con el que te registraste.';
      return;
    }
    if (codigo.length !== 6) {
      this.error = 'El codigo de verificacion tiene 6 digitos.';
      return;
    }
    this.error = '';
    this.mensaje = '';
    this.procesando = true;
    this.api.confirmarReactivacion(email, codigo).subscribe({
      next: () => {
        this.procesando = false;
        this.paso = 3;
        this.mensaje = 'Tu cuenta quedo reactivada. Ya puedes iniciar sesion con normalidad.';
        this.notificaciones.mostrarExito('Tu cuenta quedo reactivada.');
      },
      error: (err) => {
        this.procesando = false;
        this.error = err?.error?.mensaje || 'Codigo invalido o expirado. Solicita uno nuevo.';
      }
    });
  }

  reiniciar() {
    this.paso = 1;
    this.codigo = '';
    this.mensaje = '';
    this.error = '';
  }
}
