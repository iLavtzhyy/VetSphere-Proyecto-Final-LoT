import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { UsuariosComponent } from './usuarios';
import { AutorizacionService } from '../../core/autorizacion.service';
import { NotificacionService } from '../../core/notificacion.service';
import { Usuario } from '../../core/modelos';
import { environment } from '../../../environments/environment';

describe('UsuariosComponent', () => {
  let component: UsuariosComponent;
  let fixture: ComponentFixture<UsuariosComponent>;
  let httpMock: HttpTestingController;
  let autorizacion: AutorizacionService;
  let notificaciones: NotificacionService;

  const URL_USUARIOS = `${environment.apiUrl}/admin/usuarios`;
  const URL_SOLICITUDES = `${environment.apiUrl}/admin/usuarios/solicitudes-pendientes`;

  const USUARIOS: Usuario[] = [
    {
      id: 7, nombres: 'Ana', apellidos: 'Lopez', email: 'ana@vet.com', telefono: '0991111111',
      roles: [{ id: 1, nombre: 'ROLE_ADMIN' }], activo: true, emailVerificado: true, intentosFallidos: 0
    },
    {
      id: 8, nombres: 'Luis', apellidos: 'Perez', email: 'luis@vet.com', telefono: '0992222222',
      roles: [{ id: 2, nombre: 'ROLE_VETERINARIO' }], activo: true, emailVerificado: true,
      bloqueadoHasta: '2999-01-01T00:00:00', bloqueadoAhora: true, motivoBloqueo: 'Acceso suspendido'
    },
    {
      id: 9, nombres: 'Marta', apellidos: 'Ruiz', email: 'marta@vet.com', telefono: '0993333333',
      roles: [{ id: 4, nombre: 'ROLE_CLIENTE' }], activo: false, emailVerificado: false, intentosFallidos: 2
    }
  ];

  const SOLICITUDES: Usuario[] = [
    {
      id: 12, nombres: 'Carlos', apellidos: 'Vega', email: 'carlos@vet.com', telefono: '0994444444',
      roles: [{ id: 3, nombre: 'ROLE_RECEPCIONISTA' }]
    }
  ];

  /** Copias de los datos: el componente actualiza las filas en memoria. */
  function datosUsuarios(): Usuario[] {
    return USUARIOS.map(u => ({ ...u, roles: u.roles.map(r => ({ ...r })) }));
  }

  function datosSolicitudes(): Usuario[] {
    return SOLICITUDES.map(u => ({ ...u, roles: u.roles.map(r => ({ ...r })) }));
  }

  /** Vuelve a responder a las peticiones que dispara cargar(). */
  function recargar(): void {
    httpMock.expectOne({ method: 'GET', url: URL_USUARIOS })
      .flush({ ok: true, mensaje: '', data: datosUsuarios() });
    httpMock.expectOne({ method: 'GET', url: URL_SOLICITUDES })
      .flush({ ok: true, mensaje: '', data: datosSolicitudes() });
  }

  /** Texto de todos los botones renderizados. */
  function botones(): string[] {
    const lista: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('button'));
    return lista.map(b => (b.textContent || '').trim());
  }

  function textos(): string {
    return fixture.nativeElement.textContent || '';
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsuariosComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    autorizacion = TestBed.inject(AutorizacionService);
    notificaciones = TestBed.inject(NotificacionService);

    fixture = TestBed.createComponent(UsuariosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // Peticiones iniciales de ngOnInit.
    httpMock.expectOne({ method: 'GET', url: URL_USUARIOS })
      .flush({ ok: true, mensaje: '', data: datosUsuarios() });
    httpMock.expectOne({ method: 'GET', url: URL_SOLICITUDES })
      .flush({ ok: true, mensaje: '', data: datosSolicitudes() });
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('carga del servidor los usuarios y las solicitudes pendientes al iniciar', () => {
    expect(component.usuarios.length).toBe(3);
    expect(component.solicitudes.length).toBe(1);
    expect(component.cargando).toBe(false);
    expect(textos()).toContain('carlos@vet.com');
    expect(textos()).not.toContain('No hay solicitudes pendientes.');
    expect(textos()).toContain('Por aprobar');
  });

  it('cuenta los usuarios por rol y muestra el total en cada pestana', () => {
    expect(component.porRol('ROLE_ADMIN')).toBe(1);
    expect(component.porRol('ROLE_VETERINARIO')).toBe(1);
    expect(component.porRol('ROLE_CLIENTE')).toBe(1);
    expect(component.porRol('ROLE_RECEPCIONISTA')).toBe(0);

    expect(textos()).toContain('Todos (3)');
    expect(textos()).toContain('Administradores (1)');
    expect(textos()).toContain('Veterinarios (1)');
    expect(textos()).toContain('Clientes (1)');
    expect(textos()).toContain('Recepcionistas (0)');
  });

  it('filtra la tabla por el rol seleccionado', () => {
    component.rolSeleccionado = 'ROLE_CLIENTE';
    expect(component.filtrados.length).toBe(1);
    expect(component.filtrados[0].email).toBe('marta@vet.com');

    component.rolSeleccionado = 'ROLE_VETERINARIO';
    expect(component.filtrados.length).toBe(1);
    expect(component.filtrados[0].email).toBe('luis@vet.com');

    component.rolSeleccionado = 'TODOS';
    expect(component.filtrados.length).toBe(3);
  });

  it('filtra la tabla por el texto de busqueda sobre nombre, email, telefono o rol', () => {
    component.busqueda = 'marta';
    expect(component.filtrados.length).toBe(1);
    expect(component.filtrados[0].email).toBe('marta@vet.com');

    component.busqueda = '0992222222';
    expect(component.filtrados.length).toBe(1);
    expect(component.filtrados[0].email).toBe('luis@vet.com');

    component.busqueda = 'veterinario';
    expect(component.filtrados.length).toBe(1);
    expect(component.filtrados[0].email).toBe('luis@vet.com');

    component.busqueda = 'no-existe';
    expect(component.filtrados.length).toBe(0);
  });

  it('el administrador ve la opcion de desbloquear en la cuenta bloqueada', () => {
    autorizacion.guardarSesion({ token: 'x', roles: ['ROLE_ADMIN'] });
    expect(component.esAdmin).toBe(true);

    component.toggleMenu(8);
    fixture.detectChanges();

    expect(botones()).not.toContain('Bloquear');
    expect(botones()).toContain('Desbloquear');
  });

  it('un rol sin permiso como ROLE_CLIENTE no ve las acciones de bloqueo', () => {
    autorizacion.guardarSesion({ token: 'x', roles: ['ROLE_CLIENTE'] });
    expect(component.esAdmin).toBe(false);

    component.toggleMenu(8);
    fixture.detectChanges();

    expect(botones()).not.toContain('Bloquear');
    expect(botones()).not.toContain('Desbloquear');
    expect(botones()).not.toContain('Desactivar');
  });

  it('bloquea la cuenta enviando las horas y el motivo indicados', () => {
    component.abrirEstado('bloquear', USUARIOS[0]);
    component.horasBloqueo = 48;
    component.motivoEstado = 'Acceso no autorizado';

    component.confirmarEstado();

    const req = httpMock.expectOne({ method: 'PUT', url: `${URL_USUARIOS}/7/bloquear` });
    expect(req.request.body).toEqual({ horas: 48, motivo: 'Acceso no autorizado' });
    req.flush({ ok: true, mensaje: '', data: USUARIOS[0] });
    recargar();

    expect(component.modalEstadoAbierto).toBe(false);
    expect(component.procesandoEstado).toBe(false);
    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Cuenta de Ana Lopez bloqueada por 48 h.');
  });

  it('no bloquea la cuenta si las horas estan fuera del rango de 1 a 720', () => {
    component.abrirEstado('bloquear', USUARIOS[0]);
    component.motivoEstado = 'Acceso no autorizado';

    component.horasBloqueo = 0;
    component.confirmarEstado();
    httpMock.expectNone(`${URL_USUARIOS}/7/bloquear`);
    expect(component.errorEstado).toBe('Las horas de bloqueo deben ser un numero entero entre 1 y 720.');

    component.horasBloqueo = 900;
    component.confirmarEstado();
    httpMock.expectNone(`${URL_USUARIOS}/7/bloquear`);
    expect(component.errorEstado).toBe('Las horas de bloqueo deben ser un numero entero entre 1 y 720.');

    expect(component.procesandoEstado).toBe(false);
  });

  it('no bloquea la cuenta si el motivo tiene menos de 5 caracteres', () => {
    component.abrirEstado('bloquear', USUARIOS[0]);
    component.horasBloqueo = 24;
    component.motivoEstado = 'x';

    component.confirmarEstado();

    httpMock.expectNone(`${URL_USUARIOS}/7/bloquear`);
    expect(component.errorEstado).toBe('Describe el motivo del bloqueo (minimo 5 caracteres).');
    expect(component.procesandoEstado).toBe(false);
  });

  it('no bloquea la cuenta si el motivo supera los 300 caracteres', () => {
    component.abrirEstado('bloquear', USUARIOS[0]);
    component.horasBloqueo = 24;
    component.motivoEstado = 'a'.repeat(301);

    component.confirmarEstado();

    httpMock.expectNone(`${URL_USUARIOS}/7/bloquear`);
    expect(component.errorEstado).toBe('El motivo no puede superar los 300 caracteres.');
  });

  it('desbloquea la cuenta sin exigir motivo', () => {
    component.abrirEstado('desbloquear', USUARIOS[1]);
    component.confirmarEstado();

    const req = httpMock.expectOne({ method: 'PUT', url: `${URL_USUARIOS}/8/desbloquear` });
    expect(req.request.body).toEqual({});
    req.flush({ ok: true, mensaje: '', data: USUARIOS[1] });
    recargar();

    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Cuenta de Luis Perez desbloqueada.');
  });

  it('no desactiva la cuenta si el motivo tiene menos de 5 caracteres', () => {
    component.abrirEstado('desactivar', USUARIOS[0]);
    component.motivoEstado = 'mal';

    component.confirmarEstado();

    httpMock.expectNone(`${URL_USUARIOS}/7/desactivar`);
    expect(component.errorEstado).toBe('Describe el motivo de la desactivacion (minimo 5 caracteres).');
  });

  it('desactiva la cuenta con el motivo indicado', () => {
    component.abrirEstado('desactivar', USUARIOS[0]);
    component.motivoEstado = 'Solicitud del titular';

    component.confirmarEstado();

    const req = httpMock.expectOne({ method: 'PUT', url: `${URL_USUARIOS}/7/desactivar` });
    expect(req.request.body).toEqual({ motivo: 'Solicitud del titular' });
    req.flush({ ok: true, mensaje: '', data: { ...USUARIOS[0], activo: false } });
    recargar();

    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Cuenta de Ana Lopez desactivada.');
  });

  it('reactiva la cuenta con un motivo opcional', () => {
    component.abrirEstado('reactivar', USUARIOS[2]);
    component.confirmarEstado();

    const req = httpMock.expectOne({ method: 'PUT', url: `${URL_USUARIOS}/9/reactivar` });
    expect(req.request.body).toEqual({ motivo: undefined });
    req.flush({ ok: true, mensaje: '', data: { ...USUARIOS[2], activo: true } });
    recargar();

    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Cuenta de Marta Ruiz reactivada.');
  });

  it('el modal de estado muestra el titulo y la descripcion de cada modo', () => {
    component.abrirEstado('bloquear', USUARIOS[0]);
    expect(component.tituloEstado).toBe('Bloquear cuenta');
    expect(component.descripcionEstado).toBe('La cuenta no podra iniciar sesion durante el periodo indicado.');
    expect(component.textoBotonEstado).toBe('Bloquear cuenta');
    expect(component.claseBotonEstado).toBe('boton-peligro');
    expect(component.motivoObligatorio).toBe(true);
    expect(component.muestraHoras).toBe(true);

    component.abrirEstado('desbloquear', USUARIOS[0]);
    expect(component.tituloEstado).toBe('Desbloquear cuenta');
    expect(component.textoBotonEstado).toBe('Desbloquear ahora');
    expect(component.claseBotonEstado).toBe('boton-primario');
    expect(component.motivoObligatorio).toBe(false);
    expect(component.muestraHoras).toBe(false);

    component.abrirEstado('desactivar', USUARIOS[0]);
    expect(component.tituloEstado).toBe('Desactivar cuenta');
    expect(component.textoBotonEstado).toBe('Desactivar cuenta');

    component.abrirEstado('reactivar', USUARIOS[0]);
    expect(component.tituloEstado).toBe('Reactivar cuenta');
    expect(component.textoBotonEstado).toBe('Reactivar cuenta');
  });

  it('el badge de estado distingue activo, bloqueado y desactivado', () => {
    expect(component.bloqueoVigente(USUARIOS[0])).toBe(false);
    expect(component.textoEstado(USUARIOS[0])).toBe('Activo');
    expect(component.claseEstado(USUARIOS[0])).toBe('badge-ok');

    expect(component.bloqueoVigente(USUARIOS[1])).toBe(true);
    expect(component.textoEstado(USUARIOS[1])).toBe('Bloqueado hasta 01/01/2999 00:00');
    expect(component.claseEstado(USUARIOS[1])).toBe('badge-danger');

    expect(component.textoEstado(USUARIOS[2])).toBe('Desactivado');
    expect(component.claseEstado(USUARIOS[2])).toBe('badge-warn');
  });

  it('un bloqueo con fecha ya vencida no se considera vigente', () => {
    const vencido: Usuario = { ...USUARIOS[0], bloqueadoHasta: '2020-01-01T00:00:00' };
    expect(component.bloqueoVigente(vencido)).toBe(false);
    expect(component.textoEstado(vencido)).toBe('Activo');
  });

  it('formatea las fechas y devuelve sin fecha cuando el valor no es valido', () => {
    expect(component.formatearFecha('2026-03-07T08:05:00')).toBe('07/03/2026 08:05');
    expect(component.formatearFecha(null)).toBe('sin fecha');
    expect(component.formatearFecha('')).toBe('sin fecha');
    expect(component.formatearFecha('no-es-una-fecha')).toBe('sin fecha');
  });

  it('el nombre completo cae en el correo cuando no hay nombres ni apellidos', () => {
    expect(component.nombreCompleto(USUARIOS[0])).toBe('Ana Lopez');
    expect(component.nombreCompleto({ ...USUARIOS[0], nombres: '', apellidos: '' })).toBe('ana@vet.com');
  });

  it('aprueba una solicitud pendiente y recarga la lista', () => {
    component.aprobar(12);

    const req = httpMock.expectOne({ method: 'PUT', url: `${URL_USUARIOS}/12/aprobar` });
    expect(req.request.body).toEqual({});
    req.flush({ ok: true, mensaje: '', data: SOLICITUDES[0] });
    recargar();

    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Solicitud aprobada correctamente.');
  });

  it('cambia el rol de un usuario y actualiza la fila de la tabla', () => {
    component.cambiarRol(USUARIOS[2], 'ROLE_VETERINARIO');

    const req = httpMock.expectOne({ method: 'PUT', url: `${URL_USUARIOS}/9/roles` });
    expect(req.request.body).toEqual({ roles: ['ROLE_VETERINARIO'] });
    req.flush({
      ok: true, mensaje: '',
      data: { ...USUARIOS[2], roles: [{ id: 2, nombre: 'ROLE_VETERINARIO' }] }
    });
    fixture.detectChanges();

    const actualizado = component.usuarios.find(u => u.id === 9);
    expect(actualizado?.roles[0].nombre).toBe('ROLE_VETERINARIO');
    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Rol actualizado a ROLE_VETERINARIO.');
  });

  it('elimina permanentemente el usuario desactivado tras confirmar', () => {
    component.eliminarUsuario(9);
    expect(component.usuarioEliminarPendiente?.id).toBe(9);
    httpMock.expectNone(`${URL_USUARIOS}/9`);
    component.confirmarEliminarUsuario();
    const req = httpMock.expectOne({ method: 'DELETE', url: `${URL_USUARIOS}/9` });
    req.flush({ ok: true, mensaje: 'Usuario eliminado permanentemente.', data: null });
    httpMock.expectOne({ method: 'GET', url: URL_USUARIOS }).flush({ ok: true, mensaje: '', data: datosUsuarios().filter(usuario => usuario.id !== 9) });
    httpMock.expectOne({ method: 'GET', url: URL_SOLICITUDES }).flush({ ok: true, mensaje: '', data: datosSolicitudes() });

    expect(component.usuarioEliminarPendiente).toBeNull();
    expect(component.usuarios.some(usuario => usuario.id === 9)).toBe(false);
    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Marta Ruiz se eliminó permanentemente. Los historiales y comprobantes se conservaron sin asociacion a la cuenta.');
  });

  it('no vuelve a enviar la accion de estado mientras hay una en proceso', () => {
    component.abrirEstado('bloquear', USUARIOS[0]);
    component.motivoEstado = 'Acceso no autorizado';
    component.procesandoEstado = true;

    component.confirmarEstado();

    httpMock.expectNone(`${URL_USUARIOS}/7/bloquear`);
    expect(component.errorEstado).toBe('');
  });
});
