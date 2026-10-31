import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MascotasComponent } from './mascotas';
import { AutorizacionService } from '../../core/autorizacion.service';
import { NotificacionService } from '../../core/notificacion.service';
import { Cliente, Mascota } from '../../core/modelos';
import { environment } from '../../../environments/environment';

const API = environment.apiUrl;

const CLIENTES: Cliente[] = [
  { id: 10, nombres: 'Ana', apellidos: 'Ruiz', email: 'ana@correo.com', telefono: '0999999999', ciudad: 'Quito', estado: 'Activo', roles: [] },
  { id: 11, nombres: 'Luis', apellidos: 'Perez', email: 'luis@correo.com', telefono: '0888888888', ciudad: 'Quito', estado: 'Activo', roles: [] }
];

const MASCOTAS: Mascota[] = [
  {
    id: 1, nombre: 'Firulais', especie: 'Canino', raza: 'Golden Retriever', sexo: 'Macho', color: 'Dorado',
    pesoKg: 12.5, fechaNacimiento: '2020-05-10', estado: 'ACTIVA',
    propietario: { id: 10, nombres: 'Ana', apellidos: 'Ruiz', email: 'ana@correo.com', telefono: '0999999999', roles: [] },
    historiales: [
      {
        id: 100, fecha: '2026-01-05T10:00:00', tipoEvento: 'CONSULTA', motivo: 'Vacunacion anual',
        diagnostico: 'Sano', tratamiento: 'Vacuna aplicada', temperatura: 38.5,
        frecuenciaCardiaca: 90, frecuenciaRespiratoria: 25, archivosUrl: '/uploads/radiografia.pdf'
      }
    ],
    cirugias: [
      {
        id: 200, fechaProgramada: '2026-02-01T09:00:00', procedimiento: 'Castracion', tipoAnestesia: 'General',
        estadoPostoperatorio: 'En recuperacion', estado: 'COMPLETADA', constantesVitales: 'Estables'
      }
    ]
  },
  {
    id: 2, nombre: 'Michi', especie: 'Felino', raza: 'Siames', sexo: 'Hembra', color: 'Blanco',
    pesoKg: 4, fechaNacimiento: '2022-01-05', estado: 'INTERNADA',
    propietario: { id: 11, nombres: 'Luis', apellidos: 'Perez', email: 'luis@correo.com', telefono: '0888888888', roles: [] },
    historiales: null, cirugias: null
  }
];

