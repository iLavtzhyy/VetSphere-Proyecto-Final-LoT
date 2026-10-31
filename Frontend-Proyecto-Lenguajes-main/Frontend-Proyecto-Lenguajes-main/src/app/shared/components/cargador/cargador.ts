import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

/** Indicador de carga compartido: spinner SVG + texto opcional. */
@Component({
  selector: 'app-cargador',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cargador.html',
  styleUrl: './cargador.css'
})
export class CargadorComponent {
  @Input() texto = 'Cargando…';
  @Input() centrado = true;
}
