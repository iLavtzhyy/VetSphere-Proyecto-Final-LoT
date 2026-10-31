import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { EtapaFlujoRecepcion, Mascota, Usuario } from '../../core/modelos';
import { IconoComponent } from '../../shared/components/icono/icono';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { NotificacionService } from '../../core/notificacion.service';

@Component({
  selector: 'app-recepcion',
  standalone: true,
  imports: [CommonModule, FormsModule, IconoComponent, CargadorComponent],
  animations: [fadeInUp],
  templateUrl: './agendar-cita.html',
  styleUrl: './agendar-cita.css'
})
export class RecepcionComponent implements OnInit {
  panel: Record<string, number> = {};
  citas: any[] = [];
  etapasFlujo: EtapaFlujoRecepcion[] = [];
  mascotas: Mascota[] = [];
  veterinarios: Usuario[] = [];
  cargando = false;
  formularioAbierto = false;
  editorFlujoAbierto = false;
  etapaEditandoId: number | null = null;
  etapaGuardando = false;
  estadosCambiando = new Set<number>();
  confirmandoEliminacion: number | null = null;
  editandoId: number | null = null;
  mensaje = '';
  form = this.formularioVacio();
  etapaForm = this.etapaVacia();
  coloresFlujo: EtapaFlujoRecepcion['color'][] = ['cyan', 'emerald', 'amber', 'rose', 'violet', 'blue', 'slate'];
  clasesColor: Record<EtapaFlujoRecepcion['color'], string> = {
    cyan: 'border-cyan-200 bg-cyan-50 text-cyan-900 dark:border-cyan-900 dark:bg-cyan-950/30 dark:text-cyan-200',
    emerald: 'border-emerald-200 bg-emerald-50 text-emerald-900 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-200',
    amber: 'border-amber-200 bg-amber-50 text-amber-900 dark:border-amber-900 dark:bg-amber-950/30 dark:text-amber-200',
    rose: 'border-rose-200 bg-rose-50 text-rose-900 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-200',
    violet: 'border-violet-200 bg-violet-50 text-violet-900 dark:border-violet-900 dark:bg-violet-950/30 dark:text-violet-200',
    blue: 'border-blue-200 bg-blue-50 text-blue-900 dark:border-blue-900 dark:bg-blue-950/30 dark:text-blue-200',
    slate: 'border-slate-200 bg-slate-50 text-slate-900 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200'
  };
  constructor(private api: ApiService, private notificaciones: NotificacionService) {}
  ngOnInit() {
    this.cargar();
    this.api.mascotas().subscribe({
      next: r => this.mascotas = r.data,
      error: () => this.mensaje = 'No se pudieron cargar mascotas. Revisa backend y permisos.'
    });
    this.api.veterinarios().subscribe({
      next: r => this.veterinarios = r.data,
      error: () => this.mensaje = 'No se pudieron cargar veterinarios.'
    });
  }

