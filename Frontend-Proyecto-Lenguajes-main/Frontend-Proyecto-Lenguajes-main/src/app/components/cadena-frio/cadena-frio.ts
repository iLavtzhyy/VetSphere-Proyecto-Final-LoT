import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { CadenaFrioService } from '../../core/cadena-frio.service';
import { Producto } from '../../core/modelos';
import { IconoComponent } from '../../shared/components/icono/icono';

@Component({
  selector: 'app-cadena-frio', standalone: true,
  imports: [CommonModule, FormsModule, IconoComponent],
  templateUrl: './cadena-frio.html', styleUrl: './cadena-frio.css'
})
export class CadenaFrioComponent implements OnInit, OnDestroy {
  dashboard: any = null;
  refrigeradores: any[] = [];
  sensores: any[] = [];
  alertas: any[] = [];
  lecturas: any[] = [];
  storage: any[] = [];
  productos: Producto[] = [];
  estadisticas: any = null;
  seleccionRefrigerador: number | null = null;
  fechaDesde = '';
  fechaHasta = '';
  conectada = false;
  cargando = false;
  simulacion = { running: false, intervalSeconds: 5, mode: 'NORMAL', simulated: true };
  error = '';
  mensaje = '';
  tokenSensor = '';
  mostrarFormularioNevera = false;
  mostrarFormularioSensor = false;
  neveraEditando: number | null = null;
  editarNevera = { name: '', location: '', capacity: '', min_temp: 2, max_temp: 8 };
  nuevaNevera = { code: '', name: '', location: '', capacity: '', min_temp: 2, max_temp: 8 };
  nuevoSensor = { device_id: '', refrigerator_id: 0, sensor_type: 'DS18B20' };
  nuevaAsignacion = { product_code: '', refrigerator_id: 0 };
  alertaEnEdicion: number | null = null;
  notaAlerta = '';
  private desconectarLive: (() => void) | null = null;

  constructor(private frio: CadenaFrioService, private api: ApiService) {}

  ngOnInit() {
    this.cargarTodo();
    this.api.productos().subscribe({ next: r => this.productos = r.data || [] });
    this.desconectarLive = this.frio.conectarLive(event => this.recibirEvento(event), open => this.conectada = open);
  }

  ngOnDestroy() { this.desconectarLive?.(); }

  cargarTodo() {
    this.cargando = true;
    this.error = '';
    this.frio.dashboard().pipe(finalize(() => this.cargando = false)).subscribe({
      next: r => { this.dashboard = r; this.refrigeradores = r.refrigerators || []; this.simulacion = r.simulation || this.simulacion; },
      error: e => this.error = this.errorDe(e, 'No se pudo conectar con el servicio de cadena de frío.')
    });
    this.frio.sensors().subscribe({ next: r => this.sensores = r || [], error: e => this.error = this.errorDe(e, this.error) });
    this.frio.alerts().subscribe({ next: r => this.alertas = r || [], error: e => this.error = this.errorDe(e, this.error) });
    this.cargarLecturas();
    this.frio.statistics(this.seleccionRefrigerador || undefined).subscribe({ next: r => this.estadisticas = r, error: e => this.error = this.errorDe(e, this.error) });
    this.frio.storage().subscribe({ next: r => this.storage = r || [], error: e => this.error = this.errorDe(e, this.error) });
  }

  private recibirEvento(event: any) {
    if (event?.type === 'simulation') this.simulacion = event.data;
    if (event?.type === 'reading') {
      const reading = event.data;
      this.lecturas = [reading, ...this.lecturas.filter(item => item.id !== reading.id)].slice(0, 300);
      if (reading.alert) this.alertas = [reading.alert, ...this.alertas.filter(item => item.id !== reading.alert.id)];
      this.actualizarTarjeta(reading);
      this.refrescarEstadisticas();
    }
    if (event?.type === 'alert' && event.data?.id) this.alertas = [event.data, ...this.alertas.filter(item => item.id !== event.data.id)];
    if (event?.type === 'sensor') this.frio.sensors().subscribe({ next: r => this.sensores = r || [] });
  }

  private actualizarTarjeta(reading: any) {
    if (!this.dashboard?.refrigerators) return;
    this.dashboard = { ...this.dashboard, refrigerators: this.dashboard.refrigerators.map((fridge: any) =>
      fridge.refrigeratorId === reading.refrigeratorId ? { ...fridge, temperature: reading.temperature,
        measuredAt: reading.timestamp, lastSeen: reading.receivedAt,
        normalNow: reading.condition === 'NORMAL', sensorStatus: 'ACTIVE', openAlerts: reading.alert ? (fridge.openAlerts || 0) + 1 : fridge.openAlerts }
        : fridge) };
    this.refrigeradores = this.dashboard.refrigerators;
  }

  private refrescarEstadisticas() {
    this.frio.statistics(this.seleccionRefrigerador || undefined).subscribe({ next: r => this.estadisticas = r });
  }

  refrescar() { this.cargarTodo(); }

