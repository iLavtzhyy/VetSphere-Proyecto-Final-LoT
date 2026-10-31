import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { DocumentacionComponent } from './documentacion';

describe('DocumentacionComponent', () => {
  let component: DocumentacionComponent;
  let fixture: ComponentFixture<DocumentacionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentacionComponent],
      providers: [provideNoopAnimations()]
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentacionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe evaluar los nueve criterios de calidad de ISO/IEC 25010', () => {
    expect(component.criteriosCalidad.length).toBe(9);
  });

  it('debe completar nombre, evidencia, medida y estado de cada criterio', () => {
    component.criteriosCalidad.forEach(criterio => {
      expect(criterio.nombre.length).withContext(criterio.nombre).toBeGreaterThan(0);
      expect(criterio.evidencia.length).withContext(criterio.nombre).toBeGreaterThan(0);
      expect(criterio.medida.length).withContext(criterio.nombre).toBeGreaterThan(0);
      expect(criterio.estado.length).withContext(criterio.nombre).toBeGreaterThan(0);
    });
  });

  it('debe incluir los criterios de seguridad y mantenibilidad', () => {
    const nombres = component.criteriosCalidad.map(c => c.nombre);
    expect(nombres).toContain('Seguridad');
    expect(nombres).toContain('Mantenibilidad');
    expect(nombres).toContain('Fiabilidad');
  });

  it('debe dibujar una ficha por cada criterio en la matriz', () => {
    const fichas = fixture.nativeElement.querySelectorAll('.caracteristicas-grid article');
    expect(fichas.length).toBe(9);
  });

  it('debe explicar el alcance, los perfiles y los controles de seguridad', () => {
    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('ISO/IEC 25010');
    expect(texto).toContain('ISO/IEC 27001');
    expect(texto).toContain('Recepcionista');
    expect(texto).toContain('BCrypt');
  });

  it('debe pedir la impression del documento al navegador', () => {
    const imprimir = spyOn(window, 'print');
    component.imprimir();
    expect(imprimir).toHaveBeenCalled();
  });

  it('debe ofrecer el boton de impresion en el documento', () => {
    const botones = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    const botonImprimir = botones.find(b => (b.textContent || '').includes('Imprimir'));
    expect(botonImprimir).toBeTruthy();

    const imprimir = spyOn(window, 'print');
    botonImprimir!.click();
    expect(imprimir).toHaveBeenCalled();
  });
});
