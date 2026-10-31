import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './registro.html',
  styleUrl: './registro.css'
})
export class RegistroComponent {
  tipoRegistro: 'cliente' | 'personal' = 'cliente';
  form = { nombres: '', apellidos: '', email: '', telefono: '', documento: '', password: '', rolSolicitado: 'ROLE_CLIENTE' };
  confirmarPassword = '';
  codigoAdmin = '';
  solicitudEnviada = false;
  error = '';

  constructor(private api: ApiService, private router: Router) {}

  cambiarTipo(tipo: 'cliente' | 'personal') {
    this.tipoRegistro = tipo;
    this.form.rolSolicitado = tipo === 'cliente' ? 'ROLE_CLIENTE' : 'ROLE_VETERINARIO';
    this.error = '';
  }

  registrar() {
    this.error = '';
    const nombres = this.form.nombres.trim();
    const apellidos = this.form.apellidos.trim();
    const email = this.form.email.trim().toLowerCase();
    const password = this.form.password;

    if (!nombres || !apellidos) {
      this.error = 'Completa tus nombres y apellidos.';
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      this.error = 'Ingresa un correo valido.';
      return;
    }
    const documento = this.form.documento.trim();
    if (this.tipoRegistro === 'cliente' && !/^\d{10}$/.test(documento)) {
      this.error = 'Ingresa tu número de cédula de 10 dígitos.';
      return;
    }
    if (password.length < 8) {
      this.error = 'La contrasena debe tener al menos 8 caracteres.';
      return;
    }
    if (password !== this.confirmarPassword) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }
    if (this.tipoRegistro === 'personal' && !this.codigoAdmin.trim()) {
      this.error = 'Ingresa el codigo de administrador para registrar personal.';
      return;
    }

    const payload = {
      ...this.form,
      nombres,
      apellidos,
      email,
      documento: this.tipoRegistro === 'cliente' ? documento : undefined,
      password,
      codigoAdmin: this.tipoRegistro === 'personal' ? this.codigoAdmin.trim() : undefined
    };
    this.api.registrar(payload).subscribe({
      next: () => {
        if (this.tipoRegistro === 'cliente') {
          sessionStorage.setItem('vetsphere_verificacion', btoa(JSON.stringify({ email })));
          this.router.navigateByUrl('/verificacion');
        } else {
          this.solicitudEnviada = true;
        }
      },
      error: (err) => {
        this.error = err.error?.mensaje || 'No se pudo registrar. Revisa los datos, correo duplicado o el código de administrador.';
      }
    });
  }
}