  elegirRefrigerador(id: number | null) {
    this.seleccionRefrigerador = id;
    this.cargarTodo();
  }

  cargarLecturas() {
    this.frio.readings(this.seleccionRefrigerador || undefined, this.fechaUtc(this.fechaDesde, false), this.fechaUtc(this.fechaHasta, true))
      .subscribe({ next: r => this.lecturas = r || [], error: e => this.error = this.errorDe(e, this.error) });
  }

  iniciarSimulacion() {
    this.error = '';
    this.frio.startSimulation(this.simulacion).subscribe({ next: r => { this.simulacion = r; this.mensaje = 'Simulación iniciada; las lecturas quedan marcadas como SIMULADAS.'; }, error: e => this.error = this.errorDe(e, 'No se pudo iniciar la simulación.') });
  }

  detenerSimulacion() {
    this.frio.stopSimulation().subscribe({ next: r => { this.simulacion = r; this.mensaje = 'Simulación detenida. El historial se conserva.'; }, error: e => this.error = this.errorDe(e, 'No se pudo detener la simulación.') });
  }

  reiniciarSimulacion() {
    this.frio.restartSimulation(this.simulacion).subscribe({ next: r => { this.simulacion = r; this.mensaje = 'Simulación reiniciada; se conserva la trazabilidad anterior.'; this.cargarTodo(); }, error: e => this.error = this.errorDe(e, 'No se pudo reiniciar la simulación.') });
  }

  forzarCritica(fridge: any) {
    this.frio.forceCritical(fridge.refrigeratorId).subscribe({ next: () => this.mensaje = `Alerta crítica simulada en ${fridge.name}.`, error: e => this.error = this.errorDe(e, 'No se pudo provocar la lectura crítica.') });
  }

  desconectarSensor(fridge: any) {
    const sensor = this.sensores.find(item => item.refrigeratorId === fridge.refrigeratorId && item.simulated);
    if (!sensor) { this.error = 'Esta nevera no tiene un sensor simulado asociado.'; return; }
    this.frio.disconnectSensor(sensor.id).subscribe({ next: () => { this.mensaje = `${sensor.deviceId} desconectado; se generó una alerta de comunicación.`; this.cargarTodo(); }, error: e => this.error = this.errorDe(e, 'No se pudo desconectar el sensor simulado.') });
  }

  reconectarSensor(sensor: any) {
    this.frio.reconnectSensor(sensor.id).subscribe({ next: () => { this.mensaje = `${sensor.deviceId} reconectado. La alerta debe cerrarse manualmente.`; this.cargarTodo(); }, error: e => this.error = this.errorDe(e, 'No se pudo reconectar el sensor.') });
  }

  renovarClave(sensor: any) {
    this.frio.rotateSensorKey(sensor.id).subscribe({
      next: r => { this.tokenSensor = r.deviceToken; this.mensaje = r.tokenWarning; },
      error: e => this.error = this.errorDe(e, 'No se pudo renovar la credencial.')
    });
  }

  guardarNevera() {
    this.error = '';
    this.frio.createRefrigerator(this.nuevaNevera).subscribe({
      next: () => { this.mensaje = 'Nevera registrada.'; this.nuevaNevera = { code: '', name: '', location: '', capacity: '', min_temp: 2, max_temp: 8 }; this.mostrarFormularioNevera = false; this.cargarTodo(); },
      error: e => this.error = this.errorDe(e, 'No se pudo registrar la nevera.')
    });
  }

  desactivarNevera(fridge: any) {
    this.frio.deactivateRefrigerator(fridge.refrigeratorId).subscribe({ next: () => { this.mensaje = 'Nevera desactivada; su historial se conserva.'; this.cargarTodo(); }, error: e => this.error = this.errorDe(e, 'No se pudo desactivar la nevera.') });
  }

  iniciarEdicionNevera(fridge: any) {
    this.neveraEditando = fridge.refrigeratorId;
    this.editarNevera = { name: fridge.name, location: fridge.location, capacity: fridge.capacity || '', min_temp: fridge.minTemp, max_temp: fridge.maxTemp };
  }

  guardarEdicionNevera(fridge: any) {
    this.frio.updateRefrigerator(fridge.refrigeratorId, this.editarNevera).subscribe({
      next: () => { this.neveraEditando = null; this.mensaje = 'Nevera actualizada.'; this.cargarTodo(); },
      error: e => this.error = this.errorDe(e, 'No se pudo actualizar la nevera.')
    });
  }

  guardarSensor() {
    this.error = '';
    this.frio.createSensor(this.nuevoSensor).subscribe({
      next: r => { this.tokenSensor = r.deviceToken; this.mensaje = 'Sensor registrado. Copia la clave del dispositivo ahora; solo se muestra una vez.'; this.nuevoSensor = { device_id: '', refrigerator_id: 0, sensor_type: 'DS18B20' }; this.cargarTodo(); },
      error: e => this.error = this.errorDe(e, 'No se pudo registrar el sensor.')
    });
  }

  copiarClave() {
    if (this.tokenSensor && navigator.clipboard) navigator.clipboard.writeText(this.tokenSensor).then(() => this.mensaje = 'Credencial copiada al portapapeles.');
  }

