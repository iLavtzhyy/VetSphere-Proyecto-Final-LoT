import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { ConsultaMedica, Mascota, Usuario, Factura } from '../../core/modelos';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { IconoComponent } from '../../shared/components/icono/icono';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { NotificacionService } from '../../core/notificacion.service';

@Component({
  selector: 'app-consultas',
  standalone: true,
  imports: [CommonModule, FormsModule, IconoComponent, CargadorComponent],
  animations: [fadeInUp],
  templateUrl: './consultas.html',
  styleUrl: './consultas.css'
})
export class ConsultasComponent implements OnInit {
  consultas: ConsultaMedica[] = [];
  mascotas: Mascota[] = [];
  veterinarios: Usuario[] = [];
  facturas: Factura[] = [];
  cargando = false;
  busqueda = '';
  tipos = ['Rutina', 'Emergencia', 'Control', 'Cirugia', 'Vacunacion'];
  formularioAbierto = false;
  editandoId?: number;
  mensaje = '';
  form = this.formularioVacio();

  constructor(private api: ApiService, private notificaciones: NotificacionService) {}

  ngOnInit() {
    this.cargar();
    this.api.mascotas().subscribe({
      next: r => this.mascotas = r.data,
      error: () => this.mascotas = []
    });
    this.api.veterinarios().subscribe({
      next: r => this.veterinarios = r.data,
      error: () => this.veterinarios = []
    });
  }

  cargar() {
    this.cargando = true;
    this.api.consultas().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => this.consultas = r.data,
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudieron cargar las consultas. Revisa que el backend esté activo.')
    });
    this.api.facturas().subscribe({
      next: r => this.facturas = r.data,
      error: () => this.facturas = []
    });
  }

  pagarFactura(facturaId?: number) {
    if (!facturaId) return;
    this.api.actualizarEstadoFactura(facturaId, 'Pagado').subscribe({
      next: () => this.cargar(),
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo marcar la factura como pagada.')
    });
  }

  obtenerEstadoPago(facturaId?: number): string {
    if (!facturaId) return 'Pendiente';
    const f = this.facturas.find(x => x.id === facturaId);
    return f ? f.estadoPago : 'Pendiente';
  }

  nuevaConsulta() {
    this.editandoId = undefined;
    this.form = this.formularioVacio();
    this.form.mascotaId = this.mascotas[0]?.id || null;
    this.form.veterinarioId = this.veterinarios[0]?.id || null;
    this.mensaje = '';
    this.formularioAbierto = true;
  }

  editar(consulta: ConsultaMedica) {
    this.editandoId = consulta.id;
    this.form = {
      mascotaId: null,
      veterinarioId: null,
      tipo: consulta.tipo || 'Rutina',
      motivo: consulta.tipo || 'Consulta medica',
      diagnostico: consulta.diagnostico || '',
      tratamiento: consulta.tratamiento || '',
      temperatura: consulta.temperatura || 0,
      frecuenciaCardiaca: 0,
      frecuenciaRespiratoria: 0
    };
    this.mensaje = '';
    this.formularioAbierto = true;
  }

  guardar() {
    const datos = {
      tipo: this.form.tipo,
      tipoEvento: this.form.tipo,
      motivo: this.form.motivo,
      diagnostico: this.form.diagnostico,
      tratamiento: this.form.tratamiento,
      temperatura: Number(this.form.temperatura || 0),
      frecuenciaCardiaca: Number(this.form.frecuenciaCardiaca || 0),
      frecuenciaRespiratoria: Number(this.form.frecuenciaRespiratoria || 0)
    };
    if (this.editandoId) {
      this.api.actualizarConsulta(this.editandoId, datos).subscribe({
        next: () => {
          this.mensaje = 'Consulta actualizada correctamente.';
          this.notificaciones.mostrarExito('Consulta actualizada correctamente.');
          this.cargar();
        },
        error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo actualizar la consulta.')
      });
      return;
    }
    this.api.crearConsulta({
      ...datos,
      fecha: new Date().toISOString().slice(0, 19),
      mascota: { id: this.form.mascotaId },
      veterinario: { id: this.form.veterinarioId }
    }).subscribe({
      next: () => {
        this.mensaje = 'Consulta registrada correctamente.';
        this.notificaciones.mostrarExito('Consulta registrada correctamente.');
        this.cargar();
      },
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo registrar la consulta.')
    });
  }

  eliminar(consulta: ConsultaMedica) {
    if (!confirm(`Eliminar consulta de ${consulta.mascota}?`)) return;
    this.api.eliminarConsulta(consulta.id).subscribe({
      next: () => this.consultas = this.consultas.filter(c => c.id !== consulta.id),
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo eliminar la consulta.')
    });
  }

  cerrarFormulario() {
    this.formularioAbierto = false;
    this.mensaje = '';
  }

  formularioVacio() {
    return {
      mascotaId: null as number | null,
      veterinarioId: null as number | null,
      tipo: 'Rutina',
      motivo: '',
      diagnostico: '',
      tratamiento: '',
      temperatura: 38,
      frecuenciaCardiaca: 90,
      frecuenciaRespiratoria: 24
    };
  }

  contar(tipo: string) { return this.consultas.filter(c => (c.tipo || '').toLowerCase() === tipo.toLowerCase()).length; }

  badgeTipo(tipo: string) {
    const t = (tipo || '').toLowerCase();
    if (t.includes('emerg')) return 'badge-danger';
    if (t.includes('cirug')) return 'badge-warn';
    if (t.includes('vac')) return 'badge-ok';
    return 'badge-info';
  }

  get filtradas() {
    const q = this.busqueda.toLowerCase();
    return this.consultas.filter(c => `${c.mascota} ${c.cliente} ${c.veterinario} ${c.diagnostico}`.toLowerCase().includes(q));
  }
}
