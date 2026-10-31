import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AutorizacionService {
  private rolesCache: string[] | null = null;
  private nombreCache: string | null = null;

  roles() {
    if (this.rolesCache) return this.rolesCache;
    try {
      const rolesGuardados = JSON.parse(localStorage.getItem('vetsphere_roles') || '[]') as string[];
      this.rolesCache = Array.isArray(rolesGuardados) ? rolesGuardados : [];
      return this.rolesCache;
    } catch {
      this.rolesCache = [];
      return [];
    }
  }

  nombre() {
    if (this.nombreCache) return this.nombreCache;
    this.nombreCache = localStorage.getItem('vetsphere_nombre') || 'Usuario VetSphere';
    return this.nombreCache;
  }

  tieneRol(rolesPermitidos?: string[]) {
    if (!rolesPermitidos || rolesPermitidos.length === 0) return true;
    const rolesActuales = this.roles();
    return rolesPermitidos.some(rol => rolesActuales.includes(rol));
  }

  guardarSesion(data: any) {
    const roles = Array.isArray(data.roles) ? data.roles : [];
    const nombre = data.nombre || data.email || 'Usuario VetSphere';
    localStorage.setItem('vetsphere_token', data.token);
    localStorage.setItem('vetsphere_roles', JSON.stringify(roles));
    localStorage.setItem('vetsphere_nombre', nombre);
    this.rolesCache = roles;
    this.nombreCache = nombre;
  }

  limpiarSesion() {
    localStorage.removeItem('vetsphere_token');
    localStorage.removeItem('vetsphere_roles');
    localStorage.removeItem('vetsphere_nombre');
    this.rolesCache = [];
    this.nombreCache = null;
  }
}
