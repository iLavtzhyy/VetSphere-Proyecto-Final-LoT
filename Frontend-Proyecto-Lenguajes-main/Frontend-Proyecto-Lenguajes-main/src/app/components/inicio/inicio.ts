import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ThemeService } from '../../core/theme.service';
import { ApiService } from '../../core/api.service';
import { IconoComponent } from '../../shared/components/icono/icono';

interface VeterinarioCartelera {
  nombre: string;
  especialidad: string;
  foto: string;
  descripcion: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, IconoComponent],
  templateUrl: './inicio.html',
  styleUrl: './inicio.css'
})
export class HomeComponent implements OnInit {
  @ViewChild('carruselEquipo') carruselEquipo?: ElementRef<HTMLElement>;

  constructor(public theme: ThemeService, private api: ApiService) {}

  ngOnInit() {
    this.api.veterinarios().subscribe({
      next: respuesta => {
        const equipo = (respuesta.data || []).map(vet => ({
          nombre: `${vet.nombres || ''} ${vet.apellidos || ''}`.trim(),
          especialidad: vet.especialidad || 'Medicina veterinaria',
          foto: vet.fotoUrl || 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=700',
          descripcion: vet.biografia || 'Profesional de nuestro equipo médico veterinario.'
        })).filter(vet => vet.nombre.length > 0);
        if (equipo.length) this.veterinarios = equipo;
      },
      // La portada sigue disponible aunque la API del equipo esté temporalmente desconectada.
      error: () => undefined
    });
  }

  desplazarEquipo(direccion: -1 | 1) {
    const pista = this.carruselEquipo?.nativeElement;
    if (!pista) return;
    pista.scrollBy({ left: direccion * Math.max(pista.clientWidth * 0.82, 280), behavior: 'smooth' });
  }

  menuAbierto = false;
  whatsapp = '0989188046';
  mensajeWhatsApp = 'Hola VetSphere, quiero agendar una consulta veterinaria. Me ayudan con informacion de horarios y disponibilidad?';
  whatsappUrl = `https://wa.me/593989188046?text=${encodeURIComponent(this.mensajeWhatsApp)}`;

  navItems = [
    { label: 'Nosotros', href: '#nosotros', icono: 'shield' },
    { label: 'Servicios', href: '#servicios', icono: 'stethoscope' },
    { label: 'Especialidades', href: '#especialidades', icono: 'syringe' },
    { label: 'Clientes', href: '#clientes', icono: 'users' },
    { label: 'Instalaciones', href: '#instalaciones', icono: 'map' },
    { label: 'Veterinarios', href: '#veterinarios', icono: 'users' },
    { label: 'Contacto', href: '#contacto', icono: 'phone' }
  ];

  servicios = [
    { icono: 'stethoscope', area: 'Clinica', titulo: 'Consultas medicas', texto: 'Registro de diagnostico, tratamiento, precio, signos vitales y seguimiento por paciente.' },
    { icono: 'surgery', area: 'Quirofano', titulo: 'Cirugias', texto: 'Agenda quirurgica, tipo de anestesia, cirujano responsable, constantes y estado postoperatorio.' },
    { icono: 'lab', area: 'Diagnostico', titulo: 'Laboratorio', texto: 'Ordenes de laboratorio, resultados, archivos medicos y control de solicitudes.' },
    { icono: 'pill', area: 'Farmacia', titulo: 'Inventario', texto: 'Mas de 100 productos, stock critico, precios, movimientos y reportes Excel.' },
    { icono: 'file', area: 'Administracion', titulo: 'Facturacion', texto: 'Gastos por cliente, facturas pendientes, reportes financieros y control de caja.' },
    { icono: 'users', area: 'Recepcion', titulo: 'Citas y clientes', texto: 'Registro de nuevos tutores, mascotas, citas, aprobaciones y contacto por WhatsApp.' }
  ];

