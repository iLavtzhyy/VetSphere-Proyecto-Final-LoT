import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { VerificacionComponent } from './verificar-codigo';
import { AutorizacionService } from '../../core/autorizacion.service';
import { environment } from '../../../environments/environment';

describe('VerificacionComponent', () => {
  let component: VerificacionComponent;
  let fixture: ComponentFixture<VerificacionComponent>;
  let httpMock: HttpTestingController;
  let router: Router;
  let autorizacion: AutorizacionService;
  let navegar: jasmine.Spy;

  const URL_VERIFICAR = `${environment.apiUrl}/auth/verificar-2fa`;
  const URL_REENVIAR = `${environment.apiUrl}/auth/reenviar-codigo`;
  const CLAVE_SESION = 'vetsphere_verificacion';

  function textos(): string {
    return fixture.nativeElement.textContent || '';
  }

  function crearComponente(): VerificacionComponent {
    fixture = TestBed.createComponent(VerificacionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    return component;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerificacionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    autorizacion = TestBed.inject(AutorizacionService);
    navegar = spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));

    // El componente lee el correo de la sesion en el constructor.
    sessionStorage.setItem(CLAVE_SESION, btoa(JSON.stringify({ email: 'ana@vet.com' })));
    crearComponente();
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('recupera el correo de la sesion y lo enmascara en pantalla', () => {
    expect(component.email).toBe('ana@vet.com');
    expect(component.emailEnmascarado).toBe('an***@vet.com');
    expect(textos()).toContain('Ingresa el codigo enviado a an***@vet.com.');
  });

  it('redirige al registro cuando no hay ningun correo en la sesion', () => {
    sessionStorage.removeItem(CLAVE_SESION);

    crearComponente();

    expect(component.email).toBe('');
    expect(component.emailEnmascarado).toBe('tu correo');
    expect(navegar).toHaveBeenCalledWith('/registro');
  });

  it('verifica el codigo enviando el correo y el codigo introducidos', () => {
    component.codigo = '123456';

    component.verificar();

    const req = httpMock.expectOne({ method: 'POST', url: URL_VERIFICAR });
    expect(req.request.body).toEqual({ email: 'ana@vet.com', codigo: '123456' });
    req.flush({ ok: true, mensaje: '', data: { token: 'tok-123', roles: ['ROLE_ADMIN'], nombre: 'Ana' } });

    expect(localStorage.getItem('vetsphere_token')).toBe('tok-123');
    expect(localStorage.getItem('vetsphere_roles')).toBe('["ROLE_ADMIN"]');
    expect(sessionStorage.getItem(CLAVE_SESION)).toBeNull();
    expect(navegar).toHaveBeenCalledWith('/dashboard');
  });

  it('navega a /mascotas cuando la cuenta verificada es ROLE_CLIENTE', () => {
    component.codigo = '123456';

    component.verificar();
    httpMock.expectOne({ method: 'POST', url: URL_VERIFICAR })
      .flush({ ok: true, mensaje: '', data: { token: 'tok-456', roles: ['ROLE_CLIENTE'], nombre: 'Luis' } });

    expect(autorizacion.tieneRol(['ROLE_CLIENTE'])).toBe(true);
    expect(navegar).toHaveBeenCalledWith('/mascotas');
  });

  it('muestra el mensaje de codigo invalido cuando el backend rechaza la verificacion', () => {
    component.codigo = '000000';

    component.verificar();
    httpMock.expectOne({ method: 'POST', url: URL_VERIFICAR })
      .flush({ mensaje: 'Codigo incorrecto' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(component.ok).toBe(false);
    expect(component.mensaje).toBe('Codigo invalido o expirado.');
    expect(textos()).toContain('Codigo invalido o expirado.');
    expect(navegar).not.toHaveBeenCalled();
    expect(localStorage.getItem('vetsphere_token')).toBeNull();
  });

  it('reenvia el codigo de verificacion al correo de la sesion', () => {
    component.reenviar();

    const req = httpMock.expectOne({ method: 'POST', url: URL_REENVIAR });
    expect(req.request.body).toEqual({ email: 'ana@vet.com' });
    req.flush({ ok: true, mensaje: '', data: null });
    fixture.detectChanges();

    expect(component.ok).toBe(true);
    expect(component.mensaje).toBe('Codigo reenviado. Revisa tu Gmail.');
    expect(textos()).toContain('Codigo reenviado. Revisa tu Gmail.');
  });

  it('muestra el error cuando no se puede reenviar el codigo', () => {
    component.reenviar();
    httpMock.expectOne({ method: 'POST', url: URL_REENVIAR })
      .flush(null, { status: 500, statusText: 'Error' });
    fixture.detectChanges();

    expect(component.ok).toBe(false);
    expect(component.mensaje).toBe('No se pudo reenviar el codigo.');
  });

  it('el campo de codigo admite 6 digitos como maximo', () => {
    const codigo: HTMLInputElement = fixture.nativeElement.querySelector('#verificar-codigo');
    expect(codigo.getAttribute('maxlength')).toBe('6');
    expect(codigo.maxLength).toBe(6);
  });

  it('no envia la peticion de verificacion si el codigo esta vacio', () => {
    // Angular anade `novalidate` a todos los <form> (NgNoValidate) y con ello
    // desactiva la validacion interactiva del navegador, asi que se vuelve a
    // activar aqui para comprobar que el marcado declara las restricciones y
    // que el navegador bloquea el envio cuando el codigo no tiene 6 digitos.
    const form: HTMLFormElement = fixture.nativeElement.querySelector('form');
    form.noValidate = false;

    const codigo: HTMLInputElement = form.querySelector('#verificar-codigo') as HTMLInputElement;
    codigo.value = '';
    codigo.dispatchEvent(new Event('input'));

    expect(codigo.validity.valueMissing).toBe(true);
    expect(form.checkValidity()).toBe(false);

    (form.querySelector('button[type=submit]') as HTMLButtonElement).click();

    httpMock.expectNone(URL_VERIFICAR);
    expect(component.mensaje).toBe('');
  });
});
