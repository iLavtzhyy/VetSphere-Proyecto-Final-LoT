import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { RecepcionComponent } from './agendar-cita';
import { NotificacionService } from '../../core/notificacion.service';
import { Mascota, Usuario } from '../../core/modelos';
import { environment } from '../../../environments/environment';

const API = environment.apiUrl;

const PANEL = { CitasHoy: 3, CitasPendientes: 2, EnEspera: 1 };
const ETAPAS: any[] = [
  { id: 1, nombre: 'Agenda', estadoCita: 'Programada', descripcion: 'Cita reservada', color: 'cyan', posicion: 1, activa: true },
  { id: 2, nombre: 'Triage', estadoCita: 'En triage', descripcion: 'Evaluacion inicial', color: 'amber', posicion: 2, activa: true }
];

const CITAS: any[] = [
  {
    id: 1, fechaHora: '2026-03-10T09:30:00', motivo: 'Vacunacion', estado: 'Programada', observaciones: 'Primera visita',
    mascota: { id: 1, nombre: 'Firulais' }, veterinario: { id: 1, nombres: 'Carlos', apellidos: 'Mena' }
  },
  {
    id: 2, fechaHora: '2026-03-11T15:00:00', motivo: 'Control', estado: 'Confirmada', observaciones: '',
    mascota: null, veterinario: null
  }
];

const MASCOTAS: Mascota[] = [
  {
    id: 1, nombre: 'Firulais', especie: 'Canino', raza: 'Golden Retriever', sexo: 'Macho', color: 'Dorado',
    pesoKg: 12.5, fechaNacimiento: '2020-05-10', estado: 'ACTIVA',
    propietario: { id: 10, nombres: 'Ana', apellidos: 'Ruiz', email: 'ana@correo.com', telefono: '0999999999', roles: [] },
    historiales: null, cirugias: null
  },
  {
    id: 2, nombre: 'Michi', especie: 'Felino', raza: 'Siames', sexo: 'Hembra', color: 'Blanco',
    pesoKg: 4, fechaNacimiento: '2022-01-05', estado: 'INTERNADA',
    propietario: { id: 11, nombres: 'Luis', apellidos: 'Perez', email: 'luis@correo.com', telefono: '0888888888', roles: [] },
    historiales: null, cirugias: null
  }
];

const VETERINARIOS: Usuario[] = [
  {
    id: 1, nombres: 'Carlos', apellidos: 'Mena', email: 'carlos@vet.com', telefono: '0991111111',
    especialidad: 'Cirugia', cedulaProfesional: 'VET-001', roles: [{ id: 1, nombre: 'VETERINARIO' }]
  },
  {
    id: 2, nombres: 'Sofia', apellidos: 'Ramos', email: 'sofia@vet.com', telefono: '0992222222',
    especialidad: 'Dermatologia', cedulaProfesional: 'VET-002', roles: []
  }
];