  especialidades = [
    { titulo: 'Medicina interna', texto: 'Diagnostico de enfermedades digestivas, respiratorias, renales, cardiacas y metabolicas.' },
    { titulo: 'Cirugia veterinaria', texto: 'Procedimientos programados, esterilizaciones, tejidos blandos y controles postoperatorios.' },
    { titulo: 'Dermatologia', texto: 'Alergias, otitis, lesiones de piel, infecciones y seguimiento terapeutico.' },
    { titulo: 'Laboratorio clinico', texto: 'Ordenes, resultados y archivos medicos asociados al historial del paciente.' },
    { titulo: 'Farmacia veterinaria', texto: 'Medicamentos, vacunas, insumos y control de stock critico con reportes.' },
    { titulo: 'Emergencias', texto: 'Triage, estabilizacion inicial y coordinacion de atencion prioritaria.' }
  ];

  pasos = [
    { titulo: 'Contacto', texto: 'El tutor agenda por recepcion, pagina web o WhatsApp.' },
    { titulo: 'Ingreso', texto: 'Recepcion registra cliente, mascota, motivo y prioridad de atencion.' },
    { titulo: 'Consulta', texto: 'El veterinario registra signos, diagnostico, tratamiento y precio.' },
    { titulo: 'Seguimiento', texto: 'El historial queda disponible para reportes, facturacion y controles.' }
  ];

  instalaciones = [
    { titulo: 'Consultorios', foto: 'https://images.unsplash.com/photo-1581600140682-d4e68c8cde32?w=900', texto: 'Espacios limpios para evaluacion, vacunacion y control preventivo.' },
    { titulo: 'Quirofano', foto: 'https://images.unsplash.com/photo-1579684385127-1ef15d508118?w=900', texto: 'Area preparada para cirugias, anestesia y monitoreo postoperatorio.' },
    { titulo: 'Farmacia y laboratorio', foto: 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=900', texto: 'Inventario de medicamentos, insumos y procesamiento de ordenes clinicas.' }
  ];

  clientes = [
    { nombre: 'Alexander Aguilar', mascotas: 'Bella', uso: 'Consultas y farmacia' },
    { nombre: 'Ana Torres', mascotas: 'Luna, Bella', uso: 'Vacunacion y controles' },
    { nombre: 'Roberto Silva', mascotas: 'Rocky', uso: 'Cirugia y hospitalizacion' },
    { nombre: 'Juan Perez', mascotas: 'Max', uso: 'Consulta, laboratorio y farmacia' }
  ];

  veterinarios: VeterinarioCartelera[] = [
    { nombre: 'Dra. Lucia Carrion', especialidad: 'Medicina interna', foto: 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=700', descripcion: 'Especialista en diagnostico integral, enfermedades cronicas y medicina preventiva.' },
    { nombre: 'Dr. Emilio Torres', especialidad: 'Cirugia', foto: 'https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?w=700', descripcion: 'Cirujano de tejidos blandos, anestesia y recuperacion postoperatoria.' },
    { nombre: 'Dra. Mariana Cedeno', especialidad: 'Dermatologia', foto: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=700', descripcion: 'Atencion de alergias, piel, oidos y tratamiento dermatologico avanzado.' }
  ];

  preguntas = [
    { titulo: 'Puedo registrar mas de una mascota?', texto: 'Si. Cada cliente puede tener una o varias mascotas con historial independiente.' },
    { titulo: 'Se puede descargar el historial?', texto: 'Si. El sistema genera reportes PDF del historial clinico de cada mascota.' },
    { titulo: 'La farmacia actualiza stock?', texto: 'Si. El inventario muestra stock, precio, alertas criticas y reportes Excel.' },
    { titulo: 'Como se aprueba un veterinario o recepcionista?', texto: 'Cuando se registra, queda como solicitud pendiente y el administrador lo aprueba desde el panel.' }
  ];
}
