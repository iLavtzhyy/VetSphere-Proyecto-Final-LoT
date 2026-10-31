import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { RegistroComponent } from './registro';
import { environment } from '../../../environments/environment';

describe('RegistroComponent', () => {
  let component: RegistroComponent;
  let fixture: ComponentFixture<RegistroComponent>;
  let httpMock: HttpTestingController;
  let router: Router;
  let navegar: jasmine.Spy;

  const URL_REGISTRO = `${environment.apiUrl}/auth/registro`;

  function textos(): string {
    return fixture.nativeElement.textContent || '';
  }

  function escribir(campo: string, valor: string): void {
    const input: HTMLInputElement = fixture.nativeElement.querySelector(`#${campo}`);
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegistroComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    navegar = spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));

    fixture = TestBed.createComponent(RegistroComponent);
    component = fixture.componentInstance;
    // Las pruebas de registro exitoso deben completar el nuevo campo de confirmación.
    component.confirmarPassword = 'Clave123*';
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('no realiza ninguna peticion al abrir la pantalla de registro', () => {
    httpMock.expectNone(URL_REGISTRO);
    expect(component.solicitudEnviada).toBe(false);
    expect(component.tipoRegistro).toBe('cliente');
  });

  it('exige cédula de 10 dígitos para registrar clientes', () => {
    component.form.nombres = 'Ana';
    component.form.apellidos = 'Lopez';
    component.form.email = 'ana@vet.com';
    component.form.password = 'Clave123*';
    component.form.documento = '123';

    component.registrar();

    httpMock.expectNone(URL_REGISTRO);
    expect(component.error).toBe('Ingresa tu número de cédula de 10 dígitos.');
  });

  it('envia la peticion de registro con los datos del formulario', () => {
    component.form = {
      nombres: 'Ana', apellidos: 'Lopez', email: 'ana@vet.com', telefono: '0991111111', documento: '1712345678',
      password: 'Clave123*', rolSolicitado: 'ROLE_CLIENTE'
    };

    component.registrar();

    const req = httpMock.expectOne({ method: 'POST', url: URL_REGISTRO });
    expect(req.request.body.nombres).toBe('Ana');
    expect(req.request.body.apellidos).toBe('Lopez');
    expect(req.request.body.email).toBe('ana@vet.com');
    expect(req.request.body.telefono).toBe('0991111111');
    expect(req.request.body.documento).toBe('1712345678');
    expect(req.request.body.password).toBe('Clave123*');
    expect(req.request.body.rolSolicitado).toBe('ROLE_CLIENTE');
    req.flush({ ok: true, mensaje: '', data: { id: 1, email: 'ana@vet.com' } });
  });

  it('el registro de cliente guarda el correo para la verificacion y navega a /verificacion', () => {
    component.form = {
      nombres: 'Ana', apellidos: 'Lopez', email: 'ana@vet.com', telefono: '0991111111', documento: '1712345678',
      password: 'Clave123*', rolSolicitado: 'ROLE_CLIENTE'
    };

    component.registrar();
    httpMock.expectOne({ method: 'POST', url: URL_REGISTRO })
      .flush({ ok: true, mensaje: '', data: { id: 1, email: 'ana@vet.com' } });

    const guardado = sessionStorage.getItem('vetsphere_verificacion');
    expect(guardado).toBeTruthy();
    expect(JSON.parse(atob(guardado as string)).email).toBe('ana@vet.com');
    expect(navegar).toHaveBeenCalledWith('/verificacion');
    expect(component.solicitudEnviada).toBe(false);
  });

  it('el registro de personal envia el codigo de administrador y espera la aprobacion', () => {
    component.cambiarTipo('personal');
    component.form.nombres = 'Luis';
    component.form.apellidos = 'Perez';
    component.form.email = 'luis@vet.com';
    component.form.password = 'Clave123*';
    component.codigoAdmin = '1404';

    component.registrar();

    const req = httpMock.expectOne({ method: 'POST', url: URL_REGISTRO });
    expect(req.request.body.rolSolicitado).toBe('ROLE_VETERINARIO');
    expect(req.request.body.codigoAdmin).toBe('1404');
    req.flush({ ok: true, mensaje: '', data: { id: 2, email: 'luis@vet.com' } });
    fixture.detectChanges();

    expect(component.solicitudEnviada).toBe(true);
    expect(navegar).not.toHaveBeenCalled();
    expect(textos()).toContain('Solicitud Recibida');
    expect(textos()).toContain('En espera de que un administrador acepte la solicitud');
  });

  it('el registro de cliente no envia codigo de administrador', () => {
    component.form = {
      nombres: 'Ana', apellidos: 'Lopez', email: 'ana@vet.com', telefono: '0991111111', documento: '1712345678',
      password: 'Clave123*', rolSolicitado: 'ROLE_CLIENTE'
    };
    component.codigoAdmin = '1404';

    component.registrar();

    const req = httpMock.expectOne({ method: 'POST', url: URL_REGISTRO });
    expect(req.request.body.codigoAdmin).toBeUndefined();
    req.flush({ ok: true, mensaje: '', data: { id: 1, email: 'ana@vet.com' } });
  });

  it('cambiar el tipo de registro ajusta el rol solicitado y limpia el error', () => {
    component.error = 'Error anterior';

    component.cambiarTipo('personal');
    expect(component.tipoRegistro).toBe('personal');
    expect(component.form.rolSolicitado).toBe('ROLE_VETERINARIO');
    expect(component.error).toBe('');

    component.cambiarTipo('cliente');
    expect(component.tipoRegistro).toBe('cliente');
    expect(component.form.rolSolicitado).toBe('ROLE_CLIENTE');
    expect(component.error).toBe('');
  });

  it('muestra el mensaje de error del backend cuando el correo ya esta registrado', () => {
    component.form = {
      nombres: 'Ana', apellidos: 'Lopez', email: 'ana@vet.com', telefono: '0991111111', documento: '1712345678',
      password: 'Clave123*', rolSolicitado: 'ROLE_CLIENTE'
    };

    component.registrar();
    httpMock.expectOne({ method: 'POST', url: URL_REGISTRO })
      .flush({ mensaje: 'El correo ya esta registrado' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(component.error).toBe('El correo ya esta registrado');
    expect(textos()).toContain('El correo ya esta registrado');
  });

  it('muestra el mensaje generico cuando el backend no devuelve detalle del error', () => {
    component.form = {
      nombres: 'Ana', apellidos: 'Lopez', email: 'ana@vet.com', telefono: '0991111111', documento: '1712345678',
      password: 'Clave123*', rolSolicitado: 'ROLE_CLIENTE'
    };

    component.registrar();
    httpMock.expectOne({ method: 'POST', url: URL_REGISTRO })
      .flush(null, { status: 500, statusText: 'Error' });

    expect(component.error).toBe('No se pudo registrar. Revisa los datos, correo duplicado o el código de administrador.');
  });

  it('el formulario declara el correo como obligatorio y con formato de email', () => {
    const email: HTMLInputElement = fixture.nativeElement.querySelector('#registro-email');
    const password: HTMLInputElement = fixture.nativeElement.querySelector('#registro-password');

    expect(email.required).toBe(true);
    expect(email.type).toBe('email');
    expect(password.required).toBe(true);
  });

  it('no envia la peticion si el correo no cumple las restricciones del formulario', () => {
    // Angular anade `novalidate` a todos los <form> (NgNoValidate) y con ello
    // desactiva la validacion interactiva del navegador, asi que se vuelve a
    // activar aqui para comprobar que el marcado declara las restricciones y
    // que el navegador bloquea el envio cuando el correo no es valido.
    const form: HTMLFormElement = fixture.nativeElement.querySelector('form');
    form.noValidate = false;

    escribir('registro-nombres', 'Ana');
    escribir('registro-apellidos', 'Lopez');
    escribir('registro-telefono', '0991111111');
    escribir('registro-password', 'Clave123*');
    escribir('registro-email', 'correo-sin-arroba');

    const email: HTMLInputElement = fixture.nativeElement.querySelector('#registro-email');
    expect(email.validity.typeMismatch).toBe(true);
    expect(email.checkValidity()).toBe(false);
    expect(form.checkValidity()).toBe(false);

    (form.querySelector('button[type=submit]') as HTMLButtonElement).click();

    httpMock.expectNone(URL_REGISTRO);
    expect(component.error).toBe('');
  });

  it('el formulario no declara una longitud minima para la contrasena', () => {
    const password: HTMLInputElement = fixture.nativeElement.querySelector('#registro-password');
    expect(password.getAttribute('minlength')).toBeNull();
    expect(password.minLength).toBe(-1);
  });
});
