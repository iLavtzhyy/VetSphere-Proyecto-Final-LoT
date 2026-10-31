import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ThemeService } from '../../core/theme.service';
import { IconoComponent } from '../../shared/components/icono/icono';
import { AutorizacionService } from '../../core/autorizacion.service';
import { NotificacionesComponent } from '../../shared/components/notificaciones/notificaciones';
import { ChatbotComponent } from '../chatbot/chatbot';

type ItemMenu = {
  nombre: string;
  ruta: string;
  icono: string;
  roles: string[];
};

type GrupoMenu = {
  titulo: string;
  items: ItemMenu[];
};

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, IconoComponent, NotificacionesComponent, ChatbotComponent],
  templateUrl: './barra-navegacion-admin.html',
  styleUrl: './barra-navegacion-admin.css'
})
export class LayoutComponent implements OnInit {
  constructor(public theme: ThemeService, public autorizacion: AutorizacionService, private router: Router) {}

  menuAbierto = false;
  nombreSesion = 'Usuario VetSphere';
  rolesSesion = '';
  gruposVisibles: GrupoMenu[] = [];

  grupos: GrupoMenu[] = [
    { titulo: 'Principal', items: [
      { nombre: 'Dashboard', ruta: '/dashboard', icono: 'dashboard', roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO', 'ROLE_RECEPCIONISTA'] },
      { nombre: 'Reportes', ruta: '/reportes', icono: 'file', roles: ['ROLE_ADMIN'] },
      { nombre: 'Mi cuenta', ruta: '/mi-cuenta', icono: 'user', roles: [] }
    ] },
    { titulo: 'Gestion', items: [
      { nombre: 'Clientes', ruta: '/clientes', icono: 'users', roles: ['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'] },
      { nombre: 'Mascotas', ruta: '/mascotas', icono: 'heart', roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO', 'ROLE_RECEPCIONISTA', 'ROLE_CLIENTE'] },
      { nombre: 'Recepcion', ruta: '/recepcion', icono: 'calendar', roles: ['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'] },
      { nombre: 'Veterinarios', ruta: '/veterinarios', icono: 'stethoscope', roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO', 'ROLE_RECEPCIONISTA', 'ROLE_CLIENTE'] }
    ] },
    { titulo: 'Clinica', items: [
      { nombre: 'Consultas', ruta: '/consultas', icono: 'stethoscope', roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO'] },
      { nombre: 'Cirugias', ruta: '/cirugias', icono: 'surgery', roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO'] },
      { nombre: 'Laboratorio', ruta: '/laboratorio', icono: 'lab', roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO'] },
      { nombre: 'Farmacia', ruta: '/farmacia', icono: 'pill', roles: ['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'] },
      { nombre: 'Hospitalizacion', ruta: '/hospitalizacion', icono: 'shield', roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO'] }
    ] },
    { titulo: 'Administracion', items: [
      { nombre: 'Facturacion', ruta: '/facturacion', icono: 'file', roles: ['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'] },
      { nombre: 'Admin', ruta: '/administracion', icono: 'shield', roles: ['ROLE_ADMIN'] },
      { nombre: 'Cadena de frío IoT', ruta: '/cadena-frio', icono: 'thermometer', roles: ['ROLE_ADMIN'] },
      { nombre: 'Usuarios', ruta: '/usuarios', icono: 'users', roles: ['ROLE_ADMIN'] },
      { nombre: 'Calidad del software', ruta: '/calidad-software', icono: 'shield', roles: ['ROLE_ADMIN'] }
    ] }
  ];

  ngOnInit() {
    this.actualizarMenuPorRol();
  }

  actualizarMenuPorRol() {
    this.nombreSesion = this.autorizacion.nombre();
    this.rolesSesion = this.autorizacion.roles().join(', ') || 'Sin rol asignado';
    this.gruposVisibles = this.grupos
      .map(grupo => ({ ...grupo, items: grupo.items.filter(item => this.autorizacion.tieneRol(item.roles)) }))
      .filter(grupo => grupo.items.length);
  }

  trackGrupo(_: number, grupo: GrupoMenu) {
    return grupo.titulo;
  }

  trackItem(_: number, item: ItemMenu) {
    return item.ruta;
  }

  cerrarMenu() {
    this.menuAbierto = false;
  }

  salir() {
    this.autorizacion.limpiarSesion();
    this.router.navigateByUrl('/login');
  }
}
