import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ChatbotComponent } from './chatbot';
import { AutorizacionService } from '../../core/autorizacion.service';
import { environment } from '../../../environments/environment';

describe('ChatbotComponent', () => {
  let component: ChatbotComponent;
  let fixture: ComponentFixture<ChatbotComponent>;
  let httpMock: HttpTestingController;
  let autorizacion: AutorizacionService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChatbotComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(ChatbotComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    autorizacion = TestBed.inject(AutorizacionService);
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/chat/conversacion-actual`)
      .flush({ ok: true, mensaje: '', data: null });
  });

  afterEach(() => httpMock.verify());

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('enviar publica el mensaje y muestra la respuesta del asistente', () => {
    component.abierto = true;
    fixture.detectChanges();
    component.texto = '¿Cuántas citas hay hoy?';
    component.enviar();

    const peticion = httpMock.expectOne(`${environment.apiUrl}/chat`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ pregunta: '¿Cuántas citas hay hoy?', conversacionId: null });
    peticion.flush({ ok: true, mensaje: '', data: { respuesta: 'Hay 4 citas hoy.', conversacionId: 12, herramientas: ['contar_citas'] } });

    expect(component.enviando).toBe(false);
    expect(component.mensajes.length).toBe(2);
    expect(component.mensajes[0].rol).toBe('user');
    expect(component.mensajes[0].contenido).toBe('¿Cuántas citas hay hoy?');
    expect(component.mensajes[1].rol).toBe('assistant');
    expect(component.mensajes[1].contenido).toBe('Hay 4 citas hoy.');
    expect(component.mensajes[1].error).toBeFalsy();
    expect(component.historial).toEqual([
      { rol: 'user', contenido: '¿Cuántas citas hay hoy?' },
      { rol: 'assistant', contenido: 'Hay 4 citas hoy.' }
    ]);
  });

  it('continua la conversación persistida en peticiones posteriores', () => {
    component.texto = 'hola';
    component.enviar();
    httpMock.expectOne(`${environment.apiUrl}/chat`)
      .flush({ ok: true, mensaje: '', data: { respuesta: 'Buenas tardes', conversacionId: 8, herramientas: [] } });

    component.texto = 'citas de mañana';
    component.enviar();
    const segunda = httpMock.expectOne(`${environment.apiUrl}/chat`);
    expect(segunda.request.body).toEqual({ pregunta: 'citas de mañana', conversacionId: 8 });
    segunda.flush({ ok: true, mensaje: '', data: { respuesta: 'Aún no hay citas.', herramientas: [] } });
  });

  it('recupera los mensajes guardados al volver a cargar el widget', () => {
    component.ngOnInit();
    httpMock.expectOne(`${environment.apiUrl}/chat/conversacion-actual`)
      .flush({ ok: true, mensaje: '', data: {
        id: 44, usuarioEmail: 'ana@vet.test', nombreUsuario: 'Ana', titulo: 'Caso de Luna',
        actualizadoEn: '2026-10-22T10:00:00', mensajes: [
          { rol: 'user', contenido: 'Recuerda a Luna', fecha: '2026-10-22T09:00:00' },
          { rol: 'assistant', contenido: 'Lo tengo presente.', fecha: '2026-10-22T09:00:01' }
        ]
      } });

    expect(component.conversacionId).toBe(44);
    expect(component.mensajes.map(m => m.contenido)).toEqual(['Recuerda a Luna', 'Lo tengo presente.']);
  });

  it('permite al administrador abrir y consultar el historial global', () => {
    autorizacion.guardarSesion({ token: 'admin', roles: ['ROLE_ADMIN'] });
    component.abrirPanelAdmin();
    httpMock.expectOne(`${environment.apiUrl}/chat/admin/conversaciones`).flush({ ok: true, mensaje: '', data: [
      { id: 2, usuarioEmail: 'cliente@vet.test', nombreUsuario: 'Cliente', titulo: 'Consulta',
        actualizadoEn: '2026-10-22T10:00:00', cantidadMensajes: 4 }
    ] });
    expect(component.conversacionesAdmin.length).toBe(1);

    component.verConversacionAdmin(2);
    httpMock.expectOne(`${environment.apiUrl}/chat/admin/conversaciones/2`).flush({ ok: true, mensaje: '', data: {
      id: 2, usuarioEmail: 'cliente@vet.test', nombreUsuario: 'Cliente', titulo: 'Consulta',
      actualizadoEn: '2026-10-22T10:00:00', mensajes: []
    } });
    expect(component.conversacionAdmin?.id).toBe(2);
  });

  it('permite solo al administrador eliminar una conversación del panel', () => {
    autorizacion.guardarSesion({ token: 'admin', roles: ['ROLE_ADMIN'] });
    component.conversacionesAdmin = [
      { id: 2, usuarioEmail: 'cliente@vet.test', nombreUsuario: 'Cliente', titulo: 'Consulta',
        actualizadoEn: '2026-10-22T10:00:00', cantidadMensajes: 4 }
    ];

    component.eliminarConversacionAdmin(2);
    expect(component.modalBorrado?.tipo).toBe('conversacion');
    component.confirmarBorrado();
    const peticion = httpMock.expectOne(`${environment.apiUrl}/chat/admin/conversaciones/2`);
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush({ ok: true, mensaje: 'Conversación eliminada', data: null });

    expect(component.conversacionesAdmin).toEqual([]);
    expect(component.mensajeAdmin).toBe('Conversación eliminada');
  });

  it('elimina todas las conversaciones del panel usando DELETE en la ruta compatible', () => {
    autorizacion.guardarSesion({ token: 'admin', roles: ['ROLE_ADMIN'] });
    component.conversacionesAdmin = [
      { id: 2, usuarioEmail: 'cliente@vet.test', nombreUsuario: 'Cliente', titulo: 'Consulta',
        actualizadoEn: '2026-10-22T10:00:00', cantidadMensajes: 4 }
    ];

    component.eliminarTodoHistorialAdmin();
    expect(component.modalBorrado?.tipo).toBe('todo');
    component.confirmarBorrado();
    const peticion = httpMock.expectOne(`${environment.apiUrl}/chat/admin/conversaciones`);
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush({ ok: true, mensaje: 'Historial eliminado', data: 1 });

    expect(component.conversacionesAdmin).toEqual([]);
    expect(component.mensajeAdmin).toBe('1 conversación eliminada.');
    expect(component.eliminandoHistorial).toBe(false);
  });

  it('impide que usuarios sin rol administrador borren el historial', () => {
    autorizacion.guardarSesion({ token: 'recepcion', roles: ['ROLE_RECEPCIONISTA'] });
    component.conversacionesAdmin = [
      { id: 2, usuarioEmail: 'cliente@vet.test', nombreUsuario: 'Cliente', titulo: 'Consulta',
        actualizadoEn: '2026-10-22T10:00:00', cantidadMensajes: 4 }
    ];
    component.eliminarConversacionAdmin(2);
    httpMock.expectNone(`${environment.apiUrl}/chat/admin/conversaciones/2`);
    expect(component.conversacionesAdmin.length).toBe(1);
  });

  it('explica las consultas disponibles al cliente según su rol', () => {
    autorizacion.guardarSesion({ token: 'cliente', roles: ['ROLE_CLIENTE'] });
    expect(component.descripcionCapacidades).toContain('tus mascotas');
    expect(component.descripcionCapacidades).toContain('tus citas');
  });

  it('muestra el mensaje del backend cuando el asistente no esta configurado', () => {
    component.texto = 'hola';
    component.enviar();
    httpMock.expectOne(`${environment.apiUrl}/chat`)
      .flush({ ok: false, mensaje: 'El asistente no está configurado en el servidor.', data: null });

    expect(component.mensajes[1].error).toBe(true);
    expect(component.mensajes[1].contenido).toBe('El asistente no está configurado en el servidor.');
    expect(component.historial).toEqual([]);
    expect(component.enviando).toBe(false);
  });

  it('muestra el mensaje del backend cuando la peticion falla', () => {
    component.texto = 'hola';
    component.enviar();
    httpMock.expectOne(`${environment.apiUrl}/chat`)
      .flush({ mensaje: 'Fallo interno del asistente' }, { status: 500, statusText: 'Server Error' });

    expect(component.mensajes[1].error).toBe(true);
    expect(component.mensajes[1].contenido).toBe('Fallo interno del asistente');
  });

  it('usa un mensaje por defecto si el error no incluye detalle', () => {
    component.texto = 'hola';
    component.enviar();
    httpMock.expectOne(`${environment.apiUrl}/chat`)
      .flush('', { status: 0, statusText: 'Network Error' });

    expect(component.mensajes[1].error).toBe(true);
    expect(component.mensajes[1].contenido).toContain('No se pudo conectar');
  });

  it('no envia nada si el texto esta vacio o en blanco', () => {
    component.texto = '   ';
    component.enviar();
    httpMock.expectNone(`${environment.apiUrl}/chat`);
    expect(component.mensajes.length).toBe(0);
    expect(component.enviando).toBe(false);
  });

  it('presionarEnter envia el mensaje, pero con Shift+Enter solo permite el salto de linea', () => {
    const enviar = spyOn(component, 'enviar');

    const enter = { shiftKey: false, preventDefault: jasmine.createSpy('preventDefault') } as unknown as KeyboardEvent;
    component.presionarEnter(enter);
    expect(enviar).toHaveBeenCalledTimes(1);
    expect(enter.preventDefault).toHaveBeenCalled();

    const shiftEnter = { shiftKey: true, preventDefault: jasmine.createSpy('preventDefault') } as unknown as KeyboardEvent;
    component.presionarEnter(shiftEnter);
    expect(enviar).toHaveBeenCalledTimes(1);
    expect(shiftEnter.preventDefault).not.toHaveBeenCalled();
  });

  it('alternar abre y cierra la ventana del asistente', () => {
    component.alternar();
    expect(component.abierto).toBe(true);
    component.cerrar();
    expect(component.abierto).toBe(false);
  });
});
