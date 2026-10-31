import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { AutorizacionService } from '../../core/autorizacion.service';
import { ChatConversacionDetalle, ChatConversacionResumen } from '../../core/modelos';

interface MensajeChat {
  rol: 'user' | 'assistant';
  contenido: string;
  error?: boolean;
}

interface BorradorCupon {
  codigo?: string;
  descripcion?: string;
  tipoDescuento?: string;
  valor?: number;
  productoCodigo?: string;
  categoria?: string;
  fechaInicio?: string;
  fechaFin?: string;
  activo?: boolean;
}

/** Widget flotante de asistente LLM (estilo mensajería) presente en todo el layout autenticado. */
@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot.html',
  styleUrl: './chatbot.css'
})
export class ChatbotComponent implements OnInit {
  private readonly maxMensajesEnPantalla = 200;
  abierto = false;
  enviando = false;
  texto = '';
  mensajes: MensajeChat[] = [];
  /** Contexto en memoria de esta sesión; el historial duradero vive en la base de datos. */
  historial: { rol: string; contenido: string }[] = [];
  conversacionId: number | null = null;
  panelAdmin = false;
  conversacionesAdmin: ChatConversacionResumen[] = [];
  conversacionAdmin: ChatConversacionDetalle | null = null;
  cargandoHistorialAdmin = false;
  mensajeAdmin = '';
  eliminandoHistorial = false;
  modalBorrado: { tipo: 'conversacion' | 'todo'; id?: number } | null = null;
  pasoCupon: 'motivo' | 'tipo' | 'valor' | 'alcance' | 'categoria' | 'producto' | 'vigencia' | 'codigo' | 'confirmacion' | null = null;
  private borradorCupon: BorradorCupon = {};

  @ViewChild('cajaMensajes') cajaMensajes?: ElementRef<HTMLElement>;
  @ViewChild('entradaTexto') entradaTexto?: ElementRef<HTMLTextAreaElement>;

  constructor(private api: ApiService, private autorizacion: AutorizacionService) {}

  ngOnInit() {
    this.api.chatConversacionActual().subscribe({
      next: r => {
        const conversacion = r.data;
        if (!conversacion) return;
        this.conversacionId = conversacion.id;
        this.mensajes = conversacion.mensajes.map(m => ({ rol: m.rol, contenido: m.contenido }));
        this.historial = this.mensajes.map(m => ({ rol: m.rol, contenido: m.contenido }));
      }
    });
  }

  get esAdmin() { return this.autorizacion.tieneRol(['ROLE_ADMIN']); }

  get descripcionCapacidades(): string {
    if (this.autorizacion.tieneRol(['ROLE_ADMIN'])) return 'Puedo consultar la clínica y guiarte para crear cupones.';
    if (this.autorizacion.tieneRol(['ROLE_RECEPCIONISTA'])) return 'Puedo ayudarte con citas, clientes, mascotas, farmacia y facturas.';
    if (this.autorizacion.tieneRol(['ROLE_VETERINARIO'])) return 'Puedo ayudarte con mascotas, historiales clínicos y citas.';
    if (this.autorizacion.tieneRol(['ROLE_CLIENTE'])) return 'Puedo consultar tus mascotas, sus historiales y tus citas.';
    return 'No hay consultas habilitadas para tu rol.';
  }

  abrirPanelAdmin() {
    if (!this.esAdmin) return;
    this.panelAdmin = true;
    this.mensajeAdmin = '';
    this.conversacionAdmin = null;
    this.cargandoHistorialAdmin = true;
    this.api.chatConversacionesAdmin().subscribe({
      next: r => { this.conversacionesAdmin = r.data || []; this.cargandoHistorialAdmin = false; },
      error: () => { this.conversacionesAdmin = []; this.cargandoHistorialAdmin = false; }
    });
  }

  verConversacionAdmin(id: number) {
    this.api.chatConversacionAdmin(id).subscribe({ next: r => this.conversacionAdmin = r.data });
  }

