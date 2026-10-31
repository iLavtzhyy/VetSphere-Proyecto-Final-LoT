import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { Mascota, OrdenLaboratorio, Usuario, Factura } from '../../core/modelos';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { IconoComponent } from '../../shared/components/icono/icono';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { NotificacionService } from '../../core/notificacion.service';

@Component({
  selector: 'app-laboratorio',
  standalone: true,
  imports: [CommonModule, FormsModule, IconoComponent, CargadorComponent],
  animations: [fadeInUp],
  templateUrl: './laboratorio.html',
  styleUrl: './laboratorio.css'
})
export class LaboratorioComponent implements OnInit {
  ordenes: OrdenLaboratorio[] = [];
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
    this.api.laboratorio().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => this.ordenes = r.data,
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudieron cargar las órdenes de laboratorio. Revisa que el backend esté activo.')
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
    this.form.veterinarioId = this.veterinarios[0]?.id || null;
    this.formularioAbierto = true;
    this.mensaje = '';
  }

  editar(orden: OrdenLaboratorio) {
    this.editandoId = orden.id;
    this.form = {
      mascotaId: orden.mascota?.id || null,
      veterinarioId: orden.veterinarioSolicitante?.id || null,
      tipoPrueba: orden.tipoPrueba || '',
      resultado: orden.resultado || '',
      estado: orden.estado || 'Pendiente',
      archivoResultadoUrl: orden.archivoResultadoUrl || '',
      precio: orden.precio || 35.00
    };
    this.formularioAbierto = true;
    this.mensaje = '';
  }

  guardar() {
    if (this.editandoId) {
      this.api.actualizarOrdenLaboratorio(this.editandoId, { ...this.form }).subscribe({
        next: () => {
          this.mensaje = 'Orden actualizada correctamente.';
          this.notificaciones.mostrarExito('Orden actualizada correctamente.');
          this.cargar();
          this.cerrar();
        },
        error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo actualizar la orden.')
      });
      return;
    }
    this.api.crearOrdenLaboratorio({
      fecha: new Date().toISOString().slice(0, 19),
      tipoPrueba: this.form.tipoPrueba,
      resultado: this.form.resultado,
      estado: this.form.estado,
      archivoResultadoUrl: this.form.archivoResultadoUrl,
      precio: this.form.precio,
      mascota: { id: this.form.mascotaId },
      veterinarioSolicitante: { id: this.form.veterinarioId }
    }).subscribe({
      next: () => {
        this.mensaje = 'Orden creada correctamente.';
        this.notificaciones.mostrarExito('Orden creada correctamente.');
        this.cargar();
        this.cerrar();
      },
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo crear la orden.')
    });
  }

  eliminar(orden: OrdenLaboratorio) {
    if (!confirm(`Eliminar orden ${orden.tipoPrueba}?`)) return;
    this.api.eliminarOrdenLaboratorio(orden.id).subscribe({
      next: () => this.ordenes = this.ordenes.filter(o => o.id !== orden.id),
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo eliminar la orden.')
    });
  }

  cerrar() {
    this.formularioAbierto = false;
    this.mensaje = '';
  }

  formularioVacio() {
    return { mascotaId: null as number | null, veterinarioId: null as number | null, tipoPrueba: 'Hemograma completo', resultado: '', estado: 'Pendiente', archivoResultadoUrl: '', precio: 35.00 };
  }

  contar(estado: string) { return this.ordenes.filter(o => (o.estado || '').toLowerCase().includes(estado.toLowerCase())).length; }
  nombreUsuario(usuario?: Usuario) { return usuario ? `${usuario.nombres} ${usuario.apellidos}` : 'Sin veterinario'; }
  badgeEstado(estado?: string) {
    const e = (estado || '').toLowerCase();
    if (e.includes('entregado')) return 'badge-ok';
    if (e.includes('repetir')) return 'badge-danger';
    if (e.includes('proces')) return 'badge-info';
    return 'badge-warn';
  }
  get filtradas() {
    const q = this.busqueda.toLowerCase();
    return this.ordenes.filter(o => `${o.tipoPrueba} ${o.estado} ${o.resultado} ${o.mascota?.nombre}`.toLowerCase().includes(q));
  }
}
