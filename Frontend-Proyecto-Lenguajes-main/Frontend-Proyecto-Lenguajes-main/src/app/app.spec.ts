import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app';
import { SesionInactividadService } from './core/sesion-inactividad.service';

describe('AppComponent', () => {
  beforeEach(async () => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  afterEach(() => {
    document.documentElement.classList.remove('dark');
    localStorage.clear();
  });

  it('debe crearse correctamente', () => {
    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('debe aplicar el tema oscuro guardado en el navegador', () => {
    localStorage.setItem('vetsphere_tema', 'oscuro');

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('debe dejar el tema claro cuando no hay preferencia guardada', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('debe iniciar el control de inactividad de la sesion', () => {
    const sesion = TestBed.inject(SesionInactividadService);
    const iniciar = spyOn(sesion, 'iniciar');

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    expect(iniciar).toHaveBeenCalled();
  });

  it('debe renderizar el contenedor de rutas de la aplicacion', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('router-outlet')).toBeTruthy();
  });
});