  eliminarConversacionAdmin(id: number, evento?: Event) {
    evento?.stopPropagation();
    if (!this.esAdmin) return;
    this.modalBorrado = { tipo: 'conversacion', id };
  }

  eliminarTodoHistorialAdmin() {
    if (!this.esAdmin || this.eliminandoHistorial || !this.conversacionesAdmin.length) return;
    this.modalBorrado = { tipo: 'todo' };
  }

  cancelarBorrado() { this.modalBorrado = null; }

  confirmarBorrado() {
    if (!this.esAdmin || !this.modalBorrado) return;
    const solicitud = this.modalBorrado;
    this.modalBorrado = null;
    if (solicitud.tipo === 'conversacion' && solicitud.id != null) this.ejecutarBorradoConversacion(solicitud.id);
    if (solicitud.tipo === 'todo') this.ejecutarBorradoTotal();
  }

  private ejecutarBorradoConversacion(id: number) {
    this.api.chatEliminarConversacionAdmin(id).subscribe({
      next: r => {
        this.conversacionesAdmin = this.conversacionesAdmin.filter(c => c.id !== id);
        if (this.conversacionAdmin?.id === id) this.conversacionAdmin = null;
        this.mensajeAdmin = r.mensaje || 'Conversación eliminada.';
      },
      error: e => this.mensajeAdmin = e.error?.mensaje || 'No se pudo eliminar la conversación.'
    });
  }

  private ejecutarBorradoTotal() {
    if (!this.esAdmin || this.eliminandoHistorial || !this.conversacionesAdmin.length) return;
    this.eliminandoHistorial = true;
    this.api.chatEliminarHistorialAdmin().subscribe({
      next: r => {
        this.conversacionesAdmin = [];
        this.conversacionAdmin = null;
        this.eliminandoHistorial = false;
        const total = r.data ?? 0;
        this.mensajeAdmin = total === 1 ? '1 conversación eliminada.' : `${total} conversaciones eliminadas.`;
      },
      error: e => {
        this.eliminandoHistorial = false;
        this.mensajeAdmin = e.error?.mensaje || 'No se pudo eliminar el historial.';
      }
    });
  }

  volverAConversaciones() { this.conversacionAdmin = null; }
  cerrarPanelAdmin() { this.panelAdmin = false; this.conversacionAdmin = null; }

  alternar() {
    this.abierto = !this.abierto;
    if (this.abierto) {
      setTimeout(() => this.entradaTexto?.nativeElement.focus());
    }
  }

  cerrar() {
    this.abierto = false;
  }

  enviar() {
    const contenido = this.texto.trim();
    if (!contenido || this.enviando) return;
    this.texto = '';
    this.restablecerAltura();
    this.agregarMensaje({ rol: 'user', contenido });
    this.enviando = true;
    this.desplazarAlFinal();

    if (this.pasoCupon) {
      this.procesarCreacionCupon(contenido);
      return;
    }
    if (/^(ayuda|que puedes hacer|qué puedes hacer|que puedo consultar|qué puedo consultar|mis permisos)\??$/i.test(contenido.trim())) {
      const respuesta = `Según tu rol, puedo ayudarte con esto: ${this.descripcionCapacidades} Solo consultaré esa información y no haré cambios sin el flujo autorizado.`;
      this.responderLocal(respuesta);
      return;
    }
    if (/\b(crear|nuevo|nueva|registrar)\b.*\b(cupon(?:es)?|cup[oó]n(?:es)?|promoci[oó]n(?:es)?)\b/i.test(contenido)) {
      if (!this.autorizacion.tieneRol(['ROLE_ADMIN'])) {
        this.responderLocal('La creación de cupones está permitida únicamente al administrador. Puedo ayudarte con consultas que correspondan a tu rol.');
        return;
      }
      this.borradorCupon = { activo: true };
      this.pasoCupon = 'motivo';
      this.responderLocal('¿Para qué será la promoción? Escribe una descripción breve, por ejemplo: “Descuento para vacunas de temporada”.');
      return;
    }

    this.api.chat(contenido, this.conversacionId).subscribe({
      next: r => {
        this.enviando = false;
        if (r.ok && r.data?.respuesta) {
          this.conversacionId = r.data.conversacionId ?? this.conversacionId;
          this.agregarMensaje({ rol: 'assistant', contenido: r.data.respuesta });
          this.registrarHistorial(contenido, r.data.respuesta);
        } else {
          this.agregarMensaje({
            rol: 'assistant',
            contenido: r.mensaje || 'El asistente no está configurado. Intenta más tarde.',
            error: true
          });
        }
        this.desplazarAlFinal();
      },
      error: (e: HttpErrorResponse) => {
        this.enviando = false;
        this.agregarMensaje({
          rol: 'assistant',
          contenido: e.error?.mensaje || 'No se pudo conectar con el asistente. Revisa que el backend esté activo.',
          error: true
        });
        this.desplazarAlFinal();
      }
    });
  }

