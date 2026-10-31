package com.VetSphere.Backend.config;

import com.VetSphere.Backend.modules.clinic.domain.Cirugia;
import com.VetSphere.Backend.modules.clinic.domain.Cita;
import com.VetSphere.Backend.modules.clinic.domain.Consentimiento;
import com.VetSphere.Backend.modules.clinic.domain.Cupon;
import com.VetSphere.Backend.modules.clinic.domain.EstudioImagen;
import com.VetSphere.Backend.modules.clinic.domain.Factura;
import com.VetSphere.Backend.modules.clinic.domain.FacturaDetalle;
import com.VetSphere.Backend.modules.clinic.domain.HistorialClinico;
import com.VetSphere.Backend.modules.clinic.domain.Hospitalizacion;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.domain.OrdenCompra;
import com.VetSphere.Backend.modules.clinic.domain.OrdenLaboratorio;
import com.VetSphere.Backend.modules.clinic.domain.Producto;
import com.VetSphere.Backend.modules.clinic.domain.Proveedor;
import com.VetSphere.Backend.modules.clinic.domain.Rol;
import com.VetSphere.Backend.modules.clinic.domain.Seguimiento;
import com.VetSphere.Backend.modules.clinic.domain.Turno;
import com.VetSphere.Backend.modules.clinic.domain.Urgencia;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.domain.Vacunacion;
import com.VetSphere.Backend.modules.clinic.repository.CirugiaRepository;
import com.VetSphere.Backend.modules.clinic.repository.CitaRepository;
import com.VetSphere.Backend.modules.clinic.repository.ConsentimientoRepository;
import com.VetSphere.Backend.modules.clinic.repository.CuponRepository;
import com.VetSphere.Backend.modules.clinic.repository.EstudioImagenRepository;
import com.VetSphere.Backend.modules.clinic.repository.FacturaDetalleRepository;
import com.VetSphere.Backend.modules.clinic.repository.FacturaRepository;
import com.VetSphere.Backend.modules.clinic.repository.HistorialClinicoRepository;
import com.VetSphere.Backend.modules.clinic.repository.HospitalizacionRepository;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.modules.clinic.repository.OrdenCompraRepository;
import com.VetSphere.Backend.modules.clinic.repository.OrdenLaboratorioRepository;
import com.VetSphere.Backend.modules.clinic.repository.ProductoRepository;
import com.VetSphere.Backend.modules.clinic.repository.ProveedorRepository;
import com.VetSphere.Backend.modules.clinic.repository.RolRepository;
import com.VetSphere.Backend.modules.clinic.repository.SeguimientoRepository;
import com.VetSphere.Backend.modules.clinic.repository.TurnoRepository;
import com.VetSphere.Backend.modules.clinic.repository.UrgenciaRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import com.VetSphere.Backend.modules.clinic.repository.VacunacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@Profile("local")
@RequiredArgsConstructor
public class VetSphereDataSeeder {
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final MascotaRepository mascotaRepository;
    private final HistorialClinicoRepository historialRepository;
    private final ProductoRepository productoRepository;
    private final CitaRepository citaRepository;
    private final CirugiaRepository cirugiaRepository;
    private final OrdenLaboratorioRepository laboratorioRepository;
    private final HospitalizacionRepository hospitalizacionRepository;
    private final FacturaRepository facturaRepository;
    private final FacturaDetalleRepository facturaDetalleRepository;
    private final CuponRepository cuponRepository;
    private final TurnoRepository turnoRepository;
    private final VacunacionRepository vacunacionRepository;
    private final SeguimientoRepository seguimientoRepository;
    private final UrgenciaRepository urgenciaRepository;
    private final EstudioImagenRepository estudioImagenRepository;
    private final ConsentimientoRepository consentimientoRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.password:}")
    private String passwordSemilla;

    /** Propiedad alternativa aceptada por compatibilidad con configuraciones externas. */
    @Value("${app.demo.seed-password:}")
    private String passwordSemillaAlternativa;

    @Bean
    CommandLineRunner cargarDatosVetSphere() {
        return args -> {
            // Se aceptan app.seed.password y app.demo.seed-password; sin ninguna de las dos
            // no se carga el dataset de demostracion (entornos productivos reales).
            if (passwordSemilla == null || passwordSemilla.isBlank()) {
                passwordSemilla = passwordSemillaAlternativa;
            }
            if (passwordSemilla == null || passwordSemilla.isBlank()) {
                return;
            }
            Rol admin = rol("ROLE_ADMIN");
            Rol recepcionista = rol("ROLE_RECEPCIONISTA");
            Rol veterinario = rol("ROLE_VETERINARIO");
            Rol cliente = rol("ROLE_CLIENTE");

            Usuario adminPrincipal = usuario("Admin", "VetSphere", "admin1@vetsphere.com", "0989188046", Set.of(admin), null, null, null);
            Usuario recepcion = usuario("Ana", "Molina", "ana.molina@vetsphere.com", "0989188046", Set.of(recepcionista), null, null, null);
            Usuario draSofia = usuario("Sofia", "Cardenas", "sofia.cardenas@vetsphere.com", "0994521087", Set.of(veterinario), "Medicina interna", "MVZ-EC-2026-018", "Especialista en diagnostico clinico, controles preventivos y medicina felina.");
            Usuario drMateo = usuario("Mateo", "Andrade", "mateo.andrade@vetsphere.com", "0976603412", Set.of(veterinario), "Cirugia veterinaria", "MVZ-EC-2026-044", "Cirujano de tejidos blandos, esterilizaciones y manejo postoperatorio.");
            Usuario draLucia = usuario("Lucia", "Rivera", "lucia.rivera@vetsphere.com", "0963302198", Set.of(veterinario), "Dermatologia y laboratorio", "MVZ-EC-2026-071", "Atencion dermatologica, pruebas clinicas y seguimiento terapeutico.");
            usuario("Valeria", "Mendoza", "valeria.mendoza@vetsphere.com", "0992103456", Set.of(veterinario), "Medicina felina", "MVZ-EC-2026-083", "Medicina preventiva y especializada para gatos, con seguimiento cercano a cada paciente.");
            usuario("Daniel", "Paredes", "daniel.paredes@vetsphere.com", "0987654321", Set.of(veterinario), "Imagenologia", "MVZ-EC-2026-096", "Diagnostico por imagen y apoyo clinico para casos de alta complejidad.");
            usuario("Camila", "Torres", "camila.torres@vetsphere.com", "0971234567", Set.of(veterinario), "Medicina de animales exoticos", "MVZ-EC-2026-107", "Atencion integral de aves, pequenos mamiferos y animales exoticos.");

            Usuario martin = usuario("Martin", "Alvarez", "martin.alvarez@vetsphere.com", "0981112233", Set.of(cliente), null, null, null);
            Usuario carolina = usuario("Carolina", "Paredes", "carolina.paredes@vetsphere.com", "0998887766", Set.of(cliente), null, null, null);
            Usuario sebastian = usuario("Sebastian", "Cruz", "sebastian.cruz@vetsphere.com", "0975556633", Set.of(cliente), null, null, null);
            Usuario fernanda = usuario("Fernanda", "Luna", "fernanda.luna@vetsphere.com", "0962221199", Set.of(cliente), null, null, null);
            Usuario oscar = usuario("Oscar", "Reyes", "oscar.reyes@vetsphere.com", "0957774411", Set.of(cliente), null, null, null);

            List<Mascota> mascotas = cargarMascotas(martin, carolina, sebastian, fernanda, oscar);
            cargarHistorial(mascotas, draSofia, draLucia);
            cargarCirugias(mascotas, drMateo);
            cargarLaboratorio(mascotas, draLucia);
            cargarHospitalizaciones(mascotas, draSofia);
            cargarCitas(mascotas, draSofia, drMateo, draLucia, recepcion);
            cargarFacturas(mascotas);
            cargarFarmacia();
            cargarVentasFarmacia(mascotas);
            cargarCupones();
            cargarModulosOperativos(mascotas, draSofia, drMateo, draLucia, recepcion, adminPrincipal);
        };
    }

    private Rol rol(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre(nombre).build()));
    }

    private Usuario usuario(String nombres, String apellidos, String email, String telefono, Set<Rol> roles,
                            String especialidad, String cedula, String biografia) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(Usuario::new);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(passwordSemilla));
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setTelefono(telefono);
        usuario.setActivo(true);
        usuario.setEmailVerificado(true);
        usuario.setRoles(new HashSet<>(roles));
        usuario.setEspecialidad(especialidad);
        usuario.setCedulaProfesional(cedula);
        usuario.setBiografia(biografia);
        usuario.setFotoUrl(fotoProfesional(nombres));
        return usuarioRepository.save(usuario);
    }

    private String fotoProfesional(String nombre) {
        return switch (nombre) {
            case "Sofia" -> "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=600";
            case "Mateo" -> "https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=600";
            case "Lucia" -> "https://images.unsplash.com/photo-1582750433449-648ed127bb54?w=600";
            case "Valeria" -> "https://images.unsplash.com/photo-1594824476967-48c8b964273f?w=600";
            case "Daniel" -> "https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?w=600";
            case "Camila" -> "https://images.unsplash.com/photo-1551836022-d5d88e9218df?w=600";
            default -> null;
        };
    }

    private List<Mascota> cargarMascotas(Usuario martin, Usuario carolina, Usuario sebastian, Usuario fernanda, Usuario oscar) {
        if (mascotaRepository.count() == 0) {
            mascotaRepository.saveAll(List.of(
                    mascota("Milo", "Canino", "Golden Retriever", "Macho", "Dorado", 28.6, LocalDate.of(2021, 4, 14), martin, "https://images.unsplash.com/photo-1552053831-71594a27632d?w=900"),
                    mascota("Nala", "Felino", "Europeo domestico", "Hembra", "Atigrado", 4.3, LocalDate.of(2022, 9, 10), martin, "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=900"),
                    mascota("Luna", "Felino", "Siames", "Hembra", "Crema", 4.1, LocalDate.of(2020, 1, 6), carolina, "https://images.unsplash.com/photo-1574158622682-e40e69881006?w=900"),
                    mascota("Rocky", "Canino", "Bulldog Frances", "Macho", "Gris", 12.2, LocalDate.of(2019, 7, 21), sebastian, "https://images.unsplash.com/photo-1583511655826-05700d52f4d9?w=900"),
                    mascota("Kiara", "Canino", "Labrador", "Hembra", "Chocolate", 24.8, LocalDate.of(2023, 2, 12), fernanda, "https://images.unsplash.com/photo-1518717758536-85ae29035b6d?w=900"),
                    mascota("Bruno", "Canino", "Schnauzer", "Macho", "Sal y pimienta", 9.5, LocalDate.of(2021, 11, 3), oscar, "https://images.unsplash.com/photo-1507146426996-ef05306b995a?w=900"),
                    mascota("Coco", "Ave", "Periquito australiano", "Macho", "Verde", 0.08, LocalDate.of(2024, 5, 18), fernanda, "https://images.unsplash.com/photo-1552728089-57bdde30beb3?w=900"),
                    mascota("Toby", "Canino", "Beagle", "Macho", "Tricolor", 11.4, LocalDate.of(2020, 8, 30), carolina, "https://images.unsplash.com/photo-1505628346881-b72b27e84530?w=900")
            ));
        }
        return mascotaRepository.findAll();
    }

    private Mascota mascota(String nombre, String especie, String raza, String sexo, String color, Double peso,
                            LocalDate nacimiento, Usuario propietario, String fotoUrl) {
        return Mascota.builder()
                .nombre(nombre)
                .especie(especie)
                .raza(raza)
                .sexo(sexo)
                .color(color)
                .pesoKg(peso)
                .fechaNacimiento(nacimiento)
                .propietario(propietario)
                .fotoUrl(fotoUrl)
                .estado("ACTIVA")
                .build();
    }

    private void cargarHistorial(List<Mascota> mascotas, Usuario draSofia, Usuario draLucia) {
        if (historialRepository.count() > 0 || mascotas.isEmpty()) return;
        for (int i = 0; i < mascotas.size(); i++) {
            Mascota mascota = mascotas.get(i);
            historialRepository.save(HistorialClinico.builder()
                    .mascota(mascota)
                    .veterinario(i % 2 == 0 ? draSofia : draLucia)
                    .fecha(LocalDateTime.now().minusDays(10 + i))
                    .tipoEvento(i % 3 == 0 ? "VACUNA" : "CONSULTA")
                    .motivo(i % 3 == 0 ? "Vacunacion anual y control preventivo" : "Consulta general por control clinico")
                    .diagnostico("Paciente estable, mucosas normales y signos vitales dentro de parametros esperados.")
                    .tratamiento("Plan preventivo, hidratacion, antiparasitario mensual y seguimiento en 30 dias.")
                    .vacunas(i % 3 == 0 ? "Multiple canina/felina segun especie" : "")
                    .temperatura(38.2 + (i % 4) * 0.1)
                    .frecuenciaCardiaca(86 + i)
                    .frecuenciaRespiratoria(22 + i % 5)
                    .build());
        }
    }

    private void cargarCirugias(List<Mascota> mascotas, Usuario drMateo) {
        if (cirugiaRepository.count() > 0 || mascotas.size() < 4) return;
        cirugiaRepository.saveAll(List.of(
                Cirugia.builder().mascota(mascotas.get(2)).cirujano(drMateo).fechaProgramada(LocalDateTime.now().plusDays(2).withHour(9)).procedimiento("Esterilizacion programada").tipoAnestesia("General inhalatoria").cirujanoPrincipal("Dr. Mateo Andrade").constantesVitales("FC 92, FR 24, SpO2 98%").estadoPostoperatorio("Preparacion prequirurgica completa").estado("PROGRAMADA").notas("Paciente en ayuno y examenes prequirurgicos revisados.").build(),
                Cirugia.builder().mascota(mascotas.get(3)).cirujano(drMateo).fechaProgramada(LocalDateTime.now().minusDays(5).withHour(10)).procedimiento("Limpieza dental y extraccion simple").tipoAnestesia("General").cirujanoPrincipal("Dr. Mateo Andrade").constantesVitales("FC 90, FR 21, SpO2 99%").estadoPostoperatorio("Alta el mismo dia sin complicaciones").estado("FINALIZADA").notas("Control odontologico en 15 dias.").build()
        ));
    }

    private void cargarLaboratorio(List<Mascota> mascotas, Usuario draLucia) {
        if (laboratorioRepository.count() > 0 || mascotas.size() < 3) return;
        laboratorioRepository.saveAll(List.of(
                OrdenLaboratorio.builder().mascota(mascotas.get(0)).veterinarioSolicitante(draLucia).fecha(LocalDateTime.now().minusDays(1)).tipoPrueba("Hemograma completo").resultado("Leucocitos dentro de rango, hematocrito normal, plaquetas adecuadas.").estado("COMPLETADO").build(),
                OrdenLaboratorio.builder().mascota(mascotas.get(1)).veterinarioSolicitante(draLucia).fecha(LocalDateTime.now()).tipoPrueba("Perfil hepatico").resultado("Muestra procesandose en laboratorio clinico.").estado("EN_PROCESO").build(),
                OrdenLaboratorio.builder().mascota(mascotas.get(2)).veterinarioSolicitante(draLucia).fecha(LocalDateTime.now().plusHours(3)).tipoPrueba("Citologia dermatologica").resultado("Pendiente de toma de muestra.").estado("PENDIENTE").build()
        ));
    }

    private void cargarHospitalizaciones(List<Mascota> mascotas, Usuario draSofia) {
        if (hospitalizacionRepository.count() > 0 || mascotas.size() < 5) return;
        hospitalizacionRepository.save(Hospitalizacion.builder()
                .mascota(mascotas.get(4))
                .veterinarioResponsable(draSofia)
                .ingreso(LocalDateTime.now().minusHours(8))
                .jaula("H-03")
                .motivo("Observacion por gastroenteritis aguda")
                .planCuidados("Fluidoterapia, control de temperatura, dieta blanda y monitoreo cada 4 horas.")
                .estado("EN_OBSERVACION")
                .build());
    }

    private void cargarCitas(List<Mascota> mascotas, Usuario draSofia, Usuario drMateo, Usuario draLucia, Usuario recepcion) {
        if (citaRepository.count() > 0 || mascotas.size() < 5) return;
        citaRepository.saveAll(List.of(
                cita(mascotas.get(0), draSofia, recepcion, "Consulta preventiva", "Confirmada", LocalDateTime.now().plusHours(2)),
                cita(mascotas.get(1), draLucia, recepcion, "Control dermatologico", "Programada", LocalDateTime.now().plusHours(4)),
                cita(mascotas.get(2), draLucia, recepcion, "Vacunacion anual", "Confirmada", LocalDateTime.now().plusDays(1).withHour(11)),
                cita(mascotas.get(3), drMateo, recepcion, "Revision postoperatoria", "Pendiente", LocalDateTime.now().plusDays(1).withHour(15)),
                cita(mascotas.get(4), draSofia, recepcion, "Seguimiento hospitalizacion", "En triage", LocalDateTime.now().plusDays(2).withHour(10))
        ));
    }

    private Cita cita(Mascota mascota, Usuario veterinario, Usuario recepcionista, String motivo, String estado, LocalDateTime fecha) {
        return Cita.builder()
                .mascota(mascota)
                .veterinario(veterinario)
                .recepcionista(recepcionista)
                .motivo(motivo)
                .estado(estado)
                .fechaHora(fecha)
                .observaciones("Agendada desde recepcion VetSphere.")
                .build();
    }

    private void cargarFacturas(List<Mascota> mascotas) {
        if (facturaRepository.count() > 0 || mascotas.isEmpty()) return;
        int limite = Math.min(8, mascotas.size());
        for (int i = 0; i < limite; i++) {
            Mascota mascota = mascotas.get(i);
            BigDecimal subtotal = BigDecimal.valueOf(35 + (i * 18L));
            facturaRepository.save(Factura.builder()
                    .numero("FAC-2026-" + String.format("%04d", i + 1))
                    .fecha(LocalDateTime.now().minusDays(i))
                    .cliente(mascota.getPropietario())
                    .mascota(mascota)
                    .concepto((i % 2 == 0 ? "Consulta clinica - " : "Farmacia y tratamiento - ") + mascota.getNombre())
                    .subtotal(subtotal)
                    .impuestos(subtotal.multiply(BigDecimal.valueOf(0.15)))
                    .total(subtotal.multiply(BigDecimal.valueOf(1.12)))
                    .estadoPago(i % 3 == 0 ? "Pendiente" : "Pagado")
                    .metodoPago(i % 3 == 0 ? null : (i % 2 == 0 ? "Efectivo" : "Tarjeta"))
                    .build());
        }
    }

    private void cargarFarmacia() {
        String[] categorias = {"Antibioticos", "Antiparasitarios", "Vacunas", "Analgesicos", "Dermatologia", "Cardiologia", "Gastrointestinal", "Nutricion", "Insumos", "Laboratorio"};
        String[] nombres = {
                "Amoxicilina clavulanica", "Cefalexina", "Doxiciclina", "Ciprofloxacina otica", "Metronidazol",
                "Ivermectina", "Praziquantel", "Pipeta antipulgas", "Collar antiparasitario", "Albendazol",
                "Vacuna multiple canina", "Vacuna triple felina", "Vacuna rabia", "Vacuna bordetella", "Vacuna leucemia felina",
                "Meloxicam", "Carprofeno", "Tramadol gotas", "Dipirona", "Lidocaina unidad",
                "Shampoo clorhexidina", "Crema cicatrizante", "Spray antiseptico", "Otico limpiador", "Prednisolona",
                "Enalapril", "Furosemida", "Pimobendan", "Atenolol", "Suplemento omega 3",
                "Omeprazol", "Suero oral", "Probiótico veterinario", "Antiemetico", "Protectores gastricos",
                "Alimento renal", "Alimento gastrointestinal", "Leche cachorro", "Vitaminas A-D-E", "Suplemento articular",
                "Jeringa 3ml", "Cateter intravenoso", "Guantes nitrilo", "Gasas esteriles", "Venda cohesiva",
                "Tubo EDTA", "Reactivo hematologia", "Tiras uroanalisis", "Portaobjetos", "Hisopos esteriles"
        };
        if (productoRepository.count() >= 50) return;
        for (int i = 0; i < 100; i++) {
            String codigo = "MED-" + String.format("%03d", i + 1);
            if (productoRepository.findByCodigo(codigo).isPresent()) continue;
            String nombreBase = nombres[i % nombres.length];
            String categoria = categorias[i % categorias.length];
            int stockCritico = 8 + (i % 9);
            int stock = stockCritico + 6 + (i % 35);
            if (i % 13 == 0) stock = Math.max(1, stockCritico - 2);
            productoRepository.save(Producto.builder()
                    .codigo(codigo)
                    .nombre(nombreBase + " " + presentacion(i))
                    .categoria(categoria)
                    .stock(stock)
                    .stockCritico(stockCritico)
                    .precio(BigDecimal.valueOf(4.50 + (i % 24) * 2.35))
                    .prioridadAlta(i % 13 == 0 || i % 7 == 0)
                    .build());
        }
    }

    private String presentacion(int indice) {
        return switch (indice % 5) {
            case 0 -> "10ml";
            case 1 -> "250mg";
            case 2 -> "unidad";
            case 3 -> "caja";
            default -> "frasco";
        };
    }

    /**
     * Ventas de farmacia historicas (con detalle de producto persistido).
     *
     * Solo una parte del catalogo registra ventas: asi el analisis de rotacion
     * distingue productos con salida reciente, productos con baja rotacion y
     * productos que nunca vendieron, que son los que requieren promocion.
     */
    private void cargarVentasFarmacia(List<Mascota> mascotas) {
        if (facturaDetalleRepository.count() > 0) return;
        List<Producto> productos = productoRepository.findAll();
        if (productos.isEmpty() || mascotas.isEmpty()) return;

        int[] diasAtras = {2, 4, 6, 9, 12, 15, 18, 22, 48, 55, 62, 70, 85, 95, 110, 120, 130, 140};
        int conVentas = Math.min(diasAtras.length, productos.size());
        for (int i = 0; i < conVentas; i++) {
            Producto producto = productos.get(i);
            LocalDateTime fecha = LocalDateTime.now().minusDays(diasAtras[i]).withHour(10).withMinute(0);
            int cantidad = 1 + (i % 4);
            BigDecimal precio = producto.getPrecio() == null ? BigDecimal.valueOf(10) : producto.getPrecio();
            precio = precio.setScale(2, RoundingMode.HALF_UP);
            BigDecimal bruto = precio.multiply(BigDecimal.valueOf(cantidad));
            BigDecimal descuento = producto.descuentoEfectivo()
                    .multiply(precio).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = bruto.subtract(descuento);
            BigDecimal impuestos = subtotal.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
            Mascota mascota = mascotas.get(i % mascotas.size());

            Factura factura = facturaRepository.save(Factura.builder()
                    .numero("FAC-" + LocalDateTime.now().getYear() + "-F" + String.format("%04d", i + 1))
                    .fecha(fecha)
                    .cliente(mascota.getPropietario())
                    .mascota(mascota)
                    .concepto("Venta de farmacia - " + producto.getNombre())
                    .subtotal(subtotal)
                    .impuestos(impuestos)
                    .total(subtotal.add(impuestos))
                    .descuento(descuento)
                    .estadoPago("Pagado")
                    .metodoPago(i % 2 == 0 ? "Efectivo" : "Tarjeta")
                    .build());

            facturaDetalleRepository.save(FacturaDetalle.builder()
                    .factura(factura)
                    .producto(producto)
                    .descripcion(producto.getNombre())
                    .cantidad(cantidad)
                    .precioUnitario(precio)
                    .descuento(descuento)
                    .subtotal(subtotal)
                    .build());
        }
    }

    /** Cupones de ejemplo para que la pantalla de promociones tenga contenido. */
    private void cargarCupones() {
        if (cuponRepository.count() > 0) return;
        List<Producto> productos = productoRepository.findAll();
        LocalDateTime inicio = LocalDateTime.now();

        cuponRepository.save(Cupon.builder()
                .codigo("PROMO-BIENVENIDA")
                .descripcion("Descuento de bienvenida para la primera compra de farmacia")
                .tipoDescuento(Cupon.PORCENTAJE)
                .valor(new BigDecimal("10.00"))
                .fechaInicio(inicio)
                .fechaFin(inicio.plusDays(60))
                .usosMaximos(200)
                .usosActuales(0)
                .activo(true)
                .origen(Cupon.ORIGEN_MANUAL)
                .creadoPor("sistema")
                .fechaCreacion(inicio)
                .build());

        if (!productos.isEmpty()) {
            Producto producto = productos.get(productos.size() - 1);
            cuponRepository.save(Cupon.builder()
                    .codigo("PROMO-" + producto.getCodigo())
                    .descripcion("Promocion de salida rapida para " + producto.getNombre())
                    .tipoDescuento(Cupon.PORCENTAJE)
                    .valor(new BigDecimal("15.00"))
                    .producto(producto)
                    .categoria(producto.getCategoria())
                    .minimoSubtotal(new BigDecimal("10.00"))
                    .fechaInicio(inicio)
                    .fechaFin(inicio.plusDays(30))
                    .usosMaximos(Math.max(1, producto.getStock() == null ? 1 : producto.getStock()))
                    .usosActuales(0)
                    .activo(true)
                    .origen(Cupon.ORIGEN_MANUAL)
                    .creadoPor("sistema")
                    .fechaCreacion(inicio)
                    .build());
        }
    }

    /**
     * Datos de ejemplo de los modulos que antes devolvian inventos:
     * turnos del personal, calendario de vacunacion, seguimientos, urgencias,
     * estudios de imagenologia, consentimientos, proveedores y ordenes de
     * compra.
     */
    private void cargarModulosOperativos(List<Mascota> mascotas, Usuario draSofia, Usuario drMateo,
                                          Usuario draLucia, Usuario recepcion, Usuario admin) {
        if (mascotas.isEmpty()) return;
        LocalDate hoy = LocalDate.now();
        int anio = hoy.getYear();

        // ---- Proveedores -------------------------------------------------
        List<Proveedor> proveedores = proveedorRepository.findAll();
        if (proveedores.isEmpty()) {
            proveedores = proveedorRepository.saveAll(List.of(
                    Proveedor.builder()
                            .nombre("Distribuidora Vet Supply")
                            .contacto("Patricia Gomez")
                            .telefono("0991122334")
                            .email("ventas@distribuidoravetsupply.com")
                            .direccion("Av. de las Americas 120")
                            .activo(true)
                            .build(),
                    Proveedor.builder()
                            .nombre("Laboratorios Andinos")
                            .contacto("Diego Fernandez")
                            .telefono("0985566778")
                            .email("compras@laboratoriosandinos.com")
                            .direccion("Calle Los Alpes 45")
                            .activo(true)
                            .build()));
        }
        Proveedor proveedor = proveedores.get(0);

        // ---- Turnos del personal -----------------------------------------
        if (turnoRepository.count() == 0) {
            turnoRepository.saveAll(List.of(
                    Turno.builder()
                            .empleado(draSofia)
                            .fecha(hoy)
                            .horaInicio(LocalTime.of(8, 0))
                            .horaFin(LocalTime.of(14, 0))
                            .area("Consulta")
                            .notas("Consulta general y controles preventivos")
                            .build(),
                    Turno.builder()
                            .empleado(drMateo)
                            .fecha(hoy)
                            .horaInicio(LocalTime.of(8, 0))
                            .horaFin(LocalTime.of(16, 0))
                            .area("Cirugia")
                            .notas("Bloque quirurgico de la manana")
                            .build(),
                    Turno.builder()
                            .empleado(recepcion)
                            .fecha(hoy)
                            .horaInicio(LocalTime.of(7, 0))
                            .horaFin(LocalTime.of(15, 0))
                            .area("Recepcion")
                            .notas("Agenda, cobros y entrega de llaves")
                            .build(),
                    Turno.builder()
                            .empleado(draLucia)
                            .fecha(hoy.plusDays(1))
                            .horaInicio(LocalTime.of(9, 0))
                            .horaFin(LocalTime.of(17, 0))
                            .area("Laboratorio")
                            .notas("Toma de muestras y dermatologia")
                            .build()));
        }

        // ---- Calendario de vacunacion -----------------------------------
        if (vacunacionRepository.count() == 0) {
            Mascota milo = mascotas.get(0);
            Mascota nala = mascotas.get(1);
            Mascota luna = mascotas.get(2);
            vacunacionRepository.saveAll(List.of(
                    Vacunacion.builder()
                            .mascota(milo)
                            .vacuna("Vacuna antirrabica")
                            .fecha(hoy.minusDays(40))
                            .fechaProxima(hoy.plusDays(325))
                            .lote("RB-2026-014")
                            .estado("Aplicada")
                            .observaciones("Sin reacciones adversas observadas")
                            .build(),
                    Vacunacion.builder()
                            .mascota(nala)
                            .vacuna("Triples felina")
                            .fecha(hoy.plusDays(12))
                            .lote("TR-2026-088")
                            .estado("Programada")
                            .observaciones("Se avisa al propietario por correo")
                            .build(),
                    Vacunacion.builder()
                            .mascota(luna)
                            .vacuna("Rabia")
                            .fecha(hoy.minusDays(120))
                            .fechaProxima(hoy.plusDays(245))
                            .lote("RB-2025-201")
                            .estado("Vencida")
                            .observaciones("Pendiente de reprogramar")
                            .build()));
        }

        // ---- Seguimientos -------------------------------------------------
        if (seguimientoRepository.count() == 0) {
            seguimientoRepository.saveAll(List.of(
                    Seguimiento.builder()
                            .mascota(mascotas.get(0))
                            .fecha(hoy.plusDays(3))
                            .indicacion("Control de suturas a los 10 dias de la cirugia")
                            .tipo("Postoperatorio")
                            .estado("Pendiente")
                            .veterinario(drMateo)
                            .observaciones("Revisar ausencia de infeccion en la incision")
                            .build(),
                    Seguimiento.builder()
                            .mascota(mascotas.get(2))
                            .fecha(hoy.plusDays(20))
                            .indicacion("Control de peso y ajuste de dosis")
                            .tipo("Control")
                            .estado("Pendiente")
                            .veterinario(draLucia)
                            .build()));
        }

        // ---- Urgencias ----------------------------------------------------
        if (urgenciaRepository.count() == 0) {
            urgenciaRepository.saveAll(List.of(
                    Urgencia.builder()
                            .mascota(mascotas.get(3))
                            .horaIngreso(LocalDateTime.now().minusHours(2))
                            .prioridad("Alta")
                            .motivo("Dificultad respiratoria y lengua azulada")
                            .estado("En curso")
                            .veterinario(draSofia)
                            .notas("Estabilizacion con oxigeno; se solicita radiografia")
                            .build(),
                    Urgencia.builder()
                            .mascota(mascotas.get(4))
                            .horaIngreso(LocalDateTime.now().minusDays(3))
                            .prioridad("Media")
                            .motivo("Cojera posterior a salida al parque")
                            .estado("Resuelto")
                            .veterinario(draLucia)
                            .notas("Esguince leve; reposo 5 dias y control en 7 dias")
                            .build()));
        }

        // ---- Imagenologia ---------------------------------------------------
        if (estudioImagenRepository.count() == 0) {
            estudioImagenRepository.saveAll(List.of(
                    EstudioImagen.builder()
                            .mascota(mascotas.get(3))
                            .tipo("Radiografia")
                            .fecha(hoy)
                            .estado("Informado")
                            .informe("Sin alteraciones osteoarticulares relevantes")
                            .veterinario(draSofia)
                            .build(),
                    EstudioImagen.builder()
                            .mascota(mascotas.get(4))
                            .tipo("Ecografia")
                            .fecha(hoy.minusDays(1))
                            .estado("Realizado")
                            .informe("Ecografia abdominal; informe pendiente de redactar")
                            .veterinario(draLucia)
                            .build()));
        }

        // ---- Consentimientos --------------------------------------------------
        if (consentimientoRepository.count() == 0) {
            consentimientoRepository.saveAll(List.of(
                    Consentimiento.builder()
                            .folio("CONS-" + anio + "-0001")
                            .mascota(mascotas.get(0))
                            .tipo("Quirurgico")
                            .firmadoPor("Martin Alvarez")
                            .cargo("Propietario")
                            .fecha(LocalDateTime.now().minusDays(12))
                            .estado("Firmado")
                            .documento("Autorizo la intervencion quirurgica y los controles postoperatorios")
                            .build(),
                    Consentimiento.builder()
                            .folio("CONS-" + anio + "-0002")
                            .mascota(mascotas.get(2))
                            .tipo("Vacunacion")
                            .firmadoPor("Carolina Paredes")
                            .cargo("Propietaria")
                            .fecha(LocalDateTime.now().minusDays(30))
                            .estado("Pendiente")
                            .observaciones("Firma pendiente en la proxima visita")
                            .build()));
        }

        // ---- Ordenes de compra ------------------------------------------------
        if (ordenCompraRepository.count() == 0) {
            ordenCompraRepository.saveAll(List.of(
                    OrdenCompra.builder()
                            .numero("OC-" + anio + "-0001")
                            .proveedor(proveedor)
                            .fecha(hoy.minusDays(5))
                            .estado(OrdenCompra.SOLICITADA)
                            .total(new BigDecimal("486.90"))
                            .responsable(admin)
                            .detalle("Fresas, suturas y guantes esteriles")
                            .observaciones("Entrega prevista en 5 dias habiles")
                            .build(),
                    OrdenCompra.builder()
                            .numero("OC-" + anio + "-0002")
                            .proveedor(proveedor)
                            .fecha(hoy.minusDays(20))
                            .estado(OrdenCompra.RECIBIDA)
                            .total(new BigDecimal("1240.00"))
                            .responsable(admin)
                            .detalle("Reactivos de laboratorio y jeringas")
                            .observaciones("Recibida y verificada por farmacia")
                            .build()));
        }
    }
}
