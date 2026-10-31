import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AutorizacionService } from '../../core/autorizacion.service';

@Component({
  selector: 'app-verificacion',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './verificar-codigo.html',
  styleUrl: './verificar-codigo.css'
})
export class VerificacionComponent {
  email = this.obtenerEmail();
  codigo = '';
  mensaje = '';
  ok = false;

  constructor(private api: ApiService, private router: Router, private autorizacion: AutorizacionService) {
    if (!this.email) this.router.navigateByUrl('/registro');
  }

  get emailEnmascarado() {
    const [usuario, dominio] = this.email.split('@');
    if (!usuario || !dominio) return 'tu correo';
    return `${usuario.slice(0, 2)}***@${dominio}`;
  }

  verificar() {
    this.mensaje = '';
    const codigo = this.codigo.trim();
    if (!this.email) {
      this.ok = false;
      this.mensaje = 'No hay un correo verificado en esta sesion.';
      return;
    }
    if (!/^\d{6}$/.test(codigo)) {
      this.ok = false;
      this.mensaje = 'El codigo de verificacion tiene 6 digitos.';
      return;
    }
    this.api.verificar({ email: this.email, codigo }).subscribe({
      next: r => {
        this.autorizacion.guardarSesion(r.data);
        sessionStorage.removeItem('vetsphere_verificacion');
        const rutaInicio = this.autorizacion.tieneRol(['ROLE_CLIENTE']) ? '/mascotas' : '/dashboard';
        this.router.navigateByUrl(rutaInicio);
      },
      error: () => {
        this.ok = false;
        this.mensaje = 'Codigo invalido o expirado.';
      }
    });
  }

  reenviar() {
    if (!this.email) {
      this.ok = false;
      this.mensaje = 'No hay un correo verificado en esta sesion.';
      return;
    }
    this.api.reenviarCodigo(this.email).subscribe({
      next: () => {
        this.ok = true;
        this.mensaje = 'Codigo reenviado. Revisa tu Gmail.';
      },
      error: () => {
        this.ok = false;
        this.mensaje = 'No se pudo reenviar el codigo.';
      }
    });
  }

  private obtenerEmail() {
    const valor = sessionStorage.getItem('vetsphere_verificacion');
    if (!valor) return '';
    try {
      return JSON.parse(atob(valor)).email || '';
    } catch {
      return '';
    }
  }
}
