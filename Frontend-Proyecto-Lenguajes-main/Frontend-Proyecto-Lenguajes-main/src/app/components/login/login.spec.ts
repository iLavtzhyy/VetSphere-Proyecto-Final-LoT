import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, HttpErrorResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login';
import { ApiService } from '../../core/api.service';
import { AutorizacionService } from '../../core/autorizacion.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let api: ApiService;
  let router: Router;
  let autorizacion: AutorizacionService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    api = TestBed.inject(ApiService);
    router = TestBed.inject(Router);
    autorizacion = TestBed.inject(AutorizacionService);
    fixture.detectChanges();
  });

  afterEach(() => localStorage.clear());

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('entrar normaliza el email, guarda la sesion y navega al dashboard', () => {
    component.email = '  Ana@Vet.COM ';
    component.password = 'clave123';
    const login = spyOn(api, 'login').and.returnValue(
      of({ ok: true, mensaje: '', data: { token: 'tok-123', roles: ['ROLE_ADMIN'], nombre: 'Ana' } } as any)
    );
    const guardar = spyOn(autorizacion, 'guardarSesion').and.callThrough();
    const navegar = spyOn(router, 'navigateByUrl');

    component.entrar();

    expect(login).toHaveBeenCalledWith({ email: 'ana@vet.com', password: 'clave123' });
    expect(guardar).toHaveBeenCalledWith({ token: 'tok-123', roles: ['ROLE_ADMIN'], nombre: 'Ana' });
    expect(navegar).toHaveBeenCalledWith('/dashboard');
    expect(component.error).toBe('');
    expect(component.enviandoLogin).toBe(false);
  });

  it('entrar navega a /mascotas cuando el usuario es ROLE_CLIENTE', () => {
    component.email = 'luis@vet.com';
    component.password = 'clave123';
    spyOn(api, 'login').and.returnValue(
      of({ ok: true, mensaje: '', data: { token: 'tok', roles: ['ROLE_CLIENTE'], nombre: 'Luis' } } as any)
    );
    const navegar = spyOn(router, 'navigateByUrl');

    component.entrar();

    expect(navegar).toHaveBeenCalledWith('/mascotas');
  });

  it('entrar muestra un mensaje breve cuando las credenciales son invalidas', () => {
    component.email = 'ana@vet.com';
    component.password = 'mala';
    spyOn(api, 'login').and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 401, error: { mensaje: 'Credenciales invalidas' } }))
    );

    component.entrar();

    expect(component.error).toBe('Correo o contraseña incorrectos.');
    expect(component.enviandoLogin).toBe(false);
  });

  it('entrar no muestra detalles tecnicos cuando no puede validar el acceso', () => {
    component.email = 'ana@vet.com';
    component.password = 'clave123';
    spyOn(api, 'login').and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    component.entrar();

    expect(component.error).toBe('Correo o contraseña incorrectos.');
    expect(component.enviandoLogin).toBe(false);
  });

  it('no realiza la peticion si ya hay un envio en curso', () => {
    component.email = 'ana@vet.com';
    component.password = 'clave123';
    component.enviandoLogin = true;
    const login = spyOn(api, 'login');

    component.entrar();

    expect(login).not.toHaveBeenCalled();
  });

  it('abrir y cerrar recuperacion gestiona el formulario auxiliar', () => {
    component.email = 'ana@vet.com';
    component.abrirRecuperacion();
    expect(component.recuperacionAbierta).toBe(true);
    expect(component.pasoRecuperacion).toBe(1);
    expect(component.recuperacionEmail).toBe('ana@vet.com');

    component.cerrarRecuperacion();
    expect(component.recuperacionAbierta).toBe(false);
    expect(component.mensajeRecuperacion).toBe('');
  });

  it('enviarRecuperacion muestra confirmacion o error segun la respuesta', () => {
    component.recuperacionEmail = 'ana@vet.com';
    const solicitar = spyOn(api, 'solicitarRecuperacion').and.returnValue(of({ ok: true, mensaje: '', data: null } as any));
    component.enviarRecuperacion();
    expect(solicitar).toHaveBeenCalledWith('ana@vet.com');
    expect(component.mensajeRecuperacionOk).toBe(true);
    expect(component.pasoRecuperacion).toBe(2);

    solicitar.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: { mensaje: 'Se alcanzó el límite de solicitudes por hora.' }
    })));
    component.enviarRecuperacion();
    expect(component.mensajeRecuperacionOk).toBe(false);
    expect(component.mensajeRecuperacion).toBe('Se alcanzó el límite de solicitudes por hora.');
    expect(component.enviandoRecuperacion).toBe(false);
  });

  it('no solicita recuperación si el correo no tiene formato válido', () => {
    const solicitar = spyOn(api, 'solicitarRecuperacion');
    component.recuperacionEmail = 'correo-invalido';

    component.enviarRecuperacion();

    expect(solicitar).not.toHaveBeenCalled();
    expect(component.mensajeRecuperacion).toBe('Ingresa un correo válido.');
  });
});
