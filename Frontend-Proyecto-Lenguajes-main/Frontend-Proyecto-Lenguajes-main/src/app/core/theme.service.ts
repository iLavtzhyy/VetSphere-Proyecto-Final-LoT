import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  oscuro = signal(localStorage.getItem('vetsphere_tema') === 'oscuro');

  constructor() {
    this.aplicar();
  }

  alternar() {
    this.oscuro.update(valor => !valor);
    localStorage.setItem('vetsphere_tema', this.oscuro() ? 'oscuro' : 'claro');
    this.aplicar();
  }

  aplicar() {
    document.documentElement.classList.toggle('dark', this.oscuro());
  }
}
