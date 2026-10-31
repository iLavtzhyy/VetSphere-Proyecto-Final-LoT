import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { Dashboard, Factura } from '../../core/modelos';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { IconoComponent } from '../../shared/components/icono/icono';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { ConfiguracionGrafica, GraficaComponent } from '../../shared/components/grafica/grafica';
import { NotificacionService } from '../../core/notificacion.service';
import { AutorizacionService } from '../../core/autorizacion.service';

const NOMBRES_MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto',
  'septiembre', 'octubre', 'noviembre', 'diciembre'];
const DIAS_SEMANA = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb'];

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, IconoComponent, CargadorComponent, GraficaComponent],
  animations: [fadeInUp],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit {
  resumen?: Dashboard;
  cargando = false;
  tarjetas: { titulo: string; valor: number; detalle: string }[] = [];
  servicios: { nombre: string; total: number; icono: string }[] = [];

  // Gráficas del panel (Chart.js dentro de <app-grafica>)
  graficaCitas: ConfiguracionGrafica | null = null;
  graficaEspecies: ConfiguracionGrafica | null = null;
  graficaFacturacion: ConfiguracionGrafica | null = null;
  graficaStock: ConfiguracionGrafica | null = null;
  resumenCitas = '';
  resumenEspecies = '';
  resumenFacturacion = '';
  resumenStock = '';
  mensajeFacturacion = 'Aún no hay facturas emitidas este mes.';
  cargandoFacturas = false;
  errorFacturas: string | null = null;

  constructor(private api: ApiService, private notificaciones: NotificacionService, private autorizacion: AutorizacionService) {}

  ngOnInit() {
    this.cargando = true;
    this.api.dashboard().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => {
        this.resumen = r.data;
        this.tarjetas = [
        { titulo: 'Clientes', valor: r.data.totalClientes, detalle: 'Propietarios activos' },
        { titulo: 'Mascotas', valor: r.data.totalMascotas, detalle: 'Pacientes registrados' },
        { titulo: 'Citas hoy', valor: r.data.citasHoy, detalle: 'Planificadas hoy' },
        { titulo: 'Citas mes', valor: r.data.citasMes, detalle: 'Este mes' },
        { titulo: 'Internadas', valor: r.data.mascotasInternadas, detalle: 'Monitoreo activo' },
        { titulo: 'Cirugias hoy', valor: r.data.cirugiasHoy, detalle: 'En quirófano' }
      ];
      this.servicios = [
        { nombre: 'Consultas', total: r.data.totalConsultas, icono: 'stethoscope' },
        { nombre: 'Cirugias', total: r.data.totalCirugias, icono: 'surgery' },
        { nombre: 'Vacunas', total: r.data.totalVacunas, icono: 'syringe' },
        { nombre: 'Laboratorio', total: r.data.totalLaboratorio, icono: 'lab' },
        { nombre: 'Farmacia', total: r.data.totalProductos, icono: 'pill' },
        { nombre: 'Emergencias', total: r.data.totalEmergencias, icono: 'shield' }
      ];
      this.armarGraficas(r.data);
      },
      error: (e: HttpErrorResponse) => {
        this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo cargar el resumen del dashboard. Revisa que el backend esté activo.');
      }
    });
    if (this.autorizacion.tieneRol(['ROLE_ADMIN', 'ROLE_RECEPCIONISTA'])) {
      this.cargarFacturasDelMes();
    }
  }

  /** Construye las graficas que dependen unicamente del resumen del dashboard. */
  private armarGraficas(resumen: Dashboard) {
    this.armarGraficaCitas(resumen);
    this.armarGraficaEspecies(resumen);
    this.armarGraficaStock(resumen);
  }

  /** Citas de los últimos 7 días (GET /api/dashboard -> citasUltimos7Dias). */
  private armarGraficaCitas(resumen: Dashboard) {
    const serie = Array.isArray(resumen.citasUltimos7Dias) ? resumen.citasUltimos7Dias : [];
    const datos = serie.length ? [...serie] : [0, 0, 0, 0, 0, 0, 0];
    const etiquetas = this.etiquetasUltimosDias(datos.length);
    const total = datos.reduce((suma, valor) => suma + valor, 0);
    const maximo = Math.max(...datos, 0);

    this.resumenCitas = total === 0
      ? 'No hay citas registradas en los últimos 7 días.'
      : `Total ${total} citas en los últimos 7 días; máximo de ${maximo} el ${etiquetas[datos.indexOf(maximo)]}.`;

    this.graficaCitas = {
      type: 'bar',
      data: {
        labels: etiquetas,
        datasets: [{
          label: 'Citas',
          data: datos,
          backgroundColor: '#0891b2',
          hoverBackgroundColor: '#22d3ee',
          borderRadius: 6,
          maxBarThickness: 44
        }]
      },
      options: {
        plugins: { legend: { display: false } },
        scales: {
          y: { beginAtZero: true, ticks: { precision: 0 } },
          x: {}
        }
      }
    };
  }

  /** Pacientes por especie (GET /api/dashboard -> totalPerros/totalGatos/totalMascotas). */
  private armarGraficaEspecies(resumen: Dashboard) {
    const otras = Math.max(resumen.totalMascotas - resumen.totalPerros - resumen.totalGatos, 0);
    const especies = [
      { nombre: 'Perros', total: resumen.totalPerros, color: '#0891b2' },
      { nombre: 'Gatos', total: resumen.totalGatos, color: '#7c3aed' },
      { nombre: 'Otras especies', total: otras, color: '#f59e0b' }
    ].filter(especie => especie.total > 0);

    const porcentaje = (valor: number) =>
      resumen.totalMascotas > 0 ? Math.round((valor / resumen.totalMascotas) * 100) : 0;

    this.resumenEspecies = especies.length
      ? especies.map(e => `${e.nombre}: ${e.total} (${porcentaje(e.total)}%)`).join(' · ')
      : 'No hay pacientes registrados.';

    this.graficaEspecies = especies.length ? {
      type: 'doughnut',
      data: {
        labels: especies.map(e => e.nombre),
        datasets: [{
          data: especies.map(e => e.total),
          backgroundColor: especies.map(e => e.color),
          hoverOffset: 8
        }]
      },
      options: {
        cutout: '62%',
        plugins: { legend: { position: 'bottom' } }
      }
    } : null;
  }

  /** Productos con stock crítico (GET /api/dashboard -> stockCritico). */
  private armarGraficaStock(resumen: Dashboard) {
    const productos = (Array.isArray(resumen.stockCritico) ? resumen.stockCritico : []).slice(0, 8);
    if (!productos.length) {
      this.graficaStock = null;
      this.resumenStock = '';
      return;
    }

    this.resumenStock = `${productos.length} producto${productos.length === 1 ? '' : 's'} de farmacia con stock por debajo del mínimo.`;
    this.graficaStock = {
      type: 'bar',
      data: {
        labels: productos.map(producto => producto.nombre),
        datasets: [
          { label: 'Stock actual', data: productos.map(producto => producto.stock), backgroundColor: '#e11d48', borderRadius: 4 },
          { label: 'Stock mínimo', data: productos.map(producto => producto.stockCritico), backgroundColor: '#fda4af', borderRadius: 4 }
        ]
      },
      options: {
        indexAxis: 'y',
        plugins: { legend: { position: 'bottom' } },
        scales: {
          x: { beginAtZero: true, ticks: { precision: 0 } },
          y: {}
        }
      }
    };
  }

  /** Facturación del mes en curso (GET /api/facturacion, agregada por día en el cliente). */
  private cargarFacturasDelMes() {
    this.cargandoFacturas = true;
    this.api.facturas().pipe(finalize(() => this.cargandoFacturas = false)).subscribe({
      next: r => this.armarGraficaFacturacion(Array.isArray(r.data) ? r.data : []),
      error: (e: HttpErrorResponse) => {
        this.errorFacturas = 'No se pudo cargar la facturación del mes.';
        this.graficaFacturacion = null;
        this.resumenFacturacion = '';
        this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudo cargar la facturación para la gráfica del mes.');
      }
    });
  }

  private armarGraficaFacturacion(facturas: Factura[]) {
    const ahora = new Date();
    const anio = ahora.getFullYear();
    const mes = ahora.getMonth();
    const diasDelMes = new Date(anio, mes + 1, 0).getDate();
    const totalesPorDia = new Array<number>(diasDelMes).fill(0);
    const facturasPorDia = new Array<number>(diasDelMes).fill(0);

    for (const factura of facturas) {
      const fecha = new Date(factura.fecha);
      if (Number.isNaN(fecha.getTime()) || fecha.getFullYear() !== anio || fecha.getMonth() !== mes) {
        continue;
      }
      totalesPorDia[fecha.getDate() - 1] += Number(factura.total) || 0;
      facturasPorDia[fecha.getDate() - 1]++;
    }

    const totalMes = totalesPorDia.reduce((suma, valor) => suma + valor, 0);
    const cantidad = facturasPorDia.reduce((suma, valor) => suma + valor, 0);
    if (!cantidad) {
      this.graficaFacturacion = null;
      this.resumenFacturacion = '';
      return;
    }

    this.resumenFacturacion =
      `Total emitido en ${NOMBRES_MESES[mes]}: ${this.pesos(totalMes)} en ${cantidad} factura${cantidad === 1 ? '' : 's'}.`;
    this.graficaFacturacion = {
      type: 'line',
      data: {
        labels: Array.from({ length: diasDelMes }, (_, indice) => String(indice + 1).padStart(2, '0')),
        datasets: [{
          label: 'Facturado por día',
          data: totalesPorDia,
          borderColor: '#10b981',
          backgroundColor: 'rgba(16, 185, 129, .16)',
          fill: true,
          tension: .35,
          pointRadius: 0,
          pointHoverRadius: 5
        }]
      },
      options: {
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (contexto: { parsed: { y: number | null } }) => ` ${this.pesos(contexto.parsed.y ?? 0)}`
            }
          }
        },
        scales: {
          y: { beginAtZero: true, ticks: { callback: (valor: number | string) => this.pesos(Number(valor)) } },
          x: {}
        }
      }
    };
  }

  /** Etiquetas de los últimos N días (hace N-1 ... hoy) en español, sin depender del locale. */
  private etiquetasUltimosDias(cantidad: number): string[] {
    const etiquetas: string[] = [];
    for (let atras = cantidad - 1; atras >= 0; atras--) {
      const fecha = new Date();
      fecha.setDate(fecha.getDate() - atras);
      etiquetas.push(`${DIAS_SEMANA[fecha.getDay()]} ${String(fecha.getDate()).padStart(2, '0')}`);
    }
    return etiquetas;
  }

  private pesos(valor: number): string {
    return new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD', maximumFractionDigits: 2 }).format(valor);
  }
}