  private procesarCreacionCupon(respuesta: string) {
    const valor = respuesta.trim();
    const cancelar = /^(no|cancelar|cancela|salir|detener)$/i.test(valor);
    if (cancelar) {
      this.pasoCupon = null;
      this.borradorCupon = {};
      this.responderLocal('Listo, cancelé la creación. No se guardó ningún cupón.');
      return;
    }

    switch (this.pasoCupon) {
      case 'motivo':
        this.borradorCupon.descripcion = valor;
        this.pasoCupon = 'tipo';
        this.responderLocal('¿Qué tipo de descuento quieres: porcentaje o valor fijo?');
        return;
      case 'tipo': {
        const tipo = /porcentaje|%/.test(valor.toLowerCase()) ? 'PORCENTAJE'
          : /fijo|d[oó]lares|monto|valor/.test(valor.toLowerCase()) ? 'VALOR_FIJO' : '';
        if (!tipo) { this.responderLocal('Indica “porcentaje” o “valor fijo”.'); return; }
        this.borradorCupon.tipoDescuento = tipo;
        this.pasoCupon = 'valor';
        this.responderLocal(tipo === 'PORCENTAJE' ? '¿Qué porcentaje de descuento aplico? (máximo 100)' : '¿Qué valor fijo de descuento aplico?');
        return;
      }
      case 'valor': {
        const numero = Number(valor.replace(',', '.').replace(/[^\d.]/g, ''));
        if (!Number.isFinite(numero) || numero <= 0 || (this.borradorCupon.tipoDescuento === 'PORCENTAJE' && numero > 100)) {
          this.responderLocal('Escribe un valor numérico mayor que cero' + (this.borradorCupon.tipoDescuento === 'PORCENTAJE' ? ' y no mayor a 100.' : '.'));
          return;
        }
        this.borradorCupon.valor = numero;
        this.pasoCupon = 'alcance';
        this.responderLocal('¿Dónde se aplica: a toda la farmacia, a una categoría o a un producto?');
        return;
      }
      case 'alcance': {
        const texto = valor.toLowerCase();
        if (/todo|toda|farmacia|cat[aá]logo|general/.test(texto)) {
          this.pasoCupon = 'vigencia';
          this.responderLocal('¿Cuántos días estará vigente? Escribe un número, o “30” para usar 30 días.');
        } else if (/categor[ií]a/.test(texto)) {
          this.pasoCupon = 'categoria';
          this.responderLocal('¿A qué categoría se aplica?');
        } else if (/producto/.test(texto)) {
          this.pasoCupon = 'producto';
          this.responderLocal('Indica el código del producto del inventario.');
        } else this.responderLocal('Elige toda la farmacia, una categoría o un producto.');
        return;
      }
      case 'categoria':
        this.borradorCupon.categoria = valor;
        this.pasoCupon = 'vigencia';
        this.responderLocal('¿Cuántos días estará vigente? Escribe un número, o “30” para usar 30 días.');
        return;
      case 'producto':
        this.api.productos().subscribe({
          next: r => {
            const producto = r.data?.find(p => p.codigo.toLowerCase() === valor.toLowerCase());
            if (!r.ok || !producto) {
              this.responderLocal('No encontré ese código en el inventario. Revisa el código e inténtalo de nuevo.');
              return;
            }
            this.borradorCupon.productoCodigo = producto.codigo;
            this.pasoCupon = 'vigencia';
            this.responderLocal(`Se aplicará a ${producto.nombre}. ¿Cuántos días estará vigente? (por ejemplo, 30)`);
          },
          error: () => this.responderLocal('No pude consultar el inventario. Intenta de nuevo con el código del producto.')
        });
        return;
      case 'vigencia': {
        const dias = /^(no|30|predeterminado)$/i.test(valor) ? 30 : Number(valor);
        if (!Number.isInteger(dias) || dias < 1 || dias > 365) {
          this.responderLocal('Indica un número de días entre 1 y 365 (o escribe “30”).');
          return;
        }
        const inicio = new Date();
        const fin = new Date(inicio.getTime() + dias * 24 * 60 * 60 * 1000);
        this.borradorCupon.fechaInicio = this.fechaLocal(inicio);
        this.borradorCupon.fechaFin = this.fechaLocal(fin);
        this.pasoCupon = 'codigo';
        this.responderLocal('¿Quieres un código propio? Escríbelo, o escribe “automático” para que el sistema lo genere.');
        return;
      }
      case 'codigo':
        if (!/^(auto(m[aá]tico)?|generado|no)$/i.test(valor) && valor.length > 32) {
          this.responderLocal('El código puede tener hasta 32 caracteres. Escríbelo de nuevo o usa “automático”.');
          return;
        }
        if (!/^(auto(m[aá]tico)?|generado|no)$/i.test(valor)) this.borradorCupon.codigo = valor.toUpperCase().replace(/\s+/g, '-');
        this.pasoCupon = 'confirmacion';
        this.responderLocal(this.resumenCupon() + '\n\n¿Confirmas que lo cree? Pulsa “Confirmar” o escribe “sí”.');
        return;
      case 'confirmacion':
        if (/^(s[ií]|confirmar|confirmo|crear|aceptar)$/i.test(valor)) {
          this.guardarCupon();
        } else {
          this.pasoCupon = null;
          this.borradorCupon = {};
          this.responderLocal('Cancelé la creación. No se guardó ningún cupón.');
        }
        return;
    }
  }

