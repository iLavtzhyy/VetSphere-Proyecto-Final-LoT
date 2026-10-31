import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ReportesComponent } from './reportes';
import { Cirugia, HistorialClinico, Hospitalizacion, Mascota, OrdenLaboratorio, Usuario } from '../../core/modelos';
import { environment } from '../../../environments/environment';

describe('ReportesComponent', () => {
  let component: ReportesComponent;
  let fixture: ComponentFixture<ReportesComponent>;
  let httpMock: HttpTestingController;

  const URL_MASCOTAS = `${environment.apiUrl}/mascotas`;
  const URL_LABORATORIO = `${environment.apiUrl}/laboratorio`;
  const URL_HOSPITALIZACIONES = `${environment.apiUrl}/hospitalizaciones`;
  const URL_CIRUGIAS = `${environment.apiUrl}/cirugias`;

  const PROPIETARIO_ANA: Usuario = {
    id: 3, nombres: 'Ana', apellidos: 'Lopez', email: 'ana@vet.com', telefono: '0991111111', roles: []
  };

  const PROPIETARIO_LUIS: Usuario = {
    id: 4, nombres: 'Luis', apellidos: 'Perez', email: 'luis@vet.com', telefono: '0992222222', roles: []
  };

  const HISTORIALES_FIRULAIS: HistorialClinico[] = [
    {
      id: 100, fecha: '2026-01-10', tipoEvento: 'Consulta', motivo: 'Vacunacion anual',
      diagnostico: 'Sano', tratamiento: 'Vacuna antirrabica', temperatura: 38.2,
      frecuenciaCardiaca: 110, frecuenciaRespiratoria: 25
    },
    {
      id: 101, fecha: '2026-02-02', tipoEvento: 'Control', motivo: 'Dermatitis en la oreja',
      diagnostico: 'Otitis externa', tratamiento: 'Gotas Oticas', temperatura: 38.5,
      frecuenciaCardiaca: 105, frecuenciaRespiratoria: 24
    }
  ];

  const MASCOTAS: Mascota[] = [
    {
      id: 1, nombre: 'Firulais', especie: 'Perro', raza: 'Chihuahua', sexo: 'Macho', color: 'Blanco',
      pesoKg: 4.5, fechaNacimiento: '2020-05-10', estado: 'ACTIVA', propietario: PROPIETARIO_ANA,
      historiales: HISTORIALES_FIRULAIS, cirugias: null
    },
    {
      id: 2, nombre: 'Michi', especie: 'Gato', raza: 'Siamés', sexo: 'Hembra', color: 'Gris',
      pesoKg: 3.2, fechaNacimiento: '2021-08-21', estado: 'EN HOSPITALIZACION', propietario: PROPIETARIO_LUIS,
      historiales: [
        {
          id: 102, fecha: '2026-03-01', tipoEvento: 'Consulta', motivo: 'Inapetencia',
          diagnostico: 'Gastritis', tratamiento: 'Dieta suave', temperatura: 39.1,
          frecuenciaCardiaca: 180, frecuenciaRespiratoria: 30
        }
      ],
      cirugias: null
    }
  ];

  const LABORATORIOS: OrdenLaboratorio[] = [
    { id: 200, fecha: '2026-02-10', tipoPrueba: 'Hemograma', resultado: 'Normal', estado: 'COMPLETADO', mascota: { id: 1 } as unknown as Mascota },
    { id: 201, fecha: '2026-03-02', tipoPrueba: 'Perfil quimico', resultado: 'Pendiente', estado: 'PENDIENTE', mascota: { id: 2 } as unknown as Mascota }
  ];

  const HOSPITALIZACIONES: Hospitalizacion[] = [
    { id: 300, ingreso: '2026-03-01', jaula: 'A-3', motivo: 'Deshidratacion', planCuidados: 'Fluidoterapia', estado: 'EN CURSO', mascota: { id: 2 } as unknown as Mascota }
  ];

  const CIRUGIAS: Cirugia[] = [
    {
      id: 400, fechaProgramada: '2026-01-05', procedimiento: 'Castracion', tipoAnestesia: 'General',
      estadoPostoperatorio: 'Sin complicaciones', estado: 'FINALIZADA', constantesVitales: 'T 37.8',
      mascota: { id: 1 } as unknown as Mascota
    }
  ];

  function textos(): string {
    return fixture.nativeElement.textContent || '';
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportesComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(ReportesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // Peticiones iniciales de ngOnInit.
    httpMock.expectOne({ method: 'GET', url: URL_MASCOTAS })
      .flush({ ok: true, mensaje: '', data: MASCOTAS });
    httpMock.expectOne({ method: 'GET', url: URL_LABORATORIO })
      .flush({ ok: true, mensaje: '', data: LABORATORIOS });
    httpMock.expectOne({ method: 'GET', url: URL_HOSPITALIZACIONES })
      .flush({ ok: true, mensaje: '', data: HOSPITALIZACIONES });
    httpMock.expectOne({ method: 'GET', url: URL_CIRUGIAS })
      .flush({ ok: true, mensaje: '', data: CIRUGIAS });
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('carga del servidor las mascotas, los laboratorios, las hospitalizaciones y las cirugias', () => {
    expect(component.mascotas.length).toBe(2);
    expect(component.todosLabs.length).toBe(2);
    expect(component.todasHosp.length).toBe(1);
    expect(component.cirugiasRegistradas.length).toBe(1);
    expect(component.cargando).toBe(false);
  });

  it('muestra los totales de pacientes, registros clinicos y cirugias del servidor', () => {
    expect(textos()).toContain('Pacientes totales');
    expect(textos()).toContain('Mascotas registradas');
    expect(textos()).toContain('Registros clínicos');
    expect(textos()).toContain('En historiales de pacientes');
    expect(textos()).toContain('Cirugías');
    expect(textos()).toContain('Registradas en el sistema');
  });

  it('el total de eventos clinicos suma los historiales de todas las mascotas', () => {
    expect(component.totalEventosClinicos).toBe(3);

    component.mascotas = MASCOTAS.map(m => ({ ...m, historiales: null }));
    expect(component.totalEventosClinicos).toBe(0);

    component.mascotas = [{ ...MASCOTAS[0], historiales: [] }, MASCOTAS[1]];
    expect(component.totalEventosClinicos).toBe(1);
  });

  it('el listado muestra el nombre, la ficha y el propietario que devuelve el servidor', () => {
    expect(textos()).toContain('Firulais');
    expect(textos()).toContain('HC-1');
    expect(textos()).toContain('Perro - Chihuahua');
    expect(textos()).toContain('Ana Lopez');
    expect(textos()).toContain('Michi');
    expect(textos()).toContain('HC-2');
    expect(textos()).toContain('Gato - Siamés');
    expect(textos()).toContain('Luis Perez');
  });

  it('deja el historial cerrado al abrir reportes hasta elegir un paciente', () => {
    expect(component.mascotaSeleccionada).toBeUndefined();
    expect(textos()).toContain('Selecciona una mascota');
  });

  it('filtra las mascotas por nombre, especie o propietario', () => {
    component.busqueda = 'michi';
    expect(component.filtradas.length).toBe(1);
    expect(component.filtradas[0].id).toBe(2);

    component.busqueda = 'perro';
    expect(component.filtradas.length).toBe(1);
    expect(component.filtradas[0].id).toBe(1);

    component.busqueda = 'ana';
    expect(component.filtradas.length).toBe(1);
    expect(component.filtradas[0].id).toBe(1);

    component.busqueda = 'no-existe';
    expect(component.filtradas.length).toBe(0);
  });

  it('filtra los laboratorios y las hospitalizaciones de la mascota seleccionada', () => {
    expect(component.obtenerLabsPaciente(1).length).toBe(1);
    expect(component.obtenerLabsPaciente(1)[0].tipoPrueba).toBe('Hemograma');
    expect(component.obtenerLabsPaciente(2).length).toBe(1);
    expect(component.obtenerHospPaciente(1).length).toBe(0);
    expect(component.obtenerHospPaciente(2).length).toBe(1);
    expect(component.obtenerHospPaciente(2)[0].jaula).toBe('A-3');
  });

  it('el comprobante muestra los datos de la mascota y sus consultas registradas', () => {
    component.seleccionar(MASCOTAS[0]);
    fixture.detectChanges();
    expect(textos()).toContain('Historial Clínico del Paciente');
    expect(textos()).toContain('Código: HC-1');
    expect(textos()).toContain('4.5 kg - Macho');
    expect(textos()).toContain('2 registradas');
    expect(textos()).toContain('Vacunacion anual');
    expect(textos()).toContain('Otitis externa');
  });

  it('cierra la vista previa del historial y vuelve a pedir una mascota', () => {
    component.cerrarVistaPrevia();
    fixture.detectChanges();

    expect(component.mascotaSeleccionada).toBeUndefined();
    expect(textos()).toContain('Selecciona una mascota');
    expect(textos()).toContain('Elige un paciente del listado para revisar su historial médico y opciones documentales.');
  });

  it('selecciona otra mascota del listado', () => {
    component.seleccionar(MASCOTAS[1]);
    fixture.detectChanges();

    expect(component.mascotaSeleccionada?.id).toBe(2);
    expect(textos()).toContain('Código: HC-2');
  });

  it('descarga el historial en PDF desde el endpoint del servidor', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob:mock');
    spyOn(URL, 'revokeObjectURL');
    spyOn(HTMLAnchorElement.prototype, 'click');

    component.descargarPdf(MASCOTAS[0]);

    const req = httpMock.expectOne({ method: 'GET', url: `${environment.apiUrl}/reportes/mascotas/1/historial.pdf` });
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['pdf']));
  });

  it('descarga el excel de finanzas desde el endpoint del servidor', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob:mock');
    spyOn(URL, 'revokeObjectURL');
    spyOn(HTMLAnchorElement.prototype, 'click');

    component.descargarExcel();

    const req = httpMock.expectOne({ method: 'GET', url: `${environment.apiUrl}/reportes/finanzas.xlsx` });
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['xlsx']));
  });

  it('el comprobante impreso escapa el contenido del historial para evitar inyeccion de scripts', () => {
    const ventana: any = {
      document: { write: jasmine.createSpy('write'), close: jasmine.createSpy('close') }
    };
    spyOn(window, 'open').and.returnValue(ventana);

    const mascota: Mascota = {
      ...MASCOTAS[0],
      historiales: [
        {
          ...HISTORIALES_FIRULAIS[0],
          motivo: '<script>alert(1)</script>',
          diagnostico: '<img src=x onerror=alert(2)>'
        }
      ]
    };

    component.imprimirHistorial(mascota);

    expect(window.open).toHaveBeenCalled();
    const html: string = ventana.document.write.calls.mostRecent().args[0];
    expect(html).toContain('&lt;script&gt;alert(1)&lt;/script&gt;');
    expect(html).toContain('&lt;img src=x onerror=alert(2)&gt;');
    expect(html).not.toContain('<script>alert(1)</script>');
  });
});
