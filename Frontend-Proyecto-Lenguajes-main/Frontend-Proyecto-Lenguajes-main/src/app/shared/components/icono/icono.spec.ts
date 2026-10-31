import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { IconoComponent } from './icono';

describe('IconoComponent', () => {
  let component: IconoComponent;
  let fixture: ComponentFixture<IconoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IconoComponent],
      providers: [provideNoopAnimations()]
    }).compileComponents();

    fixture = TestBed.createComponent(IconoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function svg(): SVGSVGElement {
    return fixture.nativeElement.querySelector('svg') as SVGSVGElement;
  }

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe dibujar un unico svg', () => {
    expect(fixture.nativeElement.querySelectorAll('svg').length).toBe(1);
  });

  it('debe aplicar el tamano solicitado en el svg', () => {
    component.size = 32;
    fixture.detectChanges();
    expect(svg().style.width).toBe('32px');
    expect(svg().style.height).toBe('32px');
  });

  it('debe usar el glifo de whatsapp cuando el nombre es whatsapp', () => {
    component.name = 'whatsapp';
    fixture.detectChanges();
    const paths = fixture.nativeElement.querySelectorAll('path');
    expect(paths.length).toBe(1);
    expect(paths[0].getAttribute('d')).toContain('M12.04 2C6.57 2 2.12 6.45');
  });

  it('debe dibujar el icono de telefono con su trazo propio', () => {
    component.name = 'phone';
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('circle').length).toBe(0);
    expect(fixture.nativeElement.querySelector('path').getAttribute('d')).toContain('M22 16.92v3');
  });

  it('debe dibujar el icono de usuario con el circulo de la cabeza', () => {
    component.name = 'user';
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('circle').length).toBe(1);
  });

  it('debe dibujar el icono de escudo con el visto bueno', () => {
    component.name = 'shield';
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('path').length).toBe(2);
  });

  it('debe caer en el corazon por defecto cuando el nombre no existe', () => {
    component.name = 'icono-inexistente';
    fixture.detectChanges();
    const paths = fixture.nativeElement.querySelectorAll('path');
    expect(paths.length).toBe(1);
    expect(paths[0].getAttribute('d')).toContain('M12 21s-8-4.4-8-11');
  });
});