  confirmarCreacionCupon(confirmar: boolean) {
    if (this.pasoCupon !== 'confirmacion') return;
    if (confirmar) this.guardarCupon();
    else {
      this.pasoCupon = null;
      this.borradorCupon = {};
      this.responderLocal('Cancelé la creación. No se guardó ningún cupón.');
    }
  }

  private guardarCupon() {
    if (!this.autorizacion.tieneRol(['ROLE_ADMIN'])) {
      this.pasoCupon = null;
      this.responderLocal('Tu rol no tiene permiso para crear cupones.');
      return;
    }
    this.enviando = true;
    this.api.crearCupon(this.borradorCupon).subscribe({
      next: r => {
        this.pasoCupon = null;
        const codigo = r.data?.codigo || this.borradorCupon.codigo || 'generado automáticamente';
        this.borradorCupon = {};
        this.responderLocal(r.ok ? `Cupón creado correctamente. Código: ${codigo}.` : (r.mensaje || 'No se pudo crear el cupón.'));
      },
      error: e => {
        this.enviando = false;
        const mensaje = e.status === 0
          ? 'No hay conexión con el servidor. El borrador del cupón se conservó; cuando el backend esté activo, pulsa “Confirmar creación” para reintentar.'
          : (e.error?.mensaje || 'No se pudo crear el cupón. El borrador se conservó; revisa el mensaje y vuelve a confirmar.');
        this.responderLocal(mensaje, true);
      }
    });
  }

