import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ReactivarCuentaComponent } from './reactivar-cuenta';
import { NotificacionService } from '../../core/notificacion.service';
import { environment } from '../../../environments/environment';

describe('ReactivarCuentaComponent', () => {
  let component: ReactivarCuentaComponent;
  let fixture: ComponentFixture<ReactivarCuentaComponent>;
  let httpMock: HttpTestingController;
  let notificaciones: NotificacionService;

  const URL_SOLICITAR = `${environment.apiUrl}/auth/reactivar-cuenta`;
  const URL_CONFIRMAR = `${environment.apiUrl}/auth/confirmar-reactivacion`;

  function textos(): string {
    return fixture.nativeElement.textContent || '';
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactivarCuentaComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    notificaciones = TestBed.inject(NotificacionService);

    fixture = TestBed.createComponent(ReactivarCuentaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('arranca en el paso 1 pidiendo el correo', () => {
    expect(component.paso).toBe(1);
    expect(component.email).toBe('');
    expect(component.codigo).toBe('');
    expect(textos()).toContain('Enviar codigo');
    expect(textos()).toContain('Ingresa el correo con el que te registraste. Te enviaremos un codigo de 6 digitos.');
    expect(fixture.nativeElement.querySelector('#reactivar-email')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('#reactivar-codigo')).toBeNull();
  });

  it('solicita el codigo de reactivacion con el correo normalizado', () => {
    component.email = '  Ana@Vet.COM ';

    component.solicitarCodigo();

    const req = httpMock.expectOne({ method: 'POST', url: URL_SOLICITAR });
    expect(req.request.body).toEqual({ email: 'ana@vet.com' });
    req.flush({ ok: true, mensaje: '', data: null });
    fixture.detectChanges();

    expect(component.paso).toBe(2);
    expect(component.procesando).toBe(false);
    expect(component.error).toBe('');
    expect(component.mensaje).toBe('Si el correo esta registrado, te enviamos un codigo de reactivacion.');
    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Solicitud de reactivacion enviada.');
  });

  it('no envia la peticion de codigo si el correo esta vacio', () => {
    component.email = '   ';

    component.solicitarCodigo();

    httpMock.expectNone(URL_SOLICITAR);
    expect(component.error).toBe('Ingresa el correo con el que te registraste.');
    expect(component.paso).toBe(1);
    expect(component.procesando).toBe(false);
  });

  it('muestra el mensaje del backend cuando no se puede enviar el codigo', () => {
    component.email = 'ana@vet.com';

    component.solicitarCodigo();
    httpMock.expectOne({ method: 'POST', url: URL_SOLICITAR })
      .flush({ mensaje: 'La cuenta no esta desactivada' }, { status: 400, statusText: 'Bad Request' });

    expect(component.error).toBe('La cuenta no esta desactivada');
    expect(component.paso).toBe(1);
    expect(component.procesando).toBe(false);
  });

  it('muestra el mensaje generico cuando el backend no devuelve detalle del error', () => {
    component.email = 'ana@vet.com';

    component.solicitarCodigo();
    httpMock.expectOne({ method: 'POST', url: URL_SOLICITAR })
      .flush(null, { status: 500, statusText: 'Error' });

    expect(component.error).toBe('No se pudo enviar el codigo de reactivacion.');
    expect(component.paso).toBe(1);
  });

  it('confirma la reactivacion con el codigo de 6 digitos', () => {
    component.email = 'ana@vet.com';
    component.codigo = '123456';

    component.confirmar();

    const req = httpMock.expectOne({ method: 'POST', url: URL_CONFIRMAR });
    expect(req.request.body).toEqual({ email: 'ana@vet.com', codigo: '123456' });
    req.flush({ ok: true, mensaje: '', data: null });
    fixture.detectChanges();

    expect(component.paso).toBe(3);
    expect(component.procesando).toBe(false);
    expect(component.error).toBe('');
    expect(component.mensaje).toBe('Tu cuenta quedo reactivada. Ya puedes iniciar sesion con normalidad.');
    expect(notificaciones.notificaciones().map(n => n.texto))
      .toContain('Tu cuenta quedo reactivada.');
    expect(textos()).toContain('Tu cuenta quedo reactivada. Ya puedes iniciar sesion con normalidad.');
  });

  it('no confirma la reactivacion si el codigo no tiene 6 digitos', () => {
    component.email = 'ana@vet.com';
    component.codigo = '12345';

    component.confirmar();

    httpMock.expectNone(URL_CONFIRMAR);
    expect(component.error).toBe('El codigo de verificacion tiene 6 digitos.');
    expect(component.paso).toBe(1);
    expect(component.procesando).toBe(false);
  });

  it('no confirma la reactivacion si el correo esta vacio', () => {
    component.email = '';
    component.codigo = '123456';

    component.confirmar();

    httpMock.expectNone(URL_CONFIRMAR);
    expect(component.error).toBe('Ingresa el correo con el que te registraste.');
  });

  it('un codigo incorrecto no reactiva la cuenta', () => {
    component.email = 'ana@vet.com';
    component.codigo = '000000';

    component.confirmar();

    const req = httpMock.expectOne({ method: 'POST', url: URL_CONFIRMAR });
    expect(req.request.body).toEqual({ email: 'ana@vet.com', codigo: '000000' });
    req.flush({ mensaje: 'Codigo invalido o expirado.' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(component.paso).toBe(1);
    expect(component.mensaje).toBe('');
    expect(component.error).toBe('Codigo invalido o expirado.');
    expect(notificaciones.notificaciones().length).toBe(0);
  });

  it('un codigo incorrecto sin mensaje del backend muestra el aviso por defecto', () => {
    component.email = 'ana@vet.com';
    component.codigo = '000000';

    component.confirmar();
    httpMock.expectOne({ method: 'POST', url: URL_CONFIRMAR })
      .flush(null, { status: 400, statusText: 'Bad Request' });

    expect(component.paso).toBe(1);
    expect(component.error).toBe('Codigo invalido o expirado. Solicita uno nuevo.');
  });

  it('el paso 2 pide el codigo y el paso 3 confirma la cuenta reactivada', () => {
    component.email = 'ana@vet.com';
    component.solicitarCodigo();
    httpMock.expectOne({ method: 'POST', url: URL_SOLICITAR })
      .flush({ ok: true, mensaje: '', data: null });
    fixture.detectChanges();

    expect(component.paso).toBe(2);
    expect(textos()).toContain('Reactivar mi cuenta');
    expect(textos()).toContain('Ingresa el codigo de 6 digitos que enviamos a tu correo.');
    expect(fixture.nativeElement.querySelector('#reactivar-codigo')).toBeTruthy();

    component.codigo = '123456';
    component.confirmar();
    httpMock.expectOne({ method: 'POST', url: URL_CONFIRMAR })
      .flush({ ok: true, mensaje: '', data: null });
    fixture.detectChanges();

    expect(component.paso).toBe(3);
    expect(textos()).toContain('Tu cuenta quedo reactivada. Ya puedes iniciar sesion con normalidad.');
  });

  it('reiniciar vuelve al paso 1 y limpia el formulario', () => {
    component.email = 'ana@vet.com';
    component.codigo = '123456';
    component.mensaje = 'mensaje';
    component.error = 'error';

    component.reiniciar();

    expect(component.paso).toBe(1);
    expect(component.codigo).toBe('');
    expect(component.mensaje).toBe('');
    expect(component.error).toBe('');
    expect(component.email).toBe('ana@vet.com');
  });
});