  cargar() {
    this.cargando = true;
    this.api.recepcion().subscribe({
      next: r => this.panel = r.data,
      error: () => this.mensaje = 'No se pudo cargar el panel de recepcion.'
    });
    this.api.flujoRecepcion().subscribe({
      next: r => {
        this.etapasFlujo = [...(r.data || [])].sort((a, b) => a.posicion - b.posicion);
        if (this.form.estado && !this.estadosActivos.includes(this.form.estado)) {
          this.form.estado = this.estadosActivos[0] || 'Programada';
        }
      },
      error: () => this.mensaje = 'No se pudo cargar el flujo de admision.'
    });
    this.api.citas().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => this.citas = r.data,
      error: () => {
        this.citas = [];
        this.mensaje = 'No se pudieron cargar citas. Verifica que Spring Boot este ejecutandose en el puerto 8080.';
      }
    });
  }

  nueva() {
    this.editandoId = null;
    this.form = this.formularioVacio();
    this.form.mascotaId = this.mascotas[0]?.id || null;
    this.form.veterinarioId = this.veterinarios[0]?.id || null;
    this.formularioAbierto = true;
    this.mensaje = '';
  }

  editar(cita: any) {
    this.editandoId = cita.id;
    this.form = {
      fechaHora: cita.fechaHora ? cita.fechaHora.slice(0, 16) : '',
      motivo: cita.motivo || '',
      estado: cita.estado || 'Programada',
      observaciones: cita.observaciones || '',
      mascotaId: cita.mascota?.id || null,
      veterinarioId: cita.veterinario?.id || null
    };
    this.formularioAbierto = true;
    this.mensaje = '';
  }

  get estadosActivos(): string[] {
    return this.etapasFlujo.filter(etapa => etapa.activa).map(etapa => etapa.estadoCita);
  }

  contarCitas(estado: string): number {
    return this.citas.filter(cita => (cita.estado || '').toLocaleLowerCase() === estado.toLocaleLowerCase()).length;
  }

  nuevaEtapa() {
    this.etapaEditandoId = null;
    this.etapaForm = this.etapaVacia();
    this.editorFlujoAbierto = true;
    this.mensaje = '';
  }

  editarEtapa(etapa: EtapaFlujoRecepcion) {
    this.etapaEditandoId = etapa.id;
    this.etapaForm = { ...etapa, posicion: etapa.posicion };
    this.editorFlujoAbierto = true;
    this.mensaje = '';
  }

  guardarEtapa() {
    if (!this.etapaForm.nombre.trim() || !this.etapaForm.estadoCita.trim()) {
      this.mensaje = 'Completa el nombre y el estado de la cita.';
      return;
    }
    this.etapaGuardando = true;
    const datos = { ...this.etapaForm, nombre: this.etapaForm.nombre.trim(), estadoCita: this.etapaForm.estadoCita.trim() };
    const solicitud = this.etapaEditandoId
      ? this.api.actualizarEtapaFlujo(this.etapaEditandoId, datos)
      : this.api.crearEtapaFlujo(datos);
    solicitud.pipe(finalize(() => this.etapaGuardando = false)).subscribe({
      next: () => {
        this.editorFlujoAbierto = false;
        this.notificaciones.mostrarExito(this.etapaEditandoId ? 'Etapa actualizada.' : 'Etapa creada.');
        this.cargarFlujo();
        this.cargarCitas();
      },
      error: error => {
        this.mensaje = error?.error?.mensaje || 'No se pudo guardar la etapa del flujo.';
      }
    });
  }

  moverEtapa(etapa: EtapaFlujoRecepcion, desplazamiento: number) {
    const indice = this.etapasFlujo.findIndex(item => item.id === etapa.id);
    const destino = indice + desplazamiento;
    if (indice < 0 || destino < 0 || destino >= this.etapasFlujo.length) return;
    this.etapaGuardando = true;
    this.api.actualizarEtapaFlujo(etapa.id, {
      nombre: etapa.nombre, estadoCita: etapa.estadoCita, descripcion: etapa.descripcion || '', color: etapa.color,
      posicion: destino + 1, activa: etapa.activa
    }).pipe(finalize(() => this.etapaGuardando = false)).subscribe({
      next: () => this.cargarFlujo(),
      error: () => this.mensaje = 'No se pudo actualizar el orden de las etapas.'
    });
  }

  solicitarEliminarEtapa(etapa: EtapaFlujoRecepcion) {
    this.confirmandoEliminacion = etapa.id;
  }

  eliminarEtapa(etapa: EtapaFlujoRecepcion) {
    this.api.eliminarEtapaFlujo(etapa.id).subscribe({
      next: () => {
        this.confirmandoEliminacion = null;
        this.notificaciones.mostrarExito('Etapa eliminada.');
        this.cargarFlujo();
      },
      error: error => {
        this.confirmandoEliminacion = null;
        this.notificaciones.mostrarError(error?.error?.mensaje || 'No se pudo eliminar. Si tiene citas asignadas, desactiva la etapa.');
      }
    });
  }

  cambiarEstadoEtapa(etapa: EtapaFlujoRecepcion, activa: boolean) {
    if (activa && !this.estadosActivos.length) return;
    const etapaActiva = this.etapasFlujo.filter(item => item.activa).length;
    if (!activa && etapaActiva <= 1) {
      this.notificaciones.mostrarInfo('El flujo debe conservar al menos una etapa activa.');
      return;
    }
    this.api.actualizarEtapaFlujo(etapa.id, {
      nombre: etapa.nombre, estadoCita: etapa.estadoCita, descripcion: etapa.descripcion || '', color: etapa.color,
      posicion: etapa.posicion, activa
    }).subscribe({
      next: () => { this.notificaciones.mostrarExito(activa ? 'Etapa activada.' : 'Etapa desactivada.'); this.cargarFlujo(); },
      error: () => this.notificaciones.mostrarError('No se pudo cambiar la etapa.')
    });
  }

  cambiarEstadoCita(cita: any, estadoNuevo: string) {
    const estadoAnterior = cita.estado || '';
    if (!estadoNuevo || estadoNuevo === estadoAnterior || this.estadosCambiando.has(cita.id)) return;
    this.estadosCambiando.add(cita.id);
    const payload = {
      fechaHora: cita.fechaHora,
      motivo: cita.motivo,
      estado: estadoNuevo,
      observaciones: cita.observaciones || '',
      mascota: cita.mascota?.id ? { id: cita.mascota.id } : null,
      veterinario: cita.veterinario?.id ? { id: cita.veterinario.id } : null,
      recepcionista: cita.recepcionista?.id ? { id: cita.recepcionista.id } : null
    };
    this.api.actualizarCita(cita.id, payload).pipe(finalize(() => this.estadosCambiando.delete(cita.id))).subscribe({
      next: respuesta => {
        cita.estado = respuesta.data?.estado || estadoNuevo;
        this.notificaciones.mostrarExito(`Estado de la cita actualizado a ${cita.estado}.`);
      },
      error: error => {
        cita.estado = estadoAnterior;
        this.notificaciones.mostrarError(error?.error?.mensaje || 'No se pudo actualizar el estado de la cita.');
      }
    });
  }

  private cargarFlujo() {
    this.api.flujoRecepcion().subscribe({ next: r => this.etapasFlujo = [...r.data].sort((a, b) => a.posicion - b.posicion) });
  }

  private cargarCitas() {
    this.api.citas().subscribe({ next: r => this.citas = r.data || [] });
  }

  guardar() {
    const payload = {
      fechaHora: this.form.fechaHora,
      motivo: this.form.motivo,
      estado: this.form.estado,
      observaciones: this.form.observaciones,
      mascota: { id: this.form.mascotaId },
      veterinario: { id: this.form.veterinarioId }
    };

    if (this.editandoId) {
      this.api.actualizarCita(this.editandoId, payload).subscribe({
        next: () => {
          this.mensaje = 'Cita actualizada correctamente.';
          this.notificaciones.mostrarExito('Cita actualizada correctamente.');
          this.formularioAbierto = false;
          this.editandoId = null;
          this.cargar();
        },
        error: () => this.mensaje = 'No se pudo actualizar la cita. Revisa mascota, veterinario y permisos.'
      });
    } else {
      this.api.crearCita(payload).subscribe({
        next: () => {
          this.mensaje = 'Cita creada correctamente.';
          this.notificaciones.mostrarExito('Cita creada correctamente.');
          this.formularioAbierto = false;
          this.cargar();
        },
        error: () => this.mensaje = 'No se pudo crear la cita. Revisa mascota, veterinario y permisos.'
      });
    }
  }

  cerrar() {
    this.formularioAbierto = false;
    this.editandoId = null;
  }

  formularioVacio() {
    const fecha = new Date();
    fecha.setHours(fecha.getHours() + 1);
    return {
      fechaHora: fecha.toISOString().slice(0, 16),
      motivo: 'Consulta general',
      estado: 'Programada',
      observaciones: '',
      mascotaId: null as number | null,
      veterinarioId: null as number | null
    };
  }

  private etapaVacia(): Omit<EtapaFlujoRecepcion, 'id'> {
    return {
      nombre: '', estadoCita: '', descripcion: '', color: 'cyan',
      posicion: this.etapasFlujo.length + 1, activa: true
    };
  }
}
