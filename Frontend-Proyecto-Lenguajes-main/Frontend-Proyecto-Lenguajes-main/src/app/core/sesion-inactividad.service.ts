import { Injectable, NgZone } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class SesionInactividadService {
  private readonly limiteInactividadMs = 30 * 60 * 1000;
  private readonly claveAuth = 'vetsphere_token';
  private readonly claveActividad = 'vetsphere_ultima_actividad';
  private readonly claveMensaje = 'vetsphere_mensaje_sesion';
  private temporizador?: number;
  private iniciado = false;

  constructor(private router: Router, private zone: NgZone) {}

  iniciar() {
    if (this.iniciado) return;
    this.iniciado = true;

    const eventos = ['mousemove', 'mousedown', 'keydown', 'scroll', 'touchstart', 'click'];
    // Los oyentes y el temporizador viven fuera de Angular: registrar la
    // actividad NO cambia la vista, y de lo contrario cada movimiento del
    // raton dispararia un ciclo de deteccion de cambios en toda la aplicacion.
    this.zone.runOutsideAngular(() => {
      eventos.forEach(evento => {
        window.addEventListener(evento, () => this.registrarActividad(), { passive: true });
      });
      this.temporizador = window.setInterval(() => this.verificarSesion(), 60_000);
    });
    this.registrarActividadSiHayToken();
  }

  registrarActividad() {
    if (!this.obtenerToken()) return;
    localStorage.setItem(this.claveActividad, String(Date.now()));
  }

  registrarActividadSiHayToken() {
    if (this.obtenerToken()) this.registrarActividad();
  }

  sesionActiva() {
    const token = this.obtenerToken();
    if (!token) return false;
    if (this.jwtExpirado(token) || this.inactividadVencida()) {
      this.cerrarSesion('Tu sesion se cerro automaticamente por seguridad.');
      return false;
    }
    return true;
  }

  cerrarSesion(mensaje = 'Sesion cerrada.') {
    localStorage.removeItem(this.claveAuth);
    localStorage.removeItem('vetsphere_roles');
    localStorage.removeItem('vetsphere_nombre');
    localStorage.removeItem(this.claveActividad);
    sessionStorage.setItem(this.claveMensaje, mensaje);
    this.zone.run(() => {
      if (!this.router.url.startsWith('/login')) {
        this.router.navigateByUrl('/login');
      }
    });
  }

  consumirMensaje() {
    const mensaje = sessionStorage.getItem(this.claveMensaje) || '';
    sessionStorage.removeItem(this.claveMensaje);
    return mensaje;
  }

  private verificarSesion() {
    if (!this.obtenerToken()) return;
    if (this.jwtExpirado(this.obtenerToken() || '') || this.inactividadVencida()) {
      this.cerrarSesion('Por seguridad se cerro la sesion despues de 30 minutos sin actividad.');
    }
  }

  private inactividadVencida() {
    const ultimaActividad = Number(localStorage.getItem(this.claveActividad) || 0);
    if (!ultimaActividad) return false;
    return Date.now() - ultimaActividad > this.limiteInactividadMs;
  }

  private jwtExpirado(token: string) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1] || ''));
      return typeof payload.exp === 'number' && Date.now() >= payload.exp * 1000;
    } catch {
      return true;
    }
  }

  private obtenerToken() {
    return localStorage.getItem(this.claveAuth);
  }
}
