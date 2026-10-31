import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Notificacion, NotificacionService } from '../../../core/notificacion.service';
import { IconoComponent } from '../icono/icono';

/** Toasts de notificación (éxito / error / info) en la esquina superior derecha. */
@Component({
  selector: 'app-notificaciones',
  standalone: true,
  imports: [CommonModule, IconoComponent],
  templateUrl: './notificaciones.html',
  styleUrl: './notificaciones.css'
})
export class NotificacionesComponent {
  readonly servicio = inject(NotificacionService);

  clases(notificacion: Notificacion): string {
    const base = 'pointer-events-auto flex w-full items-start gap-3 rounded-xl border px-4 py-3 text-sm font-bold shadow-suave backdrop-blur';
    switch (notificacion.tipo) {
      case 'exito':
        return `${base} border-emerald-200 bg-emerald-50/95 text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950/95 dark:text-emerald-200`;
      case 'error':
        return `${base} border-rose-200 bg-rose-50/95 text-rose-800 dark:border-rose-800 dark:bg-rose-950/95 dark:text-rose-200`;
      default:
        return `${base} border-cyan-200 bg-cyan-50/95 text-cyan-800 dark:border-cyan-800 dark:bg-cyan-950/95 dark:text-cyan-200`;
    }
  }

  icono(notificacion: Notificacion): string {
    switch (notificacion.tipo) {
      case 'exito': return 'check';
      case 'error': return 'alert';
      default: return 'info';
    }
  }

  trackNotificacion(_: number, notificacion: Notificacion) {
    return notificacion.id;
  }
}
