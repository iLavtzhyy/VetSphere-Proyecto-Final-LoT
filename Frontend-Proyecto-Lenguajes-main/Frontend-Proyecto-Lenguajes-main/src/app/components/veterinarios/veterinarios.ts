import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { Usuario } from '../../core/modelos';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { IconoComponent } from '../../shared/components/icono/icono';
import { AutorizacionService } from '../../core/autorizacion.service';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { NotificacionService } from '../../core/notificacion.service';

@Component({
  selector: 'app-veterinarios',
  standalone: true,
  imports: [CommonModule, FormsModule, IconoComponent, CargadorComponent],
  animations: [fadeInUp],
  templateUrl: './veterinarios.html',
  styleUrl: './veterinarios.css'
})
export class VeterinariosComponent implements OnInit {
  veterinarios: Usuario[] = [];
  editando?: Usuario;
  mensaje = '';
  cargando = false;
  constructor(private api: ApiService, public autorizacion: AutorizacionService, private notificaciones: NotificacionService) {}
  ngOnInit() {
    this.cargando = true;
    this.api.veterinarios().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => this.veterinarios = r.data,
      error: e => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo cargar el equipo de veterinarios.')
    });
  }

  puedeEditar() {
    return this.autorizacion.tieneRol(['ROLE_ADMIN']);
  }

  editar(veterinario: Usuario) {
    this.editando = { ...veterinario, roles: veterinario.roles || [] };
    this.mensaje = '';
  }

  cerrar() {
    this.editando = undefined;
  }

  guardar() {
    if (!this.editando) return;
    this.api.actualizarVeterinario(this.editando.id, this.editando).subscribe({
      next: r => {
        this.mensaje = 'Veterinario actualizado.';
        this.notificaciones.mostrarExito('Veterinario actualizado correctamente.');
        this.veterinarios = this.veterinarios.map(v => v.id === r.data.id ? r.data : v);
        this.cerrar();
      },
      error: e => {
        this.mensaje = e?.error?.mensaje || e?.error?.message || 'No se pudo guardar. Verifica la conexión y los permisos de administrador.';
        this.notificaciones.mostrarError(this.mensaje);
      }
    });
  }
}