  private resumenCupon(): string {
    const alcance = this.borradorCupon.productoCodigo ? `producto ${this.borradorCupon.productoCodigo}`
      : this.borradorCupon.categoria ? `categoría ${this.borradorCupon.categoria}` : 'toda la farmacia';
    const codigo = this.borradorCupon.codigo || 'generado automáticamente';
    const fechaInicio = this.borradorCupon.fechaInicio?.slice(0, 10) || '';
    const fechaFin = this.borradorCupon.fechaFin?.slice(0, 10) || '';
    return `Resumen del cupón:\n• Motivo: ${this.borradorCupon.descripcion}\n• Descuento: ${this.borradorCupon.tipoDescuento === 'PORCENTAJE' ? `${this.borradorCupon.valor}%` : `$${this.borradorCupon.valor}`}\n• Aplicación: ${alcance}\n• Vigencia: ${fechaInicio} al ${fechaFin}\n• Código: ${codigo}`;
  }

  private fechaLocal(fecha: Date): string {
    const dos = (n: number) => String(n).padStart(2, '0');
    return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}T${dos(fecha.getHours())}:${dos(fecha.getMinutes())}:${dos(fecha.getSeconds())}`;
  }

  private responderLocal(contenido: string, error = false) {
    this.enviando = false;
    this.agregarMensaje({ rol: 'assistant', contenido, error });
    const pregunta = [...this.mensajes].reverse().find(m => m.rol === 'user')?.contenido;
    if (pregunta) {
      this.api.chatGuardarTurnoLocal(pregunta, contenido, this.conversacionId).subscribe({
        next: r => this.conversacionId = r.data ?? this.conversacionId
      });
    }
    this.historial.push({ rol: 'user', contenido: pregunta || '' }, { rol: 'assistant', contenido });
    this.historial = this.historial.slice(-this.maxMensajesEnPantalla);
    this.desplazarAlFinal();
  }

  /**
   * Enter envia el mensaje (Shift+Enter inserta un salto de linea).
   * El evento llega tipado como `Event` cuando la plantilla usa el pseudo
   * evento `keydown.enter`, por eso se valida antes de usarlo.
   */
  presionarEnter(evento: Event) {
    const teclado = evento as KeyboardEvent;
    if (teclado.shiftKey) return;
    teclado.preventDefault();
    this.enviar();
  }

  /** Auto-resize simple del textarea (máx. 5 líneas visibles). */
  ajustarAltura(evento: Event) {
    const area = evento.target as HTMLTextAreaElement;
    area.style.height = 'auto';
    area.style.height = `${Math.min(area.scrollHeight, 120)}px`;
  }

  private restablecerAltura() {
    if (this.entradaTexto) this.entradaTexto.nativeElement.style.height = 'auto';
  }

  private registrarHistorial(enviado: string, respuesta: string) {
    this.historial.push({ rol: 'user', contenido: enviado }, { rol: 'assistant', contenido: respuesta });
    this.historial = this.historial.slice(-this.maxMensajesEnPantalla);
  }

  private agregarMensaje(mensaje: MensajeChat) {
    this.mensajes.push(mensaje);
    if (this.mensajes.length > this.maxMensajesEnPantalla) {
      this.mensajes.splice(0, this.mensajes.length - this.maxMensajesEnPantalla);
    }
  }

  private desplazarAlFinal() {
    setTimeout(() => {
      const caja = this.cajaMensajes?.nativeElement;
      if (caja) caja.scrollTop = caja.scrollHeight;
    });
  }

  trackMensaje(_: number, mensaje: MensajeChat) {
    return mensaje;
  }
}