describe('RecepcionComponent', () => {
  let component: RecepcionComponent;
  let fixture: ComponentFixture<RecepcionComponent>;
  let httpMock: HttpTestingController;
  let notificaciones: NotificacionService;

  /** Crea un componente y responde a las cuatro peticiones de carga inicial. */
  function crearComponente(): ComponentFixture<RecepcionComponent> {
    const fx = TestBed.createComponent(RecepcionComponent);
    fx.detectChanges();
    httpMock.expectOne(`${API}/recepcion/panel`).flush({ ok: true, mensaje: '', data: PANEL });
    httpMock.expectOne(`${API}/citas`).flush({ ok: true, mensaje: '', data: structuredClone(CITAS) });
    httpMock.expectOne(`${API}/mascotas`).flush({ ok: true, mensaje: '', data: structuredClone(MASCOTAS) });
    httpMock.expectOne(`${API}/veterinarios/publico`).flush({ ok: true, mensaje: '', data: VETERINARIOS });
    httpMock.expectOne(`${API}/recepcion/flujo`).flush({ ok: true, mensaje: '', data: ETAPAS });
    fx.detectChanges();
    return fx;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecepcionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    notificaciones = TestBed.inject(NotificacionService);

    fixture = crearComponente();
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('carga el panel, las citas, las mascotas y los veterinarios al arrancar', () => {
    expect(component.panel['CitasHoy']).toBe(3);
    expect(component.citas.length).toBe(2);
    expect(component.mascotas.length).toBe(2);
    expect(component.veterinarios.length).toBe(2);
    expect(component.cargando).toBe(false);
  });

  it('muestra los indicadores del panel de recepcion', () => {
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('CitasHoy');
    expect(texto).toContain('CitasPendientes');
    expect(texto).toContain('EnEspera');
  });

  it('muestra la fecha, la mascota, el veterinario, el motivo y el estado de cada cita', () => {
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('10/03/2026 09:30');
    expect(texto).toContain('Firulais');
    expect(texto).toContain('Carlos Mena');
    expect(texto).toContain('Vacunacion');
    expect(texto).toContain('Programada');
    expect(texto).toContain('Confirmada');
  });

  it('permite cambiar el estado directamente desde la lista de citas', () => {
    const cita = component.citas[0];
    component.cambiarEstadoCita(cita, 'En triage');

    const peticion = httpMock.expectOne(`${API}/citas/1`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({
      fechaHora: cita.fechaHora,
      motivo: cita.motivo,
      estado: 'En triage',
      observaciones: cita.observaciones,
      mascota: { id: 1 },
      veterinario: { id: 1 },
      recepcionista: null
    });
    peticion.flush({ ok: true, mensaje: '', data: { ...cita, estado: 'En triage' } });

    expect(cita.estado).toBe('En triage');
    expect(component.estadosCambiando.has(cita.id)).toBe(false);
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('Estado de la cita actualizado a En triage.');
  });

  it('conserva el estado previo si el servidor rechaza el cambio directo', () => {
    const cita = component.citas[0];
    component.cambiarEstadoCita(cita, 'En triage');

    httpMock.expectOne(`${API}/citas/1`).flush({ mensaje: 'Estado no disponible' }, { status: 400, statusText: 'Bad Request' });

    expect(cita.estado).toBe('Programada');
    expect(component.estadosCambiando.has(cita.id)).toBe(false);
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('Estado no disponible');
  });

  it('muestra Sin mascota y Sin asignar cuando la cita no tiene relacion registrada', () => {
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Sin mascota');
    expect(texto).toContain('Sin asignar');
  });

  it('carga el flujo editable y permite usar sus estados en las citas', () => {
    expect(component.etapasFlujo.map(etapa => etapa.nombre)).toEqual(['Agenda', 'Triage']);
    component.nueva();
    expect(component.estadosActivos).toEqual(['Programada', 'En triage']);
  });

  it('crea una nueva etapa y recarga el flujo y las citas', () => {
    component.nuevaEtapa();
    component.etapaForm = {
      nombre: 'Hospitalización', estadoCita: 'Internada', descripcion: 'Paciente ingresada',
      color: 'violet', posicion: 3, activa: true
    };

    component.guardarEtapa();

    const crear = httpMock.expectOne(`${API}/recepcion/flujo`);
    expect(crear.request.method).toBe('POST');
    expect(crear.request.body).toEqual(component.etapaForm);
    crear.flush({ ok: true, mensaje: '', data: { ...component.etapaForm, id: 3 } });
    httpMock.expectOne(`${API}/recepcion/flujo`).flush({ ok: true, mensaje: '', data: [...ETAPAS, { ...component.etapaForm, id: 3 }] });
    httpMock.expectOne(`${API}/citas`).flush({ ok: true, mensaje: '', data: CITAS });

    expect(component.editorFlujoAbierto).toBe(false);
    expect(component.etapasFlujo.length).toBe(3);
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('Etapa creada.');
  });

  it('edita una etapa y envía los nuevos datos al servidor', () => {
    component.editarEtapa(ETAPAS[1]);
    component.etapaForm.nombre = 'Evaluación inicial';
    component.etapaForm.estadoCita = 'En evaluacion';

    component.guardarEtapa();

    const actualizar = httpMock.expectOne(`${API}/recepcion/flujo/2`);
    expect(actualizar.request.method).toBe('PUT');
    expect(actualizar.request.body.nombre).toBe('Evaluación inicial');
    expect(actualizar.request.body.estadoCita).toBe('En evaluacion');
    actualizar.flush({ ok: true, mensaje: '', data: { ...component.etapaForm, id: 2 } });
    httpMock.expectOne(`${API}/recepcion/flujo`).flush({ ok: true, mensaje: '', data: ETAPAS });
    httpMock.expectOne(`${API}/citas`).flush({ ok: true, mensaje: '', data: CITAS });

    expect(component.editorFlujoAbierto).toBe(false);
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('Etapa actualizada.');
  });

  it('reordena una etapa mediante la posición persistida', () => {
    component.moverEtapa(ETAPAS[0], 1);

    const actualizar = httpMock.expectOne(`${API}/recepcion/flujo/1`);
    expect(actualizar.request.method).toBe('PUT');
    expect(actualizar.request.body.posicion).toBe(2);
    actualizar.flush({ ok: true, mensaje: '', data: ETAPAS[0] });
    httpMock.expectOne(`${API}/recepcion/flujo`).flush({ ok: true, mensaje: '', data: [
      { ...ETAPAS[1], posicion: 1 }, { ...ETAPAS[0], posicion: 2 }
    ] });

    expect(component.etapasFlujo.map(etapa => etapa.id)).toEqual([2, 1]);
  });

  it('desactiva una etapa sin eliminarla para que deje de asignarse a citas nuevas', () => {
    component.cambiarEstadoEtapa(ETAPAS[1], false);

    const actualizar = httpMock.expectOne(`${API}/recepcion/flujo/2`);
    expect(actualizar.request.method).toBe('PUT');
    expect(actualizar.request.body.activa).toBe(false);
    actualizar.flush({ ok: true, mensaje: '', data: { ...ETAPAS[1], activa: false } });
    httpMock.expectOne(`${API}/recepcion/flujo`).flush({ ok: true, mensaje: '', data: [{ ...ETAPAS[0] }, { ...ETAPAS[1], activa: false }] });

    expect(component.estadosActivos).toEqual(['Programada']);
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('Etapa desactivada.');
  });

  it('elimina una etapa sin citas y vuelve a cargar el flujo', () => {
    component.solicitarEliminarEtapa(ETAPAS[1]);
    component.eliminarEtapa(ETAPAS[1]);

    const eliminar = httpMock.expectOne(`${API}/recepcion/flujo/2`);
    expect(eliminar.request.method).toBe('DELETE');
    eliminar.flush({ ok: true, mensaje: '', data: ETAPAS[1] });
    httpMock.expectOne(`${API}/recepcion/flujo`).flush({ ok: true, mensaje: '', data: [ETAPAS[0]] });

    expect(component.confirmandoEliminacion).toBeNull();
    expect(component.etapasFlujo.map(etapa => etapa.id)).toEqual([1]);
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('Etapa eliminada.');
  });

  it('abre el formulario de nueva cita con la primera mascota y el primer veterinario', () => {
    component.nueva();

    expect(component.formularioAbierto).toBe(true);
    expect(component.editandoId).toBeNull();
    expect(component.form.mascotaId).toBe(1);
    expect(component.form.veterinarioId).toBe(1);
    expect(component.form.motivo).toBe('Consulta general');
    expect(component.form.estado).toBe('Programada');
    expect(component.form.observaciones).toBe('');
    expect(component.form.fechaHora).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
    expect(component.mensaje).toBe('');
  });

  it('crea la cita enviando los identificadores de mascota y veterinario', () => {
    component.nueva();
    component.form.fechaHora = '2026-04-01T10:00';
    component.form.motivo = 'Vacunacion anual';
    component.form.estado = 'Confirmada';
    component.form.observaciones = 'Traer cartilla';
    component.form.mascotaId = 1;
    component.form.veterinarioId = 1;

    component.guardar();

    const peticion = httpMock.expectOne(`${API}/citas`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({
      fechaHora: '2026-04-01T10:00',
      motivo: 'Vacunacion anual',
      estado: 'Confirmada',
      observaciones: 'Traer cartilla',
      mascota: { id: 1 },
      veterinario: { id: 1 }
    });

    peticion.flush({ ok: true, mensaje: '', data: CITAS[0] });
    httpMock.expectOne(`${API}/recepcion/panel`).flush({ ok: true, mensaje: '', data: PANEL });
    httpMock.expectOne(`${API}/citas`).flush({ ok: true, mensaje: '', data: CITAS });
    httpMock.expectOne(`${API}/recepcion/flujo`).flush({ ok: true, mensaje: '', data: ETAPAS });

    expect(component.mensaje).toBe('Cita creada correctamente.');
    expect(component.formularioAbierto).toBe(false);
    expect(component.editandoId).toBeNull();
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('Cita creada correctamente.');
  });

  it('carga los datos de la cita en el formulario de edicion', () => {
    component.editar(CITAS[0]);

    expect(component.editandoId).toBe(1);
    expect(component.formularioAbierto).toBe(true);
    expect(component.form.fechaHora).toBe('2026-03-10T09:30');
    expect(component.form.motivo).toBe('Vacunacion');
    expect(component.form.estado).toBe('Programada');
    expect(component.form.observaciones).toBe('Primera visita');
    expect(component.form.mascotaId).toBe(1);
    expect(component.form.veterinarioId).toBe(1);
  });

  it('actualiza la cita en edicion contra el endpoint de la cita', () => {
    component.editar(CITAS[0]);
    component.form.estado = 'Atendida';

    component.guardar();

    const peticion = httpMock.expectOne(`${API}/citas/1`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body.estado).toBe('Atendida');
    expect(peticion.request.body.mascota).toEqual({ id: 1 });
    expect(peticion.request.body.veterinario).toEqual({ id: 1 });

    peticion.flush({ ok: true, mensaje: '', data: CITAS[0] });
    httpMock.expectOne(`${API}/recepcion/panel`).flush({ ok: true, mensaje: '', data: PANEL });
    httpMock.expectOne(`${API}/citas`).flush({ ok: true, mensaje: '', data: CITAS });
    httpMock.expectOne(`${API}/recepcion/flujo`).flush({ ok: true, mensaje: '', data: ETAPAS });

    expect(component.mensaje).toBe('Cita actualizada correctamente.');
    expect(component.formularioAbierto).toBe(false);
    expect(component.editandoId).toBeNull();
    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('Cita actualizada correctamente.');
  });

  it('informa del error cuando la cita no se puede crear', () => {
    component.nueva();
    component.guardar();

    const peticion = httpMock.expectOne(`${API}/citas`);
    peticion.flush({ mensaje: 'Error del servidor' }, { status: 500, statusText: 'Server Error' });

    expect(component.mensaje).toBe('No se pudo crear la cita. Revisa mascota, veterinario y permisos.');
    expect(component.formularioAbierto).toBe(true);
  });

  it('informa del error cuando la cita no se puede actualizar', () => {
    component.editar(CITAS[0]);
    component.guardar();

    const peticion = httpMock.expectOne(`${API}/citas/1`);
    peticion.flush({ mensaje: 'Error del servidor' }, { status: 500, statusText: 'Server Error' });

    expect(component.mensaje).toBe('No se pudo actualizar la cita. Revisa mascota, veterinario y permisos.');
  });

  it('cierra el formulario de cita sin guardar', () => {
    component.nueva();
    component.cerrar();
    expect(component.formularioAbierto).toBe(false);
    expect(component.editandoId).toBeNull();
  });

  it('informa del error cuando el panel de recepcion no se puede cargar', () => {
    const fx = TestBed.createComponent(RecepcionComponent);
    fx.detectChanges();

    httpMock.expectOne(`${API}/recepcion/panel`).flush({ mensaje: 'Sin permiso' }, { status: 403, statusText: 'Forbidden' });
    httpMock.expectOne(`${API}/citas`).flush({ ok: true, mensaje: '', data: CITAS });
    httpMock.expectOne(`${API}/mascotas`).flush({ ok: true, mensaje: '', data: MASCOTAS });
    httpMock.expectOne(`${API}/veterinarios/publico`).flush({ ok: true, mensaje: '', data: VETERINARIOS });
    httpMock.expectOne(`${API}/recepcion/flujo`).flush({ ok: true, mensaje: '', data: ETAPAS });

    expect(fx.componentInstance.mensaje).toBe('No se pudo cargar el panel de recepcion.');
  });

  it('informa del error cuando las citas no se pueden cargar', () => {
    const fx = TestBed.createComponent(RecepcionComponent);
    fx.detectChanges();

    httpMock.expectOne(`${API}/recepcion/panel`).flush({ ok: true, mensaje: '', data: PANEL });
    httpMock.expectOne(`${API}/citas`).flush({ mensaje: 'Sin permiso' }, { status: 403, statusText: 'Forbidden' });
    httpMock.expectOne(`${API}/mascotas`).flush({ ok: true, mensaje: '', data: MASCOTAS });
    httpMock.expectOne(`${API}/veterinarios/publico`).flush({ ok: true, mensaje: '', data: VETERINARIOS });
    httpMock.expectOne(`${API}/recepcion/flujo`).flush({ ok: true, mensaje: '', data: ETAPAS });

    expect(fx.componentInstance.citas.length).toBe(0);
    expect(fx.componentInstance.mensaje)
      .toBe('No se pudieron cargar citas. Verifica que Spring Boot este ejecutandose en el puerto 8080.');
  });

  it('informa del error cuando las mascotas no se pueden cargar', () => {
    const fx = TestBed.createComponent(RecepcionComponent);
    fx.detectChanges();

    httpMock.expectOne(`${API}/recepcion/panel`).flush({ ok: true, mensaje: '', data: PANEL });
    httpMock.expectOne(`${API}/citas`).flush({ ok: true, mensaje: '', data: CITAS });
    httpMock.expectOne(`${API}/mascotas`).flush({ mensaje: 'Sin permiso' }, { status: 403, statusText: 'Forbidden' });
    httpMock.expectOne(`${API}/veterinarios/publico`).flush({ ok: true, mensaje: '', data: VETERINARIOS });
    httpMock.expectOne(`${API}/recepcion/flujo`).flush({ ok: true, mensaje: '', data: ETAPAS });

    expect(fx.componentInstance.mascotas.length).toBe(0);
    expect(fx.componentInstance.mensaje).toBe('No se pudieron cargar mascotas. Revisa backend y permisos.');
  });

  it('informa del error cuando los veterinarios no se pueden cargar', () => {
    const fx = TestBed.createComponent(RecepcionComponent);
    fx.detectChanges();

    httpMock.expectOne(`${API}/recepcion/panel`).flush({ ok: true, mensaje: '', data: PANEL });
    httpMock.expectOne(`${API}/citas`).flush({ ok: true, mensaje: '', data: CITAS });
    httpMock.expectOne(`${API}/mascotas`).flush({ ok: true, mensaje: '', data: MASCOTAS });
    httpMock.expectOne(`${API}/veterinarios/publico`).flush({ mensaje: 'Sin permiso' }, { status: 403, statusText: 'Forbidden' });
    httpMock.expectOne(`${API}/recepcion/flujo`).flush({ ok: true, mensaje: '', data: ETAPAS });

    expect(fx.componentInstance.veterinarios.length).toBe(0);
    expect(fx.componentInstance.mensaje).toBe('No se pudieron cargar veterinarios.');
  });
});
