import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { Cliente, Mascota } from '../../core/modelos';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { IconoComponent } from '../../shared/components/icono/icono';
import { AutorizacionService } from '../../core/autorizacion.service';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { NotificacionService } from '../../core/notificacion.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-mascotas',
  standalone: true,
  imports: [CommonModule, FormsModule, IconoComponent, CargadorComponent],
  animations: [fadeInUp],
  templateUrl: './mascotas.html',
  styleUrl: './mascotas.css'
})
export class MascotasComponent implements OnInit, OnDestroy {
  mascotas: Mascota[] = [];
  clientes: Cliente[] = [];
  cargando = false;
  seleccionada?: Mascota;
  mascotaPendienteEliminar?: Mascota;
  archivoPendienteEliminar?: { mascota: Mascota; historialId: number; nombre: string };
  procesandoEliminacionArchivo = false;
  mensajeArchivo = '';
  mensajeFormulario = '';
  busqueda = '';
  formularioAbierto = false;
  editandoId?: number;
  foto?: File | null;
  form = this.formularioVacio();
  puedeGestionarClientes = false;
  private fotosCargadas = new Map<number, string>();

  constructor(private api: ApiService, private autorizacion: AutorizacionService, private notificaciones: NotificacionService) {}

  ngOnInit() {
    this.cargar();
    this.puedeGestionarClientes = this.autorizacion.tieneRol(['ROLE_ADMIN', 'ROLE_RECEPCIONISTA', 'ROLE_VETERINARIO']);
    if (!this.puedeGestionarClientes) return;
    this.api.clientes().subscribe({
      next: r => {
        this.clientes = r.data;
        if (!this.form.propietarioId && this.clientes.length) this.form.propietarioId = this.clientes[0].id;
      },
      error: () => {
        this.clientes = [];
      }
    });
  }

  ngOnDestroy() {
    this.limpiarFotos();
  }

