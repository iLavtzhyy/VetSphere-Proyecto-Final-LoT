import { Injectable, signal } from '@angular/core';

export type TipoNotificacion = 'exito' | 'error' | 'info';

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  texto: string;
}

/** Servicio central de notificaciones tipo toast (esquina superior derecha). */
@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private contador = 0;
  private temporizadores = new Map<number, number>();

  /** Lista visible de notificaciones (signal para render reactivo). */
  notificaciones = signal<Notificacion[]>([]);

  mostrar(texto: string, tipo: TipoNotificacion = 'info') {
    const id = ++this.contador;
    this.notificaciones.update(lista => [...lista, { id, tipo, texto }]);
    // Auto-dismiss a los 4 segundos.
    const temporizador = window.setTimeout(() => this.quitar(id), 4000);
    this.temporizadores.set(id, temporizador);
  }

  mostrarExito(texto: string) {
    this.mostrar(texto, 'exito');
  }

  mostrarError(texto: string) {
    this.mostrar(texto, 'error');
  }

  mostrarInfo(texto: string) {
    this.mostrar(texto, 'info');
  }

  cerrar(id: number) {
    this.quitar(id);
  }

  private quitar(id: number) {
    const temporizador = this.temporizadores.get(id);
    if (temporizador) window.clearTimeout(temporizador);
    this.temporizadores.delete(id);
    this.notificaciones.update(lista => lista.filter(n => n.id !== id));
  }
}
