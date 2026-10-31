import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { Usuario } from '../../core/modelos';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { NotificacionService } from '../../core/notificacion.service';

@Component({
  selector: 'app-administracion',
  standalone: true,
  imports: [CommonModule, RouterLink, CargadorComponent],
  animations: [fadeInUp],
  templateUrl: './administracion.html',
  styleUrl: './administracion.css'
})
export class AdministracionComponent implements OnInit {
  indicadores: Record<string, number> = {};
  solicitudes: Usuario[] = [];
  cargando = false;
  codigoPersonal = '';
  codigoRevelado = false;
  cuentaAtrasCodigo = 30;
  private temporizadorCodigo?: ReturnType<typeof setInterval>;
  constructor(private api: ApiService, private notificaciones: NotificacionService) {}
  ngOnInit() {
    this.actualizarCodigo();
    this.temporizadorCodigo = setInterval(() => {
      this.cuentaAtrasCodigo--;
      if (this.cuentaAtrasCodigo <= 0) this.actualizarCodigo();
    }, 1000);
    this.cargando = true;
    this.api.administracion().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => this.indicadores = r.data,
      error: e => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudieron cargar los indicadores administrativos.')
    });
    this.api.solicitudesPendientes().subscribe({
      next: r => this.solicitudes = r.data,
      error: () => this.solicitudes = []
    });
  }
  ngOnDestroy() { if (this.temporizadorCodigo) clearInterval(this.temporizadorCodigo); }
  alternarCodigo() { this.codigoRevelado = !this.codigoRevelado; }
  private actualizarCodigo() {
    this.api.codigoRegistroPersonal().subscribe({
      next: r => { this.codigoPersonal = r.data.codigo; this.cuentaAtrasCodigo = r.data.segundosRestantes; },
      error: () => { this.codigoPersonal = ''; }
    });
  }
  aprobar(id: number) {
    this.api.aprobarUsuario(id).subscribe({
      next: () => {
        this.solicitudes = this.solicitudes.filter(s => s.id !== id);
        this.indicadores['solicitudes'] = Math.max(0, (this.indicadores['solicitudes'] ?? 0) - 1);
        this.notificaciones.mostrarExito('Solicitud aprobada correctamente.');
      },
      error: e => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo aprobar la solicitud.')
    });
  }
  rechazar(id: number) {
    this.api.rechazarUsuario(id).subscribe({
      next: () => {
        this.solicitudes = this.solicitudes.filter(s => s.id !== id);
        this.indicadores['solicitudes'] = Math.max(0, (this.indicadores['solicitudes'] ?? 0) - 1);
        this.notificaciones.mostrarInfo('Solicitud rechazada.');
      },
      error: e => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo rechazar la solicitud.')
    });
  }
}
