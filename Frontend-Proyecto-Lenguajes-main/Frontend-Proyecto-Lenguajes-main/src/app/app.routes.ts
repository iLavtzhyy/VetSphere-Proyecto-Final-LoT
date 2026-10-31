import { Routes } from '@angular/router';
import { LayoutComponent } from './components/barra-navegacion-admin/barra-navegacion-admin';
import { LoginComponent } from './components/login/login';
import { RegistroComponent } from './components/registro/registro';
import { VerificacionComponent } from './components/verificar-codigo/verificar-codigo';
import { DashboardComponent } from './components/dashboard/dashboard';
import { MascotasComponent } from './components/mascotas/mascotas';
import { VeterinariosComponent } from './components/veterinarios/veterinarios';
import { AdministracionComponent } from './components/administracion/administracion';
import { RecepcionComponent } from './components/agendar-cita/agendar-cita';
import { FarmaciaComponent } from './components/farmacia/farmacia';
import { ReportesComponent } from './components/reportes/reportes';
import { authGuard } from './core/auth.guard';
import { HomeComponent } from './components/inicio/inicio';
import { ClientesComponent } from './components/clientes/clientes';
import { ConsultasComponent } from './components/consultas/consultas';
import { FacturacionComponent } from './components/facturacion/facturacion';
import { CirugiasComponent } from './components/cirugias/cirugias';
import { LaboratorioComponent } from './components/laboratorio/laboratorio';
import { HospitalizacionComponent } from './components/hospitalizacion/hospitalizacion';
import { UsuariosComponent } from './components/usuarios/usuarios';
import { DocumentacionComponent } from './components/documentacion/documentacion';
import { MiCuentaComponent } from './components/mi-cuenta/mi-cuenta';
import { ReactivarCuentaComponent } from './components/reactivar-cuenta/reactivar-cuenta';
import { CadenaFrioComponent } from './components/cadena-frio/cadena-frio';

export const routes: Routes = [
  { path: '', pathMatch: 'full', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'registro', component: RegistroComponent },
  { path: 'verificacion', component: VerificacionComponent },
  // Ruta publica: una cuenta desactivada no puede iniciar sesion, por lo que
  // necesita poder reactivar su cuenta sin token.
  { path: 'reactivar-cuenta', component: ReactivarCuentaComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO', 'ROLE_RECEPCIONISTA'] } },
      { path: 'clientes', component: ClientesComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'] } },
      { path: 'consultas', component: ConsultasComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO'] } },
      { path: 'cirugias', component: CirugiasComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO'] } },
      { path: 'laboratorio', component: LaboratorioComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO'] } },
      { path: 'hospitalizacion', component: HospitalizacionComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO'] } },
      { path: 'mascotas', component: MascotasComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO', 'ROLE_RECEPCIONISTA', 'ROLE_CLIENTE'] } },
      { path: 'veterinarios', component: VeterinariosComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_VETERINARIO', 'ROLE_RECEPCIONISTA', 'ROLE_CLIENTE'] } },
      { path: 'administracion', component: AdministracionComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN'] } },
      { path: 'recepcion', component: RecepcionComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'] } },
      { path: 'farmacia', component: FarmaciaComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'] } },
      { path: 'cadena-frio', component: CadenaFrioComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN'] } },
      { path: 'facturacion', component: FacturacionComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'] } },
      { path: 'usuarios', component: UsuariosComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN'] } },
      { path: 'calidad-software', component: DocumentacionComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN'] } },
      { path: 'reportes', component: ReportesComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN'] } },
      { path: 'mi-cuenta', component: MiCuentaComponent, canActivate: [authGuard] },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
