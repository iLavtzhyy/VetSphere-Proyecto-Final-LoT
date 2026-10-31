import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.removeItem('vetsphere_tema');
    document.documentElement.classList.remove('dark');
    TestBed.configureTestingModule({});
  });

  afterEach(() => {
    localStorage.removeItem('vetsphere_tema');
    document.documentElement.classList.remove('dark');
  });

  it('inicia en modo claro cuando no hay preferencia guardada', () => {
    const servicio = TestBed.inject(ThemeService);
    expect(servicio.oscuro()).toBe(false);
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('lee la preferencia oscura desde localStorage y la aplica al iniciar', () => {
    localStorage.setItem('vetsphere_tema', 'oscuro');
    const servicio = TestBed.inject(ThemeService);
    expect(servicio.oscuro()).toBe(true);
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('alternar cambia el tema, lo persiste y lo aplica al documento', () => {
    const servicio = TestBed.inject(ThemeService);

    servicio.alternar();
    expect(servicio.oscuro()).toBe(true);
    expect(localStorage.getItem('vetsphere_tema')).toBe('oscuro');
    expect(document.documentElement.classList.contains('dark')).toBe(true);

    servicio.alternar();
    expect(servicio.oscuro()).toBe(false);
    expect(localStorage.getItem('vetsphere_tema')).toBe('claro');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });
});
