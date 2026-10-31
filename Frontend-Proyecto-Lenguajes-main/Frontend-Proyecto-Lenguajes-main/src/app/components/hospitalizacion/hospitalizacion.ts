import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { Hospitalizacion, Mascota, Usuario, Factura, Producto } from '../../core/modelos';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { IconoComponent } from '../../shared/components/icono/icono';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { NotificacionService } from '../../core/notificacion.service';

@Component({
  selector: 'app-hospitalizacion',
  standalone: true,
  imports: [CommonModule, FormsModule, IconoComponent, CargadorComponent],
  animations: [fadeInUp],
  templateUrl: './hospitalizacion.html',
  styleUrl: './hospitalizacion.css'
})
export class HospitalizacionComponent implements OnInit {
  hospitalizaciones: Hospitalizacion[] = [];
  mascotas: Mascota[] = [];
  veterinarios: Usuario[] = [];
  facturas: Factura[] = [];
  cargando = false;
  busqueda = '';
  formularioAbierto = false;
  editandoId?: number;
  mensaje = '';
  form = this.formularioVacio();
  hospitalizacionCuenta: Hospitalizacion | null = null;
  cuenta: any = null;
  productos: Producto[] = [];
  consumo = { productoCodigo: '', cantidad: 1 };
  procesandoCuenta = false;

  constructor(private api: ApiService, private notificaciones: NotificacionService) {}

