import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { Cliente } from '../../core/modelos';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { NotificacionService } from '../../core/notificacion.service';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule, CargadorComponent],
  animations: [fadeInUp],
  templateUrl: './clientes.html',
  styleUrl: './clientes.css'
})
export class ClientesComponent implements OnInit {
  clientes: Cliente[] = [];
  cargando = false;
  busqueda = '';
  
  mostrarFormCliente = false;
  esNuevo = false;
  clienteEdicion: any = {};
  clienteDetalles: Cliente | null = null;

  constructor(private api: ApiService, private notificaciones: NotificacionService) {}
  
  ngOnInit() { 
    this.cargarClientes(); 
  }
  
  cargarClientes() {
    this.cargando = true;
    this.api.clientes().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => this.clientes = r.data,
      error: () => this.notificaciones.mostrarError('No se pudieron cargar los clientes. Revisa que el backend esté activo y tu rol tenga permiso.')
    });
  }
  
  get filtrados() {
    const q = this.busqueda.toLowerCase();
    return this.clientes.filter(c => `${c.nombres} ${c.apellidos} ${c.documento || ''} ${c.email} ${c.telefono || ''}`.toLowerCase().includes(q));
  }
  
  get totalMascotas() { return this.clientes.reduce((s, c) => s + (c.mascotas?.length || 0), 0); }
  get totalGastado() { return this.clientes.reduce((s, c) => s + Number(c.totalGastado || 0), 0); }
  get activos() { return this.clientes.filter(c => (c.estado || 'Activo') === 'Activo').length; }

  abrirNuevoCliente() {
    this.esNuevo = true;
    this.clienteEdicion = {
      nombres: '',
      apellidos: '',
      email: '',
      telefono: '',
      ciudad: 'Quito',
      documento: '',
      estado: 'Activo'
    };
    this.mostrarFormCliente = true;
  }

  abrirEditarCliente(cliente: Cliente) {
    this.esNuevo = false;
    this.clienteEdicion = { ...cliente };
    this.mostrarFormCliente = true;
  }

  abrirDetallesCliente(cliente: Cliente) {
    this.clienteDetalles = cliente;
  }

  cerrarFormCliente() {
    this.mostrarFormCliente = false;
    this.clienteEdicion = {};
  }

  cerrarDetallesCliente() {
    this.clienteDetalles = null;
  }

  guardarCliente() {
    if (!this.clienteEdicion.nombres || !this.clienteEdicion.apellidos || !this.clienteEdicion.email ||
        (this.esNuevo && !/^\d{10}$/.test(String(this.clienteEdicion.documento || '').trim()))) {
      this.notificaciones.mostrarError('Completa nombres, apellidos, correo y una cédula válida de 10 dígitos.');
      return;
    }

    if (this.esNuevo) {
      this.api.crearCliente(this.clienteEdicion).subscribe({
        next: (res) => {
          if (res.ok) {
            this.notificaciones.mostrarExito(res.mensaje || 'Cliente guardado correctamente.');
            this.cargarClientes();
            this.cerrarFormCliente();
          } else {
            this.notificaciones.mostrarError(res.mensaje || 'Error al guardar el cliente');
          }
        },
        error: (err) => {
          this.notificaciones.mostrarError(err.error?.mensaje || 'Error al guardar el cliente. Verifique que el correo no esté registrado.');
        }
      });
    } else {
      this.api.actualizarCliente(this.clienteEdicion.id, this.clienteEdicion).subscribe({
        next: (res) => {
          if (res.ok) {
            this.notificaciones.mostrarExito(res.mensaje || 'Cliente actualizado correctamente.');
            this.cargarClientes();
            this.cerrarFormCliente();
          } else {
            this.notificaciones.mostrarError(res.mensaje || 'Error al actualizar el cliente');
          }
        },
        error: (err) => {
          this.notificaciones.mostrarError(err.error?.mensaje || 'Error al actualizar el cliente');
        }
      });
    }
  }
}
