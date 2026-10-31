import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Chart } from 'chart.js';
import { GraficaComponent, ConfiguracionGrafica } from './grafica';
import { ThemeService } from '../../../core/theme.service';

describe('GraficaComponent', () => {
  let component: GraficaComponent;
  let fixture: ComponentFixture<GraficaComponent>;
  let fixtureDestruido = false;

  const configuracionDePrueba: ConfiguracionGrafica = {
    type: 'bar',
    data: {
      labels: ['lun', 'mar'],
      datasets: [{ label: 'Citas', data: [3, 5], backgroundColor: '#0891b2' }]
    },
    options: {
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true } }
    }
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [GraficaComponent] }).compileComponents();

    fixtureDestruido = false;
    fixture = TestBed.createComponent(GraficaComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    if (!fixtureDestruido) {
      fixture.destroy();
    }
    document.documentElement.classList.remove('dark');
  });

  it('crea una instancia de Chart.js con la configuracion recibida', () => {
    component.configuracion = configuracionDePrueba;
    fixture.detectChanges();

    expect(component.instancia).toBeTruthy();
    expect((component.instancia?.config as ConfiguracionGrafica).type).toBe('bar');
    expect(component.instancia?.data.labels).toEqual(['lun', 'mar']);
  });

  it('expone el resumen como aria-label accesible del canvas', () => {
    component.configuracion = configuracionDePrueba;
    component.resumen = 'Total 8 citas en la semana.';
    fixture.detectChanges();

    const lienzo: HTMLCanvasElement = fixture.nativeElement.querySelector('canvas');
    expect(lienzo.getAttribute('role')).toBe('img');
    expect(lienzo.getAttribute('aria-label')).toBe('Total 8 citas en la semana.');
  });

  it('destruye la instancia de Chart.js al destruir el componente', () => {
    component.configuracion = configuracionDePrueba;
    fixture.detectChanges();
    const lienzo: HTMLCanvasElement = fixture.nativeElement.querySelector('canvas');
    expect(Chart.getChart(lienzo)).toBeTruthy();

    fixtureDestruido = true;
    fixture.destroy();

    expect(Chart.getChart(lienzo)).toBeFalsy();
  });

  it('no dibuja la grafica y muestra el error cuando la carga fallo', () => {
    component.configuracion = configuracionDePrueba;
    component.error = 'No se pudo cargar la facturación del mes.';
    fixture.detectChanges();

    expect(component.instancia).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('No se pudo cargar la facturación del mes.');
  });

  it('muestra el mensaje de vacio cuando no hay configuracion', () => {
    component.configuracion = null;
    fixture.detectChanges();

    expect(component.instancia).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Sin datos para mostrar.');
  });

  it('repinta los ejes al alternar el tema claro/oscuro', () => {
    const tema = TestBed.inject(ThemeService);
    component.configuracion = configuracionDePrueba;
    fixture.detectChanges();
    const graficaClara = component.instancia!;
    const colorClaro = ((graficaClara.options?.scales as any)['y'].ticks.color) as string;
    expect(colorClaro).toBe('#64748b');

    tema.alternar();
    fixture.detectChanges();

    const graficaOscura = component.instancia!;
    expect(graficaOscura).not.toBe(graficaClara);
    expect(((graficaOscura.options?.scales as any)['y'].ticks.color)).toBe('#94a3b8');

    tema.alternar();
    fixture.detectChanges();
  });
});