  cargar() {
    this.cargando = true;
    this.api.mascotas().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => {
        this.mascotas = r.data;
        this.cargarFotos(r.data);
        if (!this.seleccionada && r.data.length) this.seleccionada = r.data[0];
        if (this.seleccionada) this.seleccionada = r.data.find(m => m.id === this.seleccionada?.id) || r.data[0];
      },
      error: () => {
        this.mascotas = [];
        this.seleccionada = undefined;
        this.mensajeFormulario = 'No se pudieron cargar mascotas. Revisa que el backend este encendido y tu rol tenga permiso.';
      }
    });
  }

  get filtradas() {
    const q = this.busqueda.toLowerCase();
    return this.mascotas.filter(m => `${m.nombre} ${m.especie} ${m.raza} ${m.propietario?.nombres} ${m.propietario?.apellidos}`.toLowerCase().includes(q));
  }

  seleccionarMascota(mascota: Mascota) {
    this.seleccionada = mascota;
  }

  nuevo() {
    this.editandoId = undefined;
    this.form = this.formularioVacio();
    this.form.propietarioId = this.clientes[0]?.id || null;
    this.foto = null;
    this.mensajeFormulario = '';
    this.formularioAbierto = true;
  }

  editar(mascota: Mascota) {
    this.editandoId = mascota.id;
    this.form = {
      nombre: mascota.nombre,
      especie: mascota.especie || 'Canino',
      raza: mascota.raza,
      sexo: mascota.sexo || 'Macho',
      color: mascota.color,
      pesoKg: mascota.pesoKg,
      fechaNacimiento: mascota.fechaNacimiento,
      propietarioId: mascota.propietario?.id || null,
      estado: mascota.estado || 'ACTIVA'
    };
    this.mensajeFormulario = '';
    this.formularioAbierto = true;
  }

  guardar() {
    const datos = { ...this.form, pesoKg: Number(this.form.pesoKg || 0), fechaNacimiento: this.form.fechaNacimiento || null };
    if (this.editandoId) {
      this.api.actualizarMascota(this.editandoId, datos).subscribe({
        next: r => {
          this.mensajeFormulario = 'Mascota actualizada correctamente.';
          this.seleccionada = r.data;
          this.cargar();
          this.cerrarFormulario();
        },
        error: () => this.mensajeFormulario = 'No se pudo actualizar la mascota.'
      });
      return;
    }
    this.api.crearMascota(datos, this.foto).subscribe({
      next: r => {
        this.mensajeFormulario = 'Mascota registrada correctamente.';
        this.seleccionada = r.data;
        this.cargar();
        this.cerrarFormulario();
      },
      error: () => this.mensajeFormulario = 'No se pudo registrar la mascota.'
    });
  }

  eliminar(mascota: Mascota) {
    this.mascotaPendienteEliminar = mascota;
  }

  cancelarEliminacion() {
    this.mascotaPendienteEliminar = undefined;
  }

  confirmarEliminacion() {
    const mascota = this.mascotaPendienteEliminar;
    if (!mascota) return;
    this.api.eliminarMascota(mascota.id).subscribe({
      next: () => {
        this.mascotas = this.mascotas.filter(m => m.id !== mascota.id);
        this.seleccionada = this.mascotas[0];
        this.mascotaPendienteEliminar = undefined;
        this.notificaciones.mostrarExito(`${mascota.nombre} fue archivada. Su historial clínico se conserva.`);
        if (this.editandoId === mascota.id) this.cerrarFormulario();
      },
      error: e => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo eliminar la mascota.')
    });
  }

  cerrarFormulario() {
    this.formularioAbierto = false;
    this.editandoId = undefined;
    this.mensajeFormulario = '';
  }

  /** Cierra el panel de alta/edición con la tecla Escape. */
  @HostListener('document:keydown.escape')
  cerrarFormularioConEscape() {
    if (this.archivoPendienteEliminar) {
      this.cancelarEliminacionArchivo();
      return;
    }
    if (this.mascotaPendienteEliminar) {
      this.cancelarEliminacion();
      return;
    }
    if (this.formularioAbierto) this.cerrarFormulario();
  }

  fotoFormulario(evento: Event) {
    this.foto = (evento.target as HTMLInputElement).files?.[0] || null;
  }

  formularioVacio() {
    return {
      nombre: '',
      especie: 'Canino',
      raza: '',
      sexo: 'Macho',
      color: '',
      pesoKg: 0,
      fechaNacimiento: '',
      propietarioId: null as number | null,
      estado: 'ACTIVA'
    };
  }

  fotoMascota(mascota: Mascota) {
    const foto = mascota.fotoUrl;
    if (!foto) return this.fotoPredeterminada;
    if (this.esArchivoSubido(foto)) return this.fotosCargadas.get(mascota.id) || this.fotoPredeterminada;
    return foto;
  }

  get fotoPredeterminada() {
    return 'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=900';
  }

  imagenFallida(evento: Event) {
    const imagen = evento.target as HTMLImageElement;
    if (imagen.src !== this.fotoPredeterminada) imagen.src = this.fotoPredeterminada;
  }

  abrirArchivo(url?: string) {
    if (!url) return;
    // Abrir la pestaña desde el gesto del usuario evita que el navegador
    // bloquee la vista mientras se descarga el archivo con el token.
    const ventana = window.open('about:blank', '_blank');
    if (ventana) {
      ventana.opener = null;
      ventana.document.title = 'Abriendo archivo clínico';
      ventana.document.body.textContent = 'Cargando archivo clínico…';
    }
    try {
      this.api.obtenerArchivo(url).subscribe({
        next: blob => {
          const objectUrl = URL.createObjectURL(blob);
          if (ventana && !ventana.closed) ventana.location.replace(objectUrl);
          else this.notificaciones.mostrarInfo('El navegador bloqueó la pestaña del archivo. Permite ventanas emergentes e inténtalo de nuevo.');
          window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
        },
        error: error => {
          ventana?.close();
          this.notificaciones.mostrarError(error?.error?.mensaje || 'No se pudo abrir el archivo. Inicia sesión de nuevo si tu sesión venció.');
        }
      });
    } catch {
      ventana?.close();
      this.notificaciones.mostrarError('La ruta del archivo no es válida.');
    }
  }

  private cargarFotos(mascotas: Mascota[]) {
    this.limpiarFotos();
    for (const mascota of mascotas) {
      if (!mascota.fotoUrl || !this.esArchivoSubido(mascota.fotoUrl)) continue;
      this.api.obtenerArchivo(mascota.fotoUrl).subscribe({
        next: blob => {
          if (!blob.type.startsWith('image/')) return;
          const anterior = this.fotosCargadas.get(mascota.id);
          if (anterior) URL.revokeObjectURL(anterior);
          this.fotosCargadas.set(mascota.id, URL.createObjectURL(blob));
        },
        error: () => this.fotosCargadas.delete(mascota.id)
      });
    }
  }

  private limpiarFotos() {
    this.fotosCargadas.forEach(url => URL.revokeObjectURL(url));
    this.fotosCargadas.clear();
  }

  private esArchivoSubido(url: string) {
    try {
      return new URL(url, `${environment.apiUrl}/`).pathname.startsWith('/uploads/');
    } catch {
      return false;
    }
  }

  eventos(mascota: Mascota) {
    return [
      ...(mascota.historiales || []).map(h => ({ fecha: h.fecha, tipo: h.tipoEvento, titulo: h.motivo, detalle: `${h.diagnostico || ''} Tratamiento: ${h.tratamiento || ''}` })),
      ...(mascota.cirugias || []).map(c => ({ fecha: c.fechaProgramada, tipo: 'CIRUGIA', titulo: c.procedimiento, detalle: `${c.tipoAnestesia || ''}. ${c.estadoPostoperatorio || ''}` }))
    ].sort((a, b) => +new Date(b.fecha) - +new Date(a.fecha));
  }

  seleccionar(evento: Event) {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (archivo) this.subir(archivo);
    input.value = '';
  }

  soltar(evento: DragEvent) {
    evento.preventDefault();
    const archivo = evento.dataTransfer?.files?.[0];
    if (archivo) this.subir(archivo);
  }

  subir(archivo: File) {
    if (!this.seleccionada) return;
    const historial = {
      fecha: new Date().toISOString().slice(0, 19),
      tipoEvento: 'ARCHIVO MEDICO',
      motivo: `Archivo cargado: ${archivo.name}`,
      diagnostico: 'Documento adjunto al historial clinico del paciente.',
      tratamiento: 'Revision documental pendiente en consulta.',
      vacunas: '',
      temperatura: 0,
      frecuenciaCardiaca: 0,
      frecuenciaRespiratoria: 0,
      veterinarioId: null
    };
    this.api.agregarHistorialMascota(this.seleccionada.id, historial, archivo).subscribe({
      next: r => {
        this.mensajeArchivo = `Archivo guardado en historial: ${this.nombreArchivo(r.data.archivosUrl)}`;
        this.cargar();
      },
      error: e => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo subir el archivo al historial.')
    });
  }

  eliminarArchivo(mascota: Mascota, historialId: number, nombre: string) {
    this.archivoPendienteEliminar = { mascota, historialId, nombre };
  }

  cancelarEliminacionArchivo() {
    if (this.procesandoEliminacionArchivo) return;
    this.archivoPendienteEliminar = undefined;
  }

  confirmarEliminacionArchivo() {
    const archivo = this.archivoPendienteEliminar;
    if (!archivo || this.procesandoEliminacionArchivo) return;
    this.procesandoEliminacionArchivo = true;
    this.api.eliminarArchivoMascota(archivo.mascota.id, archivo.historialId).pipe(
      finalize(() => this.procesandoEliminacionArchivo = false)
    ).subscribe({
      next: () => {
        this.mensajeArchivo = 'Archivo eliminado del historial.';
        this.archivoPendienteEliminar = undefined;
        this.cargar();
      },
      error: e => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo eliminar el archivo del historial.')
    });
  }

  descargarPdf(id: number) { this.api.descargar(`/reportes/mascotas/${id}/historial.pdf`, `historial-${id}.pdf`); }
  descargarExcel() { this.api.descargar('/reportes/finanzas.xlsx', 'reporte-financiero-vetsphere.xlsx'); }

  archivosClinicos(mascota: Mascota) {
    return (mascota.historiales || []).filter(h => !!h.archivosUrl);
  }

  nombreArchivo(url?: string) {
    if (!url) return 'archivo-medico';
    return decodeURIComponent(url.split('/').pop() || 'archivo-medico');
  }

  iconoArchivo(url?: string) {
    const nombre = (url || '').toLowerCase();
    if (nombre.endsWith('.pdf')) return 'PDF';
    if (/\.(png|jpg|jpeg|webp)$/i.test(nombre)) return 'IMG';
    return 'DOC';
  }
}
