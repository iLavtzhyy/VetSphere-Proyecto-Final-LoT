import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { fadeInUp } from '../../shared/animations/fade-in-up';
import { IconoComponent } from '../../shared/components/icono/icono';

type CriterioCalidad = { nombre: string; evidencia: string; medida: string; estado: string };

@Component({
  selector: 'app-documentacion',
  standalone: true,
  imports: [CommonModule, IconoComponent],
  animations: [fadeInUp],
  templateUrl: './documentacion.html',
  styleUrl: './documentacion.css'
})
export class DocumentacionComponent {
  criteriosCalidad: CriterioCalidad[] = [
    { nombre: 'Adecuación funcional', evidencia: 'Flujos de acceso, pacientes, agenda, atención y administración.', medida: 'Casos aprobados / casos ejecutados por módulo.', estado: 'Medir' },
    { nombre: 'Eficiencia de desempeño', evidencia: 'Interfaz web, API y consultas a SQL Server.', medida: 'Tiempo p95, solicitudes por segundo y uso de memoria.', estado: 'Pendiente' },
    { nombre: 'Compatibilidad', evidencia: 'Frontend y backend se comunican mediante HTTP/JSON en el entorno local.', medida: 'Matriz de navegadores, resoluciones y servicios integrados.', estado: 'Medir' },
    { nombre: 'Capacidad de interacción', evidencia: 'Diseño adaptable, formularios y navegación diferenciada por perfil.', medida: 'Tareas completadas, errores y evaluación con usuarios.', estado: 'Mejorar' },
    { nombre: 'Fiabilidad', evidencia: 'Persistencia en SQL Server y validación de entradas del servidor.', medida: 'Tasa de errores, disponibilidad y restauraciones exitosas.', estado: 'Pendiente' },
    { nombre: 'Seguridad', evidencia: 'Inicio de sesión, contraseñas BCrypt, JWT y permisos en endpoints.', medida: 'Pruebas de acceso permitido y denegado por rol.', estado: 'Revisar' },
    { nombre: 'Mantenibilidad', evidencia: 'Separación de módulos Angular, controladores, servicios y repositorios.', medida: 'Cobertura de pruebas, defectos y tiempo de cambio.', estado: 'Mejorar' },
    { nombre: 'Flexibilidad', evidencia: 'Configuración local por variables de entorno y componentes web modulares.', medida: 'Instalaciones reproducibles en distintos entornos.', estado: 'Medir' },
    { nombre: 'Seguridad operacional', evidencia: 'La aplicación apoya decisiones clínicas; requiere validación del personal veterinario.', medida: 'Revisión clínica, trazabilidad y análisis de riesgos de uso.', estado: 'Pendiente' }
  ];

  imprimir() {
    window.print();
  }
}