describe('MascotasComponent', () => {
  let component: MascotasComponent;
  let fixture: ComponentFixture<MascotasComponent>;
  let httpMock: HttpTestingController;
  let autorizacion: AutorizacionService;
  let notificaciones: NotificacionService;

  /** Crea un componente con los roles indicados y responde a la carga inicial. */
  function crearComponente(roles: string[] = ['ROLE_ADMIN']): ComponentFixture<MascotasComponent> {
    autorizacion.guardarSesion({ token: 'tok-admin', roles });
    const fx = TestBed.createComponent(MascotasComponent);
    fx.detectChanges();
    httpMock.expectOne(`${API}/mascotas`).flush({ ok: true, mensaje: '', data: structuredClone(MASCOTAS) });
    if (roles.some(r => ['ROLE_ADMIN', 'ROLE_RECEPCIONISTA', 'ROLE_VETERINARIO'].includes(r))) {
      httpMock.expectOne(`${API}/clientes`).flush({ ok: true, mensaje: '', data: CLIENTES });
    }
    fx.detectChanges();
    return fx;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MascotasComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    autorizacion = TestBed.inject(AutorizacionService);
    notificaciones = TestBed.inject(NotificacionService);

    fixture = crearComponente();
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('carga las mascotas y los clientes en el arranque', () => {
    expect(component.mascotas.length).toBe(2);
    expect(component.clientes.length).toBe(2);
    expect(component.cargando).toBe(false);
  });

  it('selecciona la primera mascota como ficha visible al cargar', () => {
    expect(component.seleccionada?.id).toBe(1);
    expect(component.seleccionada?.nombre).toBe('Firulais');
  });

  it('no solicita los clientes cuando el usuario no tiene un rol de gestion', () => {
    autorizacion.limpiarSesion();
    const fx = TestBed.createComponent(MascotasComponent);
    fx.detectChanges();

    httpMock.expectOne(`${API}/mascotas`).flush({ ok: true, mensaje: '', data: structuredClone(MASCOTAS) });
    httpMock.expectNone(`${API}/clientes`);
    expect(fx.componentInstance.puedeGestionarClientes).toBe(false);
    expect(fx.componentInstance.clientes.length).toBe(0);
  });

  it('el administrador si puede gestionar las mascotas', () => {
    expect(component.puedeGestionarClientes).toBe(true);
  });

  it('muestra el contador de mascotas registradas y la ficha de la seleccionada', () => {
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('2 mascotas registradas');
    expect(texto).toContain('Canino - Macho - Dorado - 12.5 kg');
    expect(texto).toContain('Propietario: Ana Ruiz');
    expect(texto).toContain('ACTIVA');
  });

  it('filtra las mascotas por nombre', () => {
    component.busqueda = 'mich';
    expect(component.filtradas.length).toBe(1);
    expect(component.filtradas[0].nombre).toBe('Michi');
  });

  it('filtra las mascotas por especie', () => {
    component.busqueda = 'felino';
    expect(component.filtradas.map(m => m.id)).toEqual([2]);
  });

  it('filtra las mascotas por raza', () => {
    component.busqueda = 'golden';
    expect(component.filtradas.map(m => m.id)).toEqual([1]);
  });

  it('filtra las mascotas por el nombre del propietario', () => {
    component.busqueda = 'perez';
    expect(component.filtradas.map(m => m.id)).toEqual([2]);
  });

  it('devuelve una lista vacia cuando la busqueda no coincide con ninguna mascota', () => {
    component.busqueda = 'inexistente';
    expect(component.filtradas.length).toBe(0);
  });

  it('selecciona la mascota pulsada en la lista', () => {
    component.seleccionarMascota(component.mascotas[1]);
    expect(component.seleccionada?.id).toBe(2);
  });

  it('ordena la linea de tiempo clinica de la mascota por fecha descendente', () => {
    const eventos = component.eventos(component.mascotas[0]);
    expect(eventos.length).toBe(2);
    expect(eventos[0].titulo).toBe('Castracion');
    expect(eventos[0].tipo).toBe('CIRUGIA');
    expect(eventos[1].titulo).toBe('Vacunacion anual');
    expect(eventos[1].tipo).toBe('CONSULTA');
  });

  it('la linea de tiempo clinica queda vacia si la mascota no tiene historial ni cirugias', () => {
    expect(component.eventos(component.mascotas[1]).length).toBe(0);
  });

  it('solo cuenta como archivo clinico los historiales con archivo adjunto', () => {
    expect(component.archivosClinicos(component.mascotas[0]).length).toBe(1);
    expect(component.archivosClinicos(component.mascotas[1]).length).toBe(0);
  });

  it('extrae el nombre del archivo y el icono segun su extension', () => {
    expect(component.nombreArchivo('/uploads/radiografia.pdf')).toBe('radiografia.pdf');
    expect(component.nombreArchivo(undefined)).toBe('archivo-medico');
    expect(component.iconoArchivo('/uploads/radiografia.pdf')).toBe('PDF');
    expect(component.iconoArchivo('/uploads/foto.JPG')).toBe('IMG');
    expect(component.iconoArchivo('/uploads/hoja.txt')).toBe('DOC');
  });

  it('usa la foto de la mascota y una imagen por defecto cuando no tiene fotoUrl', () => {
    expect(component.fotoMascota(component.mascotas[0])).toContain('images.unsplash.com');
    component.mascotas[0].fotoUrl = 'http://localhost:8080/uploads/firulais.jpg';
    expect(component.fotoMascota(component.mascotas[0])).toContain('images.unsplash.com');
  });

  it('carga la foto protegida con la sesión y la presenta desde un blob', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob:foto-firulais');
    const conFoto = MASCOTAS.map(mascota => ({ ...mascota }));
    conFoto[0].fotoUrl = '/uploads/firulais.jpg';

    component.cargar();
    httpMock.expectOne(`${API}/mascotas`).flush({ ok: true, mensaje: '', data: conFoto });
    const archivo = httpMock.expectOne('http://localhost:8080/uploads/firulais.jpg');
    expect(archivo.request.responseType).toBe('blob');
    archivo.flush(new Blob(['imagen'], { type: 'image/jpeg' }));

    expect(component.fotoMascota(conFoto[0])).toBe('blob:foto-firulais');
  });

  it('abre adjuntos usando una solicitud autenticada en lugar de navegar a la URL protegida', () => {
    const reemplazar = jasmine.createSpy('replace');
    const popup = {
      opener: window,
      closed: false,
      document: { title: '', body: { textContent: '' } },
      location: { replace: reemplazar },
      close: jasmine.createSpy('close')
    } as any;
    spyOn(window, 'open').and.returnValue(popup);
    spyOn(URL, 'createObjectURL').and.returnValue('blob:archivo-clinico');

    component.abrirArchivo('/uploads/radiografia.pdf');

    const archivo = httpMock.expectOne('http://localhost:8080/uploads/radiografia.pdf');
    expect(archivo.request.method).toBe('GET');
    expect(archivo.request.responseType).toBe('blob');
    archivo.flush(new Blob(['pdf'], { type: 'application/pdf' }));

    expect(reemplazar).toHaveBeenCalledWith('blob:archivo-clinico');
  });

  it('abre el formulario de alta con el primer cliente como propietario por defecto', () => {
    component.nuevo();
    expect(component.formularioAbierto).toBe(true);
    expect(component.editandoId).toBeUndefined();
    expect(component.form.nombre).toBe('');
    expect(component.form.especie).toBe('Canino');
    expect(component.form.sexo).toBe('Macho');
    expect(component.form.estado).toBe('ACTIVA');
    expect(component.form.propietarioId).toBe(10);
  });

  it('carga los datos de la mascota en el formulario de edicion', () => {
    component.editar(component.mascotas[1]);
    expect(component.editandoId).toBe(2);
    expect(component.formularioAbierto).toBe(true);
    expect(component.form.nombre).toBe('Michi');
    expect(component.form.especie).toBe('Felino');
    expect(component.form.sexo).toBe('Hembra');
    expect(component.form.pesoKg).toBe(4);
    expect(component.form.propietarioId).toBe(11);
    expect(component.form.estado).toBe('INTERNADA');
  });

  it('registra una mascota nueva enviando el peso como numero y la fecha vacia como null', async () => {
    component.nuevo();
    component.form.nombre = 'Nube';
    component.form.raza = 'Siames';
    component.form.pesoKg = '3.4' as unknown as number;
    component.form.fechaNacimiento = '';

    let mensajeAlGuardar = '';
    spyOn(component, 'cerrarFormulario').and.callFake(() => {
      mensajeAlGuardar = component.mensajeFormulario;
      component.formularioAbierto = false;
      component.editandoId = undefined;
      component.mensajeFormulario = '';
    });

    component.guardar();

    const peticion = httpMock.expectOne(`${API}/mascotas`);
    expect(peticion.request.method).toBe('POST');
    const cuerpo = peticion.request.body as FormData;
    expect(cuerpo.get('mascota')).toBeTruthy();
    const datos = JSON.parse(await (cuerpo.get('mascota') as Blob).text());
    expect(datos.nombre).toBe('Nube');
    expect(datos.raza).toBe('Siames');
    expect(datos.pesoKg).toBe(3.4);
    expect(datos.fechaNacimiento).toBeNull();
    expect(datos.propietarioId).toBe(10);

    peticion.flush({ ok: true, mensaje: '', data: { ...MASCOTAS[0], id: 3, nombre: 'Nube' } });
    httpMock.expectOne(`${API}/mascotas`).flush({ ok: true, mensaje: '', data: MASCOTAS });

    expect(mensajeAlGuardar).toBe('Mascota registrada correctamente.');
    expect(component.formularioAbierto).toBe(false);
  });

  it('actualiza la mascota en edicion contra el endpoint de la mascota', () => {
    component.editar(component.mascotas[0]);
    component.form.nombre = 'Firulais II';

    component.guardar();

    const peticion = httpMock.expectOne(`${API}/mascotas/1`);
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body.nombre).toBe('Firulais II');
    expect(peticion.request.body.pesoKg).toBe(12.5);

    peticion.flush({ ok: true, mensaje: '', data: { ...MASCOTAS[0], nombre: 'Firulais II' } });
    httpMock.expectOne(`${API}/mascotas`).flush({ ok: true, mensaje: '', data: MASCOTAS });

    expect(component.formularioAbierto).toBe(false);
    expect(component.editandoId).toBeUndefined();
  });

  it('informa del error cuando la mascota no se puede registrar', () => {
    component.nuevo();
    component.form.nombre = 'Nube';
    component.guardar();

    const peticion = httpMock.expectOne(`${API}/mascotas`);
    peticion.flush({ mensaje: 'Error del servidor' }, { status: 500, statusText: 'Server Error' });

    expect(component.mensajeFormulario).toBe('No se pudo registrar la mascota.');
    expect(component.formularioAbierto).toBe(true);
  });

  it('informa del error cuando la mascota no se puede actualizar', () => {
    component.editar(component.mascotas[0]);
    component.guardar();

    const peticion = httpMock.expectOne(`${API}/mascotas/1`);
    peticion.flush({ mensaje: 'Error del servidor' }, { status: 500, statusText: 'Server Error' });

    expect(component.mensajeFormulario).toBe('No se pudo actualizar la mascota.');
  });

  it('elimina la mascota desde el cuadro de confirmacion centrado', () => {
    component.eliminar(component.mascotas[0]);
    expect(component.mascotaPendienteEliminar?.nombre).toBe('Firulais');
    component.confirmarEliminacion();
    const peticion = httpMock.expectOne(`${API}/mascotas/1`);
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush({ ok: true, mensaje: '', data: null });

    expect(component.mascotas.length).toBe(1);
    expect(component.seleccionada?.id).toBe(2);
  });

  it('cancela la eliminacion sin enviar la peticion', () => {
    component.eliminar(component.mascotas[0]);
    component.cancelarEliminacion();

    httpMock.expectNone(`${API}/mascotas/1`);
    expect(component.mascotaPendienteEliminar).toBeUndefined();
    expect(component.mascotas.length).toBe(2);
  });

  it('notifica el error cuando el backend rechaza el borrado de la mascota', () => {
    component.eliminar(component.mascotas[0]);
    component.confirmarEliminacion();
    const peticion = httpMock.expectOne(`${API}/mascotas/1`);
    peticion.flush({ mensaje: 'No se puede eliminar' }, { status: 409, statusText: 'Conflict' });

    expect(notificaciones.notificaciones().map(n => n.texto)).toContain('No se puede eliminar');
    expect(component.mascotas.length).toBe(2);
  });

  it('sube el archivo al historial de la mascota seleccionada', async () => {
    const archivo = new File(['contenido'], 'radiografia.pdf', { type: 'application/pdf' });

    component.seleccionada = component.mascotas[0];
    component.subir(archivo);

    const peticion = httpMock.expectOne(`${API}/mascotas/1/historial`);
    expect(peticion.request.method).toBe('POST');
    const cuerpo = peticion.request.body as FormData;
    expect(cuerpo.get('archivo')).toBe(archivo);
    const historial = JSON.parse(await (cuerpo.get('historial') as Blob).text());
    expect(historial.tipoEvento).toBe('ARCHIVO MEDICO');
    expect(historial.motivo).toBe('Archivo cargado: radiografia.pdf');
    expect(historial.veterinarioId).toBeNull();

    peticion.flush({ ok: true, mensaje: '', data: { archivosUrl: '/uploads/radiografia.pdf' } });
    httpMock.expectOne(`${API}/mascotas`).flush({ ok: true, mensaje: '', data: MASCOTAS });

    expect(component.mensajeArchivo).toBe('Archivo guardado en historial: radiografia.pdf');
  });

  it('no sube ningun archivo si no hay mascota seleccionada', () => {
    component.seleccionada = undefined;
    component.subir(new File(['contenido'], 'radiografia.pdf', { type: 'application/pdf' }));
    httpMock.expectNone(`${API}/mascotas/1/historial`);
  });

  it('elimina el archivo clinico de la mascota cuando se confirma', () => {
    component.eliminarArchivo(component.mascotas[0], 100, 'radiografia.pdf');
    expect(component.archivoPendienteEliminar?.nombre).toBe('radiografia.pdf');
    httpMock.expectNone(`${API}/mascotas/1/historial/100/archivo`);

    component.confirmarEliminacionArchivo();
    const peticion = httpMock.expectOne(`${API}/mascotas/1/historial/100/archivo`);
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush({ ok: true, mensaje: '', data: null });
    httpMock.expectOne(`${API}/mascotas`).flush({ ok: true, mensaje: '', data: MASCOTAS });

    expect(component.mensajeArchivo).toBe('Archivo eliminado del historial.');
    expect(component.archivoPendienteEliminar).toBeUndefined();
  });

  it('no borra el archivo clinico si el usuario cancela la confirmacion', () => {
    component.eliminarArchivo(component.mascotas[0], 100, 'radiografia.pdf');
    component.cancelarEliminacionArchivo();

    expect(component.archivoPendienteEliminar).toBeUndefined();
    httpMock.expectNone(`${API}/mascotas/1/historial/100/archivo`);
  });

  it('cierra la confirmacion de archivo con Escape', () => {
    component.eliminarArchivo(component.mascotas[0], 100, 'radiografia.pdf');
    component.cerrarFormularioConEscape();

    expect(component.archivoPendienteEliminar).toBeUndefined();
    httpMock.expectNone(`${API}/mascotas/1/historial/100/archivo`);
  });

  it('descarga el historial en PDF de la mascota seleccionada', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob:fake');
    spyOn(URL, 'revokeObjectURL');

    component.descargarPdf(1);

    const peticion = httpMock.expectOne(`${API}/reportes/mascotas/1/historial.pdf`);
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.responseType).toBe('blob');
    peticion.flush(new Blob(['pdf']));
  });

  it('cierra el formulario con la tecla Escape', () => {
    component.nuevo();
    expect(component.formularioAbierto).toBe(true);
    component.cerrarFormularioConEscape();
    expect(component.formularioAbierto).toBe(false);
  });

  it('informa del error cuando el backend no responde al listar las mascotas', () => {
    autorizacion.limpiarSesion();
    const fx = TestBed.createComponent(MascotasComponent);
    fx.detectChanges();

    httpMock.expectOne(`${API}/mascotas`).flush({ mensaje: 'Sin permiso' }, { status: 403, statusText: 'Forbidden' });

    expect(fx.componentInstance.mascotas.length).toBe(0);
    expect(fx.componentInstance.seleccionada).toBeUndefined();
    expect(fx.componentInstance.mensajeFormulario)
      .toBe('No se pudieron cargar mascotas. Revisa que el backend este encendido y tu rol tenga permiso.');
  });
});
