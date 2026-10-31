import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { HomeComponent } from './inicio';
import { ApiService } from '../../core/api.service';
import { of } from 'rxjs';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: ApiService, useValue: { veterinarios: () => of({ ok: true, mensaje: '', data: [] }) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('debe presentar todas las secciones publicas de la clinica', () => {
    const secciones = ['inicio', 'nosotros', 'servicios', 'especialidades', 'clientes', 'instalaciones', 'veterinarios', 'contacto'];
    secciones.forEach(id => {
      expect(fixture.nativeElement.querySelector('#' + id)).withContext('seccion ' + id).toBeTruthy();
    });
  });

  it('debe ofrecer los siete accesos del menu', () => {
    expect(component.navItems.length).toBe(7);
    expect(component.navItems.map(item => item.label)).toEqual([
      'Nosotros', 'Servicios', 'Especialidades', 'Clientes', 'Instalaciones', 'Veterinarios', 'Contacto'
    ]);
    expect(component.navItems.every(item => item.href.startsWith('#'))).toBe(true);
  });

  it('debe describir los seis servicios de la clinica', () => {
    expect(component.servicios.length).toBe(6);
    expect(component.servicios.map(s => s.area)).toEqual([
      'Clinica', 'Quirofano', 'Diagnostico', 'Farmacia', 'Administracion', 'Recepcion'
    ]);
    expect(component.servicios.every(s => s.titulo.length > 0 && s.texto.length > 0)).toBe(true);
  });

  it('debe listar las seis especialidades medicas', () => {
    expect(component.especialidades.length).toBe(6);
    expect(component.especialidades.map(e => e.titulo)).toContain('Medicina interna');
    expect(component.especialidades.map(e => e.titulo)).toContain('Emergencias');
  });

  it('debe explicar los cuatro pasos de la atencion', () => {
    expect(component.pasos.length).toBe(4);
    expect(component.pasos.map(p => p.titulo)).toEqual(['Contacto', 'Ingreso', 'Consulta', 'Seguimiento']);
  });

  it('debe mostrar las instalaciones con su fotografia', () => {
    expect(component.instalaciones.length).toBe(3);
    expect(component.instalaciones.every(i => i.foto.startsWith('https://'))).toBe(true);
  });

  it('debe mostrar los testimonios de clientes y el equipo medico', () => {
    expect(component.clientes.length).toBe(4);
    expect(component.veterinarios.length).toBe(3);
    expect(component.veterinarios.every(v => v.especialidad.length > 0)).toBe(true);
  });

  it('debe responder las preguntas frecuentes', () => {
    expect(component.preguntas.length).toBe(4);
    expect(component.preguntas.every(p => p.titulo.length > 0 && p.texto.length > 0)).toBe(true);
  });

  it('debe construir el enlace de WhatsApp con el mensaje codificado', () => {
    expect(component.whatsapp).toBe('0989188046');
    expect(component.whatsappUrl).toContain('https://wa.me/593989188046?text=');
    expect(component.whatsappUrl).toContain(encodeURIComponent(component.mensajeWhatsApp));
  });

  it('debe mostrar el enlace de WhatsApp en la seccion de contacto', () => {
    const enlaces = Array.from(fixture.nativeElement.querySelectorAll('a')) as HTMLAnchorElement[];
    const enlaceWhatsApp = enlaces.find(a => a.href.includes('wa.me'));
    expect(enlaceWhatsApp).toBeTruthy();
    expect(enlaceWhatsApp!.href).toContain('wa.me/593989188046');
  });

  it('debe alternar el menu movil', () => {
    expect(component.menuAbierto).toBe(false);
    component.menuAbierto = true;
    fixture.detectChanges();
    expect(component.menuAbierto).toBe(true);
  });
});