  ngOnInit() {
    this.cargar();
    this.api.productos().subscribe({ next: r => this.productos = r.data || [], error: () => this.productos = [] });
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
    this.api.hospitalizaciones().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => this.hospitalizaciones = r.data,
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudieron cargar las hospitalizaciones. Revisa que el backend esté activo.')
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

  abrirCuenta(h: Hospitalizacion) {
    this.hospitalizacionCuenta = h;
    this.cuenta = null;
    this.consumo = { productoCodigo: '', cantidad: 1 };
    this.api.cuentaHospitalizacion(h.id).subscribe({
      next: r => this.cuenta = r.data,
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo cargar la cuenta acumulada.')
    });
  }

  agregarConsumo() {
    if (!this.hospitalizacionCuenta || !this.consumo.productoCodigo || this.consumo.cantidad < 1) return;
    this.procesandoCuenta = true;
    this.api.agregarConsumoHospitalizacion(this.hospitalizacionCuenta.id, this.consumo.productoCodigo, this.consumo.cantidad)
      .pipe(finalize(() => this.procesandoCuenta = false)).subscribe({
        next: r => { this.cuenta = r.data; this.consumo = { productoCodigo: '', cantidad: 1 }; this.notificaciones.mostrarExito('Consumo agregado a la cuenta. El inventario fue actualizado.'); },
        error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo agregar el consumo a la cuenta.')
      });
  }

  darAlta() {
    if (!this.hospitalizacionCuenta || this.procesandoCuenta) return;
    this.procesandoCuenta = true;
    this.api.altaHospitalizacion(this.hospitalizacionCuenta.id).pipe(finalize(() => this.procesandoCuenta = false)).subscribe({
      next: r => {
        this.notificaciones.mostrarExito(`Alta registrada. Factura ${r.data?.numero || ''} por $${Number(r.data?.total || this.cuenta?.total || 0).toFixed(2)}.`);
        this.hospitalizacionCuenta = null; this.cuenta = null; this.cargar();
      },
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo cerrar la cuenta y emitir la factura.')
    });
  }

  esEstanciaActiva(h: Hospitalizacion) {
    return !h.facturaId && !h.alta && !/alta/i.test(h.estado || '');
  }

  obtenerEstadoPago(facturaId?: number): string {
    if (!facturaId) return 'Pendiente';
    const f = this.facturas.find(x => x.id === facturaId);
    return f ? f.estadoPago : 'Pendiente';
  }

  nuevoIngreso() {
    this.editandoId = undefined;
    this.form = this.formularioVacio();
    this.form.mascotaId = this.mascotas[0]?.id || null;
    this.form.veterinarioId = this.veterinarios[0]?.id || null;
    this.formularioAbierto = true;
    this.mensaje = '';
  }

  editar(h: Hospitalizacion) {
    this.editandoId = h.id;
    this.form = {
      mascotaId: h.mascota?.id || null,
      veterinarioId: h.veterinarioResponsable?.id || null,
      jaula: h.jaula || '',
      motivo: h.motivo || '',
      planCuidados: h.planCuidados || '',
      estado: h.estado || 'Internado',
      alta: h.alta ? h.alta.slice(0, 16) : '',
      precio: h.precio || 45.00
    };
    this.formularioAbierto = true;
    this.mensaje = '';
  }

  guardar() {
    if (this.editandoId && this.form.estado.toLowerCase() === 'alta') {
      const hospitalizacion = this.hospitalizaciones.find(h => h.id === this.editandoId);
      if (hospitalizacion && this.esEstanciaActiva(hospitalizacion)) {
        this.formularioAbierto = false;
        this.abrirCuenta(hospitalizacion);
        return;
      }
    }
    if (this.editandoId) {
      this.api.actualizarHospitalizacion(this.editandoId, { ...this.form, alta: this.form.alta || null }).subscribe({
        next: () => {
          this.mensaje = 'Hospitalizacion actualizada correctamente.';
          this.notificaciones.mostrarExito('Hospitalización actualizada correctamente.');
          this.cargar();
          this.cerrar();
        },
        error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo actualizar la hospitalización.')
      });
      return;
    }
    this.api.crearHospitalizacion({
      ingreso: new Date().toISOString().slice(0, 19),
      jaula: this.form.jaula,
      motivo: this.form.motivo,
      planCuidados: this.form.planCuidados,
      estado: this.form.estado,
      alta: this.form.alta || null,
      precio: this.form.precio,
      mascota: { id: this.form.mascotaId },
      veterinarioResponsable: { id: this.form.veterinarioId }
    }).subscribe({
      next: () => {
        this.mensaje = 'Ingreso registrado correctamente.';
        this.notificaciones.mostrarExito('Ingreso registrado correctamente.');
        this.cargar();
        this.cerrar();
      },
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo registrar el ingreso.')
    });
  }

  eliminar(h: Hospitalizacion) {
    if (!confirm(`Eliminar hospitalizacion de ${h.mascota?.nombre || 'paciente'}?`)) return;
    this.api.eliminarHospitalizacion(h.id).subscribe({
      next: () => this.hospitalizaciones = this.hospitalizaciones.filter(i => i.id !== h.id),
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo eliminar la hospitalización.')
    });
  }

  cerrar() {
    this.formularioAbierto = false;
    this.mensaje = '';
  }

  formularioVacio() {
    return { mascotaId: null as number | null, veterinarioId: null as number | null, jaula: 'H-01', motivo: '', planCuidados: '', estado: 'Internado', alta: '', precio: 45.00 };
  }

  contar(estado: string) { return this.hospitalizaciones.filter(h => (h.estado || '').toLowerCase().includes(estado.toLowerCase())).length; }
  nombreUsuario(usuario?: Usuario) { return usuario ? `${usuario.nombres} ${usuario.apellidos}` : 'Sin responsable'; }
  badgeEstado(estado?: string) {
    const e = (estado || '').toLowerCase();
    if (e.includes('alta')) return 'badge-ok';
    if (e.includes('crit')) return 'badge-danger';
    if (e.includes('observ')) return 'badge-info';
    return 'badge-warn';
  }
  get filtradas() {
    const q = this.busqueda.toLowerCase();
    return this.hospitalizaciones.filter(h => `${h.mascota?.nombre} ${h.jaula} ${h.motivo} ${h.estado}`.toLowerCase().includes(q));
  }
}
