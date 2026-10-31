import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TimeoutError } from 'rxjs';
import { ApiService } from './api.service';
import { NotificacionService } from './notificacion.service';
import { environment } from '../../environments/environment';

describe('ApiService', () => {
  let servicio: ApiService;
  let httpMock: HttpTestingController;
  const notificaciones = { mostrarError: jasmine.createSpy('mostrarError') };

  beforeEach(() => {
    notificaciones.mostrarError.calls.reset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificacionService, useValue: notificaciones }
      ]
    });
    servicio = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('login envia un POST con email y password al endpoint correcto', () => {
    let respuesta: any;
    servicio.login({ email: 'ana@vet.com', password: 'clave123' }).subscribe(r => respuesta = r);
    const peticion = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ email: 'ana@vet.com', password: 'clave123' });
    peticion.flush({ ok: true, mensaje: '', data: { token: 'tok-123' } });
    expect(respuesta.data.token).toBe('tok-123');
  });

  it('mascotas consume GET /mascotas', () => {
    servicio.mascotas().subscribe();
    const peticion = httpMock.expectOne(`${environment.apiUrl}/mascotas`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush({ ok: true, mensaje: '', data: [] });
  });

  it('obtiene un adjunto privado como blob por HttpClient', () => {
    let archivo: Blob | undefined;
    servicio.obtenerArchivo('/uploads/estudio.pdf').subscribe(blob => archivo = blob);
    const peticion = httpMock.expectOne('http://localhost:8080/uploads/estudio.pdf');
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.responseType).toBe('blob');
    peticion.flush(new Blob(['pdf'], { type: 'application/pdf' }));
    expect(archivo?.type).toBe('application/pdf');
  });

  it('rechaza un adjunto fuera del host de la API para proteger el token', () => {
    expect(() => servicio.obtenerArchivo('https://example.com/uploads/privado.pdf')).toThrowError();
  });

  it('chat envia el mensaje y el id de la conversación al endpoint /chat', () => {
    let respuesta: any;
    servicio.chat('¿Cuántas mascotas hay?', 12).subscribe(r => respuesta = r);
    const peticion = httpMock.expectOne(`${environment.apiUrl}/chat`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({
      pregunta: '¿Cuántas mascotas hay?',
      conversacionId: 12
    });
    peticion.flush({ ok: true, mensaje: '', data: { respuesta: 'Hay 3 mascotas registradas.', consultoBaseDatos: true, asistenteConfigurado: true } });
    expect(respuesta.data.respuesta).toBe('Hay 3 mascotas registradas.');
    expect(respuesta.data.consultoBaseDatos).toBe(true);
  });

  it('chat crea o continúa la conversación por defecto', () => {
    servicio.chat('hola').subscribe();
    const peticion = httpMock.expectOne(`${environment.apiUrl}/chat`);
    expect(peticion.request.body).toEqual({ pregunta: 'hola', conversacionId: null });
    peticion.flush({ ok: true, mensaje: '', data: { respuesta: 'ok', consultoBaseDatos: false, asistenteConfigurado: true } });
  });

  it('chat falla con TimeoutError si el backend no responde en 60 segundos', fakeAsync(() => {
    let error: unknown;
    servicio.chat('hola').subscribe({ error: e => error = e });
    httpMock.expectOne(`${environment.apiUrl}/chat`);
    tick(60_001);
    expect(error instanceof TimeoutError).toBe(true);
    // la peticion queda cancelada por el timeout; no debe poder flush-earse
  }));

  it('descargar notifica un error si la descarga falla', () => {
    servicio.descargar('/archivos/informe.pdf', 'informe.pdf');
    const peticion = httpMock.expectOne(`${environment.apiUrl}/archivos/informe.pdf`);
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.responseType).toBe('blob');
    peticion.flush(new Blob(['error'], { type: 'application/json' }), { status: 403, statusText: 'Forbidden' });
    expect(notificaciones.mostrarError).toHaveBeenCalled();
    const mensaje = notificaciones.mostrarError.calls.mostRecent().args[0] as string;
    expect(mensaje).toContain('No se pudo descargar');
  });

  it('descargar descarga el archivo sin notificar error cuando el servidor responde bien', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob:fake-url');
    const revocar = spyOn(URL, 'revokeObjectURL');
    servicio.descargar('/archivos/informe.pdf', 'informe.pdf');
    const peticion = httpMock.expectOne(`${environment.apiUrl}/archivos/informe.pdf`);
    peticion.flush(new Blob(['contenido'], { type: 'application/pdf' }));
    expect(notificaciones.mostrarError).not.toHaveBeenCalled();
    expect(revocar).toHaveBeenCalledWith('blob:fake-url');
  });
});
