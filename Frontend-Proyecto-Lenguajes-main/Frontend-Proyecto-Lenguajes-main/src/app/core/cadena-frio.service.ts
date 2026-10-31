import { Injectable, NgZone } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CadenaFrioService {
  private readonly base = environment.iotApiUrl;

  constructor(private http: HttpClient, private zone: NgZone) {}

  dashboard() { return this.http.get<any>(`${this.base}/admin/dashboard`); }
  refrigerators() { return this.http.get<any[]>(`${this.base}/admin/refrigerators`); }
  sensors() { return this.http.get<any[]>(`${this.base}/admin/sensors`); }
  alerts(status?: string) {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<any[]>(`${this.base}/admin/alerts`, { params });
  }
  readings(refrigeratorId?: number, start?: string, end?: string) {
    let params = new HttpParams().set('limit', '300');
    if (refrigeratorId) params = params.set('refrigerator_id', refrigeratorId);
    if (start) params = params.set('start', start);
    if (end) params = params.set('end', end);
    return this.http.get<any[]>(`${this.base}/admin/readings`, { params });
  }
  statistics(refrigeratorId?: number) {
    let params = new HttpParams().set('hours', '24');
    if (refrigeratorId) params = params.set('refrigerator_id', refrigeratorId);
    return this.http.get<any>(`${this.base}/admin/statistics`, { params });
  }
  storage() { return this.http.get<any[]>(`${this.base}/admin/storage`); }
  simulation() { return this.http.get<any>(`${this.base}/admin/simulation`); }
  createRefrigerator(body: any) { return this.http.post<any>(`${this.base}/admin/refrigerators`, body); }
  updateRefrigerator(id: number, body: any) { return this.http.patch<any>(`${this.base}/admin/refrigerators/${id}`, body); }
  deactivateRefrigerator(id: number) { return this.http.delete<any>(`${this.base}/admin/refrigerators/${id}`); }
  createSensor(body: any) { return this.http.post<any>(`${this.base}/admin/sensors`, body); }
  rotateSensorKey(id: number) { return this.http.post<any>(`${this.base}/admin/sensors/${id}/rotate-key`, {}); }
  deactivateSensor(id: number) { return this.http.delete<any>(`${this.base}/admin/sensors/${id}`); }
  exportReadings(format: 'xlsx' | 'pdf', refrigeratorId?: number, start?: string, end?: string) {
    let params = new HttpParams().set('format', format);
    if (refrigeratorId) params = params.set('refrigerator_id', refrigeratorId);
    if (start) params = params.set('start', start);
    if (end) params = params.set('end', end);
    return this.http.get(`${this.base}/admin/readings/export`, { params, responseType: 'blob' });
  }
  saveProductStorage(body: any) { return this.http.post<any>(`${this.base}/admin/storage`, body); }
  reviewStorage(id: number, body: any) { return this.http.patch<any>(`${this.base}/admin/storage/${id}/review`, body); }
  manageAlert(id: number, body: any) { return this.http.post<any>(`${this.base}/admin/alerts/${id}/actions`, body); }
  startSimulation(body: any) { return this.http.post<any>(`${this.base}/admin/simulation/start`, body); }
  stopSimulation() { return this.http.post<any>(`${this.base}/admin/simulation/stop`, {}); }
  restartSimulation(body: any) { return this.http.post<any>(`${this.base}/admin/simulation/restart`, body); }
  forceCritical(id: number) { return this.http.post<any>(`${this.base}/admin/simulation/force-critical/${id}`, {}); }
  disconnectSensor(id: number) { return this.http.post<any>(`${this.base}/admin/simulation/sensors/${id}/disconnect`, {}); }
  reconnectSensor(id: number) { return this.http.post<any>(`${this.base}/admin/simulation/sensors/${id}/reconnect`, {}); }

  conectarLive(onEvent: (event: any) => void, onState?: (open: boolean) => void): () => void {
    let socket: WebSocket | null = null;
    let closed = false;
    let retry: ReturnType<typeof setTimeout> | null = null;
    const connect = () => {
      if (closed) return;
      const token = localStorage.getItem('vetsphere_token');
      if (!token) return;
      const base = new URL(this.base, window.location.href);
      base.protocol = base.protocol === 'https:' ? 'wss:' : 'ws:';
      base.pathname = `${base.pathname.replace(/\/$/, '')}/ws`;
      // Envía el JWT como subprotocolo del handshake, nunca en la URL.
      socket = new WebSocket(base.toString(), ['vetsphere', `vetsphere.jwt.${token}`]);
      socket.onopen = () => this.zone.run(() => onState?.(true));
      socket.onmessage = message => {
        try {
          const event = JSON.parse(message.data);
          this.zone.run(() => onEvent(event));
        } catch { /* Ignora mensajes que no tengan el formato del protocolo. */ }
      };
      socket.onclose = () => {
        this.zone.run(() => onState?.(false));
        if (!closed) retry = setTimeout(connect, 3000);
      };
      socket.onerror = () => socket?.close();
    };
    connect();
    return () => {
      closed = true;
      if (retry) clearTimeout(retry);
      socket?.close();
    };
  }
}
