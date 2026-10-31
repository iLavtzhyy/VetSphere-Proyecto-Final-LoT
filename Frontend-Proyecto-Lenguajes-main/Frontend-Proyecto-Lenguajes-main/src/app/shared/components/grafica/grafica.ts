import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, DoCheck, ElementRef, Input, OnChanges, OnDestroy, ViewChild } from '@angular/core';
import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  ChartConfiguration,
  DoughnutController,
  Filler,
  Legend,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip
} from 'chart.js';
import { ThemeService } from '../../../core/theme.service';
import { CargadorComponent } from '../cargador/cargador';
import { IconoComponent } from '../icono/icono';

// Chart.js exige registrar controladores y elementos antes de crear graficas.
// Se registran solo los que usa el proyecto (bar, line y doughnut) para que el
// bundle se quede con lo estrictamente necesario.
Chart.register(
  CategoryScale,
  LinearScale,
  ArcElement,
  BarElement,
  LineElement,
  PointElement,
  BarController,
  LineController,
  DoughnutController,
  Filler,
  Tooltip,
  Legend
);

/** Tipos de graficas soportadas por el panel de dashboard. */
export type TipoGrafica = 'bar' | 'line' | 'doughnut';

/** Configuracion tipada de Chart.js compartida por todas las graficas del proyecto. */
export type ConfiguracionGrafica = ChartConfiguration<TipoGrafica, number[], string>;

/** Paleta acorde a Tailwind (slate + cyan) para los temas claro y oscuro. */
const PALETA = {
  claro: {
    texto: '#334155',
    textoSecundario: '#64748b',
    rejilla: 'rgba(100, 116, 139, .18)',
    borde: '#e2e8f0',
    fondoTooltip: '#0f172a'
  },
  oscuro: {
    texto: '#cbd5e1',
    textoSecundario: '#94a3b8',
    rejilla: 'rgba(148, 163, 184, .16)',
    borde: '#2f4056',
    fondoTooltip: '#0b1422'
  }
};

/**
 * Envoltorio reutilizable de Chart.js para el dashboard.
 * - Crea y destruye la instancia en cada cambio de configuracion (sin fugas).
 * - Pinta ejes, leyendas y tooltips con la paleta del tema claro/oscuro
 *   (ThemeService) y se redibuja al alternar el tema.
 * - Expone estados de carga, error y "sin datos" ademas de un resumen textual
 *   accesible (aria-label del canvas).
 */
@Component({
  selector: 'app-grafica',
  standalone: true,
  imports: [CommonModule, CargadorComponent, IconoComponent],
  templateUrl: './grafica.html',
  styleUrl: './grafica.css'
})
export class GraficaComponent implements OnChanges, DoCheck, AfterViewInit, OnDestroy {
  /** Configuracion de Chart.js (tipo, etiquetas, datasets y opciones). */
  @Input() configuracion: ConfiguracionGrafica | null = null;
  /** Titulo corto de la grafica; respaldo del aria-label del canvas. */
  @Input() titulo = 'Gráfica';
  /** Resumen textual de los datos: se muestra al pie y como aria-label. */
  @Input() resumen = '';
  /** Mensaje mostrado cuando no hay datos que graficar. */
  @Input() vacio = 'Sin datos para mostrar.';
  /** Muestra el indicador de carga sobre la grafica. */
  @Input() cargando = false;
  /** Mensaje de error mostrado sobre la grafica (null cuando no hay error). */
  @Input() error: string | null = null;
  /** Alto del area de dibujo (valor CSS de longitud, p. ej. '15rem'). */
  @Input() altura = '15rem';

  @ViewChild('lienzo', { static: true }) lienzo?: ElementRef<HTMLCanvasElement>;

  /** Instancia activa de Chart.js; null mientras no haya grafica dibujada. */
  instancia: Chart<TipoGrafica, number[], string> | null = null;

  private vistaLista = false;
  private temaOscuro = false;

  constructor(private readonly tema: ThemeService) {}

  ngOnChanges() {
    this.renderizar();
  }

  ngDoCheck() {
    // El tema lo alterna la barra superior (ThemeService.alternar); al notar el
    // cambio se recrea la grafica con la paleta correspondiente.
    if (this.instancia && this.tema.oscuro() !== this.temaOscuro) {
      this.renderizar();
    }
  }

  ngAfterViewInit() {
    this.vistaLista = true;
    this.renderizar();
  }

  ngOnDestroy() {
    this.instancia?.destroy();
    this.instancia = null;
  }

  private renderizar() {
    if (!this.vistaLista) {
      return;
    }
    this.instancia?.destroy();
    this.instancia = null;
    const canvas = this.lienzo?.nativeElement;
    if (!canvas || !this.configuracion || this.cargando || this.error) {
      return;
    }
    this.temaOscuro = this.tema.oscuro();
    this.instancia = new Chart(canvas, this.conPaleta(this.configuracion, this.temaOscuro));
  }

  /** Copia la configuracion inyectada y pinta ejes/leyenda/tooltip segun el tema. */
  private conPaleta(config: ConfiguracionGrafica, oscuro: boolean): ConfiguracionGrafica {
    const paleta = oscuro ? PALETA.oscuro : PALETA.claro;
    const opciones = (config.options ?? {}) as Record<string, any>;
    const plugins: Record<string, any> = { ...(opciones['plugins'] ?? {}) };
    const leyenda: Record<string, any> = { ...(plugins['legend'] ?? {}) };

    plugins['legend'] = {
      ...leyenda,
      labels: { font: { weight: 700 }, ...(leyenda['labels'] ?? {}), color: paleta.texto }
    };
    plugins['tooltip'] = {
      backgroundColor: paleta.fondoTooltip,
      titleColor: '#f8fafc',
      bodyColor: '#e2e8f0',
      borderColor: paleta.borde,
      borderWidth: 1,
      padding: 10,
      ...(plugins['tooltip'] ?? {})
    };

    const opcionesConTema: Record<string, any> = {
      responsive: true,
      maintainAspectRatio: false,
      ...opciones,
      plugins,
      color: paleta.texto
    };

    const escalas = opciones['scales'];
    if (escalas) {
      const copiaDeEscalas: Record<string, any> = {};
      for (const clave of Object.keys(escalas)) {
        const escala: Record<string, any> = { ...(escalas[clave] ?? {}) };
        escala['ticks'] = { ...(escala['ticks'] ?? {}), color: paleta.textoSecundario };
        escala['grid'] = { ...(escala['grid'] ?? {}), color: paleta.rejilla };
        escala['border'] = { ...(escala['border'] ?? {}), color: paleta.borde };
        if (escala['title']) {
          escala['title'] = { ...escala['title'], color: paleta.textoSecundario };
        }
        copiaDeEscalas[clave] = escala;
      }
      opcionesConTema['scales'] = copiaDeEscalas;
    }

    return {
      ...config,
      data: { ...config.data, datasets: config.data.datasets.map(dataset => ({ ...dataset })) },
      options: opcionesConTema
    } as ConfiguracionGrafica;
  }
}