  asignarProducto() {
    this.frio.saveProductStorage(this.nuevaAsignacion).subscribe({ next: () => { this.mensaje = 'Producto asociado a la nevera.'; this.nuevaAsignacion = { product_code: '', refrigerator_id: 0 }; this.cargarTodo(); }, error: e => this.error = this.errorDe(e, 'No se pudo asociar el producto.') });
  }

  actualizarRevision(storage: any, required: boolean) {
    const note = required ? 'Marcado para revisión de conservación.' : 'Revisión del producto documentada por el administrador.';
    this.frio.reviewStorage(storage.id, { review_required: required, note }).subscribe({ next: () => { this.mensaje = required ? 'Producto marcado para revisión.' : 'Revisión registrada; confirma el estado según el protocolo clínico.'; this.cargarTodo(); }, error: e => this.error = this.errorDe(e, 'No se pudo registrar la revisión.') });
  }

  desactivarSensor(sensor: any) {
    this.frio.deactivateSensor(sensor.id).subscribe({ next: () => { this.mensaje = 'Sensor desactivado; las mediciones históricas se conservan.'; this.cargarTodo(); }, error: e => this.error = this.errorDe(e, 'No se pudo desactivar el sensor.') });
  }

  descargarReporte(format: 'xlsx' | 'pdf') {
    this.frio.exportReadings(format, this.seleccionRefrigerador || undefined, this.fechaUtc(this.fechaDesde, false), this.fechaUtc(this.fechaHasta, true)).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob); const link = document.createElement('a');
        link.href = url; link.download = `vetsphere-cadena-frio.${format}`; link.click(); URL.revokeObjectURL(url);
      }, error: e => this.error = this.errorDe(e, 'No se pudo descargar el reporte.')
    });
  }

  atenderAlerta(alerta: any, status: 'ATTENDED' | 'RESOLVED') {
    if (this.alertaEnEdicion !== alerta.id) { this.alertaEnEdicion = alerta.id; this.notaAlerta = ''; return; }
    if (this.notaAlerta.trim().length < 3) { this.error = 'Escribe una nota breve para registrar la acción realizada.'; return; }
    this.frio.manageAlert(alerta.id, { status, note: this.notaAlerta.trim() }).subscribe({
      next: () => { this.mensaje = status === 'RESOLVED' ? 'Alerta resuelta y acción guardada.' : 'Atención registrada; la alerta sigue abierta hasta su resolución.'; this.alertaEnEdicion = null; this.notaAlerta = ''; this.cargarTodo(); },
      error: e => this.error = this.errorDe(e, 'No se pudo guardar la acción de la alerta.')
    });
  }

  estadoNevera(fridge: any): 'normal' | 'warning' | 'critical' | 'offline' {
    if (!fridge.sensorStatus || fridge.sensorStatus !== 'ACTIVE' || fridge.temperature == null) return 'offline';
    const age = fridge.lastSeen ? Date.now() - new Date(fridge.lastSeen).getTime() : Infinity;
    if (age > 30_000) return 'offline';
    if ((fridge.openAlerts || 0) > 0 && !fridge.normalNow) return 'critical';
    const value = Number(fridge.temperature);
    if (value < Number(fridge.minTemp) || value > Number(fridge.maxTemp)) return 'critical';
    if (value - Number(fridge.minTemp) <= .5 || Number(fridge.maxTemp) - value <= .5) return 'warning';
    return 'normal';
  }

  textoEstado(fridge: any) {
    const state = this.estadoNevera(fridge);
    return state === 'normal' ? 'En rango' : state === 'warning' ? 'Próxima al límite' : state === 'critical' ? 'Requiere atención' : 'Sin comunicación';
  }

  tieneSensorSimulado(fridge: any): boolean {
    return this.sensores.some(sensor => sensor.refrigeratorId === fridge.refrigeratorId && sensor.simulated && sensor.status !== 'INACTIVE');
  }

  nombreProducto(code: string) { return this.productos.find(product => product.codigo === code)?.nombre || code; }

  get puntosGrafica(): string {
    const values = [...this.lecturas].slice(0, 80).reverse().map(item => Number(item.temperature)).filter(Number.isFinite);
    if (values.length < 2) return '';
    const low = Math.min(...values) - .4; const high = Math.max(...values) + .4; const span = Math.max(high - low, .2);
    return values.map((value, index) => `${(index / (values.length - 1)) * 100},${94 - ((value - low) / span) * 82}`).join(' ');
  }

  get fechaAhora() { return new Date(); }

  private errorDe(error: any, fallback: string): string {
    if (error?.status === 0) return 'No hay conexión con el servicio IoT en localhost:8091.';
    return error?.error?.detail || error?.error?.mensaje || fallback;
  }

  private fechaUtc(date: string, end: boolean): string | undefined {
    if (!date) return undefined;
    const local = new Date(`${date}T${end ? '23:59:59' : '00:00:00'}`);
    return local.toISOString();
  }
}
