import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { tokenInterceptor } from './token.interceptor';
import { SesionInactividadService } from './sesion-inactividad.service';
import { environment } from '../../environments/environment';

describe('tokenInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  const sesion = { cerrarSesion: jasmine.createSpy('cerrarSesion') };

  beforeEach(() => {
    sesion.cerrarSesion.calls.reset();
    localStorage.removeItem('vetsphere_token');
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([tokenInterceptor])),
        provideHttpClientTesting(),
        { provide: SesionInactividadService, useValue: sesion }
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    localStorage.removeItem('vetsphere_token');
    httpMock.verify();
  });

  it('agrega el encabezado Authorization cuando hay token en localStorage', () => {
    localStorage.setItem('vetsphere_token', 'abc123');
    http.get('/api/datos').subscribe();
    const peticion = httpMock.expectOne('/api/datos');
    expect(peticion.request.headers.get('Authorization')).toBe('Bearer abc123');
    peticion.flush({ ok: true });
  });

  it('no agrega Authorization cuando no hay token', () => {
    http.get('/api/datos').subscribe();
    const peticion = httpMock.expectOne('/api/datos');
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush({ ok: true });
  });

  it('cierra la sesion ante un 401 de la API principal', () => {
    localStorage.setItem('vetsphere_token', 'expirado');
    let errorRecibido: HttpErrorResponse | undefined;
    const url = `${environment.apiUrl}/datos`;
    http.get(url).subscribe({ error: (e: HttpErrorResponse) => errorRecibido = e });
    const peticion = httpMock.expectOne(url);
    peticion.flush('', { status: 401, statusText: 'Unauthorized' });
    expect(sesion.cerrarSesion).toHaveBeenCalledTimes(1);
    expect(errorRecibido?.status).toBe(401);
  });

  it('conserva la sesion ante un 401 de un servicio externo como IoT', () => {
    localStorage.setItem('vetsphere_token', 'token-valido-para-la-app');
    let errorRecibido: HttpErrorResponse | undefined;
    const url = `${environment.iotApiUrl}/admin/dashboard`;
    http.get(url).subscribe({ error: (e: HttpErrorResponse) => errorRecibido = e });
    const peticion = httpMock.expectOne(url);
    peticion.flush('', { status: 401, statusText: 'Unauthorized' });
    expect(sesion.cerrarSesion).not.toHaveBeenCalled();
    expect(errorRecibido?.status).toBe(401);
  });

  it('no cierra la sesion ante errores distintos de 401', () => {
    let errorRecibido: HttpErrorResponse | undefined;
    http.get('/api/datos').subscribe({ error: (e: HttpErrorResponse) => errorRecibido = e });
    const peticion = httpMock.expectOne('/api/datos');
    peticion.flush('', { status: 500, statusText: 'Server Error' });
    expect(sesion.cerrarSesion).not.toHaveBeenCalled();
    expect(errorRecibido?.status).toBe(500);
  });
});
