import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { Cirugia, Mascota } from '../../core/modelos';
import { IconoComponent } from '../../shared/components/icono/icono';
import { CargadorComponent } from '../../shared/components/cargador/cargador';
import { NotificacionService } from '../../core/notificacion.service';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, IconoComponent, FormsModule, CargadorComponent],
  templateUrl: './reportes.html',
  styleUrl: './reportes.css'
})
export class ReportesComponent implements OnInit {
  mascotas: Mascota[] = [];
  mascotaSeleccionada?: Mascota;
  cargando = false;
  busqueda = '';

  todosLabs: any[] = [];
  todasHosp: any[] = [];
  cirugiasRegistradas: Cirugia[] = [];

  constructor(private api: ApiService, private notificaciones: NotificacionService) {}

  ngOnInit() {
    this.cargar();
  }

  cargar() {
    this.cargando = true;
    this.api.mascotas().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => {
        this.mascotas = r.data;
        if (this.mascotaSeleccionada) {
          this.mascotaSeleccionada = this.mascotas.find(m => m.id === this.mascotaSeleccionada?.id);
        }
      },
      error: (e: HttpErrorResponse) => this.notificaciones.mostrarError(e.error?.mensaje || 'No se pudieron cargar los historiales. Revisa que el backend esté activo.')
    });
    this.api.laboratorio().subscribe({
      next: r => this.todosLabs = r.data,
      error: () => this.todosLabs = []
    });
    this.api.hospitalizaciones().subscribe({
      next: r => this.todasHosp = r.data,
      error: () => this.todasHosp = []
    });
    this.api.cirugias().subscribe({
      next: r => this.cirugiasRegistradas = r.data,
      error: () => this.cirugiasRegistradas = []
    });
  }

  seleccionar(m: Mascota) {
    this.mascotaSeleccionada = m;
  }

  cerrarVistaPrevia() {
    this.mascotaSeleccionada = undefined;
  }

  obtenerLabsPaciente(mascotaId: number): any[] {
    return this.todosLabs.filter(o => o.mascota?.id === mascotaId);
  }

  obtenerHospPaciente(mascotaId: number): any[] {
    return this.todasHosp.filter(hp => hp.mascota?.id === mascotaId);
  }

  get filtradas() {
    const q = this.busqueda.toLowerCase();
    return this.mascotas.filter(m => `${m.nombre} ${m.especie} ${m.propietario?.nombres} ${m.propietario?.apellidos}`.toLowerCase().includes(q));
  }

  get totalEventosClinicos() {
    return this.mascotas.reduce((total, mascota) => total + (mascota.historiales?.length ?? 0), 0);
  }

  descargarPdf(m: Mascota) {
    this.api.descargar(`/reportes/mascotas/${m.id}/historial.pdf`, `historial-${m.nombre.toLowerCase()}.pdf`);
  }

  descargarExcel() {
    this.api.descargar('/reportes/finanzas.xlsx', 'finanzas-vetsphere.xlsx');
  }

  imprimirHistorial(m: Mascota) {
    const ventana = window.open('', '_blank', 'width=920,height=720');
    if (!ventana) return;
    ventana.document.write(this.crearHistorialHtml(m, true));
    ventana.document.close();
  }

  private crearHistorialHtml(m: Mascota, imprimir = false) {
    const eventsHtml = (m.historiales || []).map(h => `
      <tr>
        <td>${new Date(h.fecha).toLocaleDateString('es-EC')}</td>
        <td>${this.escapar(h.tipoEvento || 'Consulta')}</td>
        <td>${this.escapar(h.motivo || 'Chequeo')}</td>
        <td>Dx: ${this.escapar(h.diagnostico || 'Estable')}<br>Tx: ${this.escapar(h.tratamiento || 'Ninguno')}</td>
      </tr>
    `).join('');

    const cirugiasHtml = (m.cirugias || []).map(c => `
      <tr>
        <td>${new Date(c.fechaProgramada).toLocaleDateString('es-EC')}</td>
        <td>${this.escapar(c.procedimiento)}</td>
        <td>${this.escapar(c.tipoAnestesia)}</td>
        <td>${this.escapar(c.estado)}</td>
      </tr>
    `).join('');

    const labOrders = this.obtenerLabsPaciente(m.id);
    const labHtml = labOrders.map(o => `
      <tr>
        <td>${new Date(o.fecha).toLocaleDateString('es-EC')}</td>
        <td>${this.escapar(o.tipoPrueba)}</td>
        <td>${this.escapar(o.resultado || 'Pendiente')}</td>
        <td>${this.escapar(o.estado)}</td>
      </tr>
    `).join('');

    const hospOrders = this.obtenerHospPaciente(m.id);
    const hospHtml = hospOrders.map(h => `
      <tr>
        <td>${new Date(h.ingreso).toLocaleDateString('es-EC')}</td>
        <td>${h.alta ? new Date(h.alta).toLocaleDateString('es-EC') : 'En curso'}</td>
        <td>${this.escapar(h.jaula)}</td>
        <td>${this.escapar(h.motivo)}</td>
        <td>${this.escapar(h.estado)}</td>
      </tr>
    `).join('');

    const imprimirScript = imprimir ? '<script>window.onload = () => setTimeout(() => window.print(), 350);</script>' : '';

    return `<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <title>Historial de ${m.nombre}</title>
  <style>
    * { box-sizing: border-box; }
    body { margin: 0; background: #eef5f8; color: #0f172a; font-family: Arial, Helvetica, sans-serif; }
    .page { max-width: 860px; margin: 34px auto; padding: 24px; }
    .receipt { overflow: hidden; border: 1px solid #d9e7ee; border-radius: 18px; background: #fff; box-shadow: 0 24px 70px rgba(15, 23, 42, .14); }
    .hero { display: flex; justify-content: space-between; gap: 24px; padding: 30px; color: white; background: linear-gradient(135deg, #075985, #0891b2 58%, #14b8a6); }
    .brand { display: flex; gap: 14px; align-items: center; }
    .logo { display: grid; width: 58px; height: 58px; place-items: center; border-radius: 16px; background: rgba(255,255,255,.18); font-weight: 900; letter-spacing: .03em; }
    .brand h1 { margin: 0; font-size: 26px; line-height: 1; }
    .brand p { margin: 5px 0 0; opacity: .88; font-size: 13px; }
    .hero-meta { text-align: right; }
    .hero-meta strong { display: block; font-size: 18px; margin-top: 4px; }
    .status { display: inline-flex; margin-top: 12px; padding: 8px 12px; border-radius: 999px; background: rgba(255,255,255,.2); font-size: 12px; font-weight: 900; }
    .content { padding: 30px; }
    .grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
    .box { border: 1px solid #e2edf3; border-radius: 14px; padding: 14px; background: #f8fbfd; }
    .label { color: #64748b; font-size: 10px; font-weight: 900; letter-spacing: .06em; text-transform: uppercase; }
    .value { margin-top: 5px; color: #0f172a; font-size: 14px; font-weight: 900; }
    
    .section-title { margin: 28px 0 12px; font-size: 13px; font-weight: 900; color: #0f172a; text-transform: uppercase; letter-spacing: .08em; border-bottom: 2px solid #e2edf3; padding-bottom: 6px; }
    table { width: 100%; margin-top: 8px; border-collapse: collapse; overflow: hidden; border-radius: 12px; border: 1px solid #e2e8f0; }
    th { padding: 12px; background: #0f172a; color: #e0f2fe; text-align: left; font-size: 11px; text-transform: uppercase; letter-spacing: .06em; }
    td { padding: 12px; border-bottom: 1px solid #e2e8f0; color: #334155; font-size: 13px; }
    
    .footer { display: grid; grid-template-columns: 2.2fr 1fr; gap: 18px; padding: 0 30px 30px; }
    .note { border-radius: 14px; background: #ecfeff; padding: 16px; color: #155e75; font-size: 13px; line-height: 1.55; }
    .signature { display: flex; align-items: flex-end; justify-content: center; border-bottom: 1px solid #94a3b8; color: #64748b; font-size: 12px; min-height: 74px; text-align: center; }
    @media print { body { background: white; } .page { margin: 0; max-width: none; } .receipt { box-shadow: none; } }
  </style>
</head>
<body>
  <main class="page">
    <section class="receipt">
      <header class="hero">
        <div class="brand">
          <div class="logo">VS</div>
          <div>
            <h1>VetSphere</h1>
            <p>Clínica Veterinaria Integral</p>
            <p>Ficha de Salud y Resumen de Historial Clínico</p>
          </div>
        </div>
        <div class="hero-meta">
          <p>Código Ficha</p>
          <strong>HC-${m.id}</strong>
          <span class="status">VETSPHERE CERTIFICADO</span>
        </div>
      </header>

      <div class="content">
        <div class="grid">
          <div class="box"><div class="label">Paciente</div><div class="value">${this.escapar(m.nombre)}</div></div>
          <div class="box"><div class="label">Especie / Raza</div><div class="value">${this.escapar(m.especie)} - ${this.escapar(m.raza)}</div></div>
          <div class="box"><div class="label">Peso / Color</div><div class="value">${this.escapar(m.pesoKg)} kg - ${this.escapar(m.color)}</div></div>
          <div class="box"><div class="label">Sexo</div><div class="value">${this.escapar(m.sexo)}</div></div>
          <div class="box"><div class="label">Propietario</div><div class="value">${this.escapar(m.propietario?.nombres || '')} ${this.escapar(m.propietario?.apellidos || '')}</div></div>
          <div class="box"><div class="label">Contacto</div><div class="value">${this.escapar(m.propietario?.telefono || 'N/A')}</div></div>
          <div class="box"><div class="label">Nacimiento</div><div class="value">${this.escapar(m.fechaNacimiento || 'N/A')}</div></div>
          <div class="box"><div class="label">Estado</div><div class="value">${this.escapar(m.estado)}</div></div>
        </div>

        <div class="section-title">Consultas y Eventos Médicos</div>
        <table>
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Tipo</th>
              <th>Motivo</th>
              <th>Diagnóstico y Tratamiento</th>
            </tr>
          </thead>
          <tbody>
            ${eventsHtml || '<tr><td colspan="4" style="text-align:center;">No hay consultas registradas.</td></tr>'}
          </tbody>
        </table>

        <div class="section-title">Cirugías y Procedimientos</div>
        <table>
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Procedimiento</th>
              <th>Anestesia</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            ${cirugiasHtml || '<tr><td colspan="4" style="text-align:center;">No hay cirugías registradas.</td></tr>'}
          </tbody>
        </table>

        <div class="section-title">Análisis de Laboratorio</div>
        <table>
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Prueba</th>
              <th>Resultado</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            ${labHtml || '<tr><td colspan="4" style="text-align:center;">No hay análisis registrados.</td></tr>'}
          </tbody>
        </table>

        <div class="section-title">Hospitalizaciones y Internamiento</div>
        <table>
          <thead>
            <tr>
              <th>Ingreso</th>
              <th>Alta</th>
              <th>Jaula</th>
              <th>Motivo</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            ${hospHtml || '<tr><td colspan="5" style="text-align:center;">No hay hospitalizaciones registradas.</td></tr>'}
          </tbody>
        </table>
      </div>

      <footer class="footer">
        <div class="note">Este historial médico unificado consolida todos los servicios clínicos del paciente. Debe conservarse para fines médicos y administrativos oficiales de la clínica.</div>
        <div class="signature">Firma Autorizada VetSphere</div>
      </footer>
    </section>
  </main>
  ${imprimirScript}
</body>
</html>`;
  }

  /**
   * Escapa los valores que vienen de la base de datos antes de insertarlos en
   * el HTML del historial: sin esto, un motivo o diagnostico con etiquetas
   * ejecutaria JavaScript al abrir el comprobante (XSS almacenado).
   */
  private escapar(valor?: string | number | null) {
    return String(valor ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
}
