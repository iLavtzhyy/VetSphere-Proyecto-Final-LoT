import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { DashboardComponent } from './dashboard';
import { Dashboard, Factura, Producto } from '../../core/modelos';
import { NotificacionService } from '../../core/notificacion.service';
import { environment } from '../../../environments/environment';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let httpMock: HttpTestingController;
  const notificaciones = { mostrarError: jasmine.createSpy('mostrarError') };

  const productoCritico: Producto = {
    codigo: 'MED-001', nombre: 'Amoxicilina 250mg', categoria: 'Antibiótico',
    stock: 2, stockCritico: 10, precio: 8.5, prioridadAlta: true
  };

  const resumenDePrueba: Dashboard = {
    totalClientes: 10,
    totalMascotas: 8,
    mascotasInternadas: 1,
    citasHoy: 2,
    citasMes: 12,
    cirugiasHoy: 0,
    totalConsultas: 5,
    totalCirugias: 3,
    totalVacunas: 4,
    totalLaboratorio: 1,
    totalProductos: 20,
    totalEmergencias: 2,
    citasUltimos7Dias: [1, 2, 3, 0, 4, 5, 6],
    totalPerros: 5,
    totalGatos: 2,
    cirugiasRecientes: [],
    stockCritico: [productoCritico]
  };

  beforeEach(async () => {
    localStorage.setItem('vetsphere_roles', JSON.stringify(['ROLE_ADMIN']));
    notificaciones.mostrarError.calls.reset();
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations(),
        { provide: NotificacionService, useValue: notificaciones }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => { httpMock.verify(); localStorage.clear(); });

  const flushDashboard = (data: Dashboard = resumenDePrueba) =>
    httpMock.expectOne(`${environment.apiUrl}/dashboard`)
      .flush({ ok: true, mensaje: 'Resumen clinico', data });

  const flushFacturas = (data: Factura[] = []) =>
    httpMock.expectOne(`${environment.apiUrl}/facturacion`)
      .flush({ ok: true, mensaje: 'Facturas', data });

  const facturaDePrueba = (dia: number, total: number): Factura => {
    const ahora = new Date();
    return {
      id: 1,
      numero: 'FAC-2026-0001',
      cliente: 'Ana Pérez',
      mascota: 'Max',
      concepto: 'Consulta veterinaria',
      total,
      estadoPago: 'Pagada',
      fecha: `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}-${String(dia).padStart(2, '0')}T10:30:00`
    };
  };

  it('debe crearse correctamente', () => {
    flushDashboard();
    flushFacturas();
    expect(component).toBeTruthy();
  });

  it('carga el resumen y arma las graficas de citas, especies y stock critico', () => {
    flushDashboard();
    flushFacturas();
    fixture.detectChanges();

    expect(component.tarjetas.length).toBe(6);
    expect(component.servicios.length).toBe(6);

    expect(component.graficaCitas?.type).toBe('bar');
    expect(component.graficaCitas?.data.labels?.length).toBe(7);
    expect(component.graficaCitas?.data.datasets[0].data).toEqual(resumenDePrueba.citasUltimos7Dias);
    expect(component.resumenCitas).toContain('citas');

    expect(component.graficaEspecies?.type).toBe('doughnut');
    expect(component.graficaEspecies?.data.datasets[0].data).toEqual([5, 2, 1]);
    expect(component.resumenEspecies).toContain('Perros');

    expect(component.graficaStock?.type).toBe('bar');
    expect(component.graficaStock?.options?.indexAxis).toBe('y');
    expect(component.graficaStock?.data.labels).toEqual(['Amoxicilina 250mg']);
    expect(component.resumenStock).toContain('stock');
  });

  it('arma la grafica de facturacion del mes con las facturas recibidas', () => {
    flushDashboard();
    flushFacturas([
      facturaDePrueba(15, 120.5),
      facturaDePrueba(15, 79.5),
      { ...facturaDePrueba(2, 50), fecha: '2025-02-02T09:00:00' }
    ]);
    fixture.detectChanges();

    expect(component.errorFacturas).toBeNull();
    expect(component.graficaFacturacion?.type).toBe('line');
    expect(component.graficaFacturacion?.data.datasets[0].data[14]).toBe(200);
    // La factura de otro año queda fuera del agregado mensual.
    expect(component.graficaFacturacion?.data.datasets[0].data[1]).toBe(0);
    expect(component.resumenFacturacion).toContain('facturas');
  });

  it('muestra el mensaje de vacio cuando no hay facturas del mes', () => {
    flushDashboard();
    flushFacturas([]);
    fixture.detectChanges();

    expect(component.graficaFacturacion).toBeNull();
    expect(component.cargandoFacturas).toBe(false);
    expect(component.errorFacturas).toBeNull();
  });

  it('notifica el error del dashboard y deja las graficas vacias', () => {
    httpMock.expectOne(`${environment.apiUrl}/dashboard`)
      .flush({ mensaje: 'No autorizado' }, { status: 401, statusText: 'Unauthorized' });
    flushFacturas();
    fixture.detectChanges();

    expect(component.cargando).toBe(false);
    expect(component.graficaCitas).toBeNull();
    expect(notificaciones.mostrarError).toHaveBeenCalled();
  });

  it('notifica el error de facturacion sin ocultar el resto del panel', () => {
    flushDashboard();
    httpMock.expectOne(`${environment.apiUrl}/facturacion`)
      .flush({ mensaje: 'denegado' }, { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();

    expect(component.errorFacturas).toBe('No se pudo cargar la facturación del mes.');
    expect(component.graficaFacturacion).toBeNull();
    expect(component.cargandoFacturas).toBe(false);
    expect(component.graficaCitas).not.toBeNull();
    expect(notificaciones.mostrarError).toHaveBeenCalled();
  });
});
