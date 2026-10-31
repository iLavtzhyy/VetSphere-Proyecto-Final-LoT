import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/theme.service';
import { SesionInactividadService } from './core/sesion-inactividad.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent {
  constructor(private themeService: ThemeService, private sesionInactividad: SesionInactividadService) {
    this.themeService.aplicar();
    this.sesionInactividad.iniciar();
  }
}
