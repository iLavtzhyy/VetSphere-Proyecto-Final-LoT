import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { Cirugia, Mascota, Usuario, Factura } from '../../core/modelos';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { IconoComponent } from '../../shared/components/icono/icono';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { NotificacionService } from '../../core/notificacion.service';

@Component({
  selector: 'app-cirugias',
  standalone: true,
  imports: [CommonModule, FormsModule, IconoComponent, CargadorComponent],
  animations: [fadeInUp],
  templateUrl: './cirugias.html',
  styleUrl: './cirugias.css'
})
export class CirugiasComponent implements OnInit {
  cirugias: Cirugia[] = [];
  mascotas: Mascota[] = [];
  veterinarios: Usuario[] = [];
  facturas: Factura[] = [];
  cargando = false;
  busqueda = '';
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
    this.api.cirugias().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => this.cirugias = r.data,
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudieron cargar las cirugías. Revisa que el backend esté activo.')
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

  nueva() {
    this.editandoId = undefined;
    this.form = this.formularioVacio();
    this.form.mascotaId = this.mascotas[0]?.id || null;
    this.form.cirujanoId = this.veterinarios[0]?.id || null;
    this.formularioAbierto = true;
    this.mensaje = '';
  }

  editar(cirugia: Cirugia) {
    this.editandoId = cirugia.id;
    this.form = {
      mascotaId: cirugia.mascota?.id || this.mascotas[0]?.id || null,
      cirujanoId: cirugia.cirujano?.id || this.veterinarios[0]?.id || null,
      fechaProgramada: this.fechaInput(cirugia.fechaProgramada),
      procedimiento: cirugia.procedimiento || '',
      tipoAnestesia: cirugia.tipoAnestesia || '',
      cirujanoPrincipal: cirugia.cirujanoPrincipal || this.nombreUsuario(cirugia.cirujano),
      constantesVitales: cirugia.constantesVitales || '',
      estadoPostoperatorio: cirugia.estadoPostoperatorio || '',
      estado: cirugia.estado || 'Pendiente',
      notas: cirugia.notas || '',
      precio: cirugia.precio || 280.00
    };
    this.formularioAbierto = true;
    this.mensaje = '';
  }

  guardar() {
    const datos = { ...this.form };
    const peticion = this.editandoId ? this.api.actualizarCirugia(this.editandoId, datos) : this.api.crearCirugia(datos);
    peticion.subscribe({
      next: () => {
        this.mensaje = 'Cirugia guardada correctamente.';
        this.notificaciones.mostrarExito('Cirugía guardada correctamente.');
        this.cargar();
        this.cerrar();
      },
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo guardar la cirugía.')
    });
  }

  eliminar(cirugia: Cirugia) {
    if (!confirm(`Eliminar cirugia ${cirugia.procedimiento}?`)) return;
    this.api.eliminarCirugia(cirugia.id).subscribe({
      next: () => this.cirugias = this.cirugias.filter(c => c.id !== cirugia.id),
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo eliminar la cirugía.')
    });
  }

  cerrar() {
    this.formularioAbierto = false;
    this.mensaje = '';
  }

  formularioVacio() {
    return {
      mascotaId: null as number | null,
      cirujanoId: null as number | null,
      fechaProgramada: new Date().toISOString().slice(0, 16),
      procedimiento: '',
      tipoAnestesia: 'General inhalatoria',
      cirujanoPrincipal: '',
      constantesVitales: 'FC 90 / FR 24 / Temp 38.4',
      estadoPostoperatorio: '',
      estado: 'Pendiente',
      notas: '',
      precio: 280.00
    };
  }

  fechaInput(fecha?: string) {
    return fecha ? fecha.slice(0, 16) : new Date().toISOString().slice(0, 16);
  }

  nombreUsuario(usuario?: Usuario) {
    return usuario ? `${usuario.nombres} ${usuario.apellidos}` : 'Sin asignar';
  }

  contarEstado(estado: string) {
    return this.cirugias.filter(c => (c.estado || '').toLowerCase().includes(estado.toLowerCase())).length;
  }

  get postoperatorios() {
    return this.cirugias.filter(c => (c.estado || '').toLowerCase().includes('post')).length;
  }

  badgeEstado(estado?: string) {
    const e = (estado || '').toLowerCase();
    if (e.includes('complet')) return 'badge-ok';
    if (e.includes('cancel')) return 'badge-danger';
    if (e.includes('post')) return 'badge-info';
    return 'badge-warn';
  }

  get filtradas() {
    const q = this.busqueda.toLowerCase();
    return this.cirugias.filter(c => `${c.procedimiento} ${c.estado} ${c.mascota?.nombre} ${c.cirujanoPrincipal}`.toLowerCase().includes(q));
  }
}
