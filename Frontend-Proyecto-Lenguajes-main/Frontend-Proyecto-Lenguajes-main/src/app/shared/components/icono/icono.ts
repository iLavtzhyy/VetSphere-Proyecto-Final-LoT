import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-icono',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './icono.html',
  styleUrl: './icono.css'
})
export class IconoComponent {
  @Input() name = 'heart';
  @Input() size = 20;

  get clase() {
    return `block shrink-0`;
  }
}

