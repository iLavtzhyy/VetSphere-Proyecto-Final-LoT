package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.modules.clinic.dto.DashboardResponse;
import com.VetSphere.Backend.modules.clinic.domain.Producto;
import com.VetSphere.Backend.modules.clinic.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final MascotaRepository mascotaRepository;
    private final HistorialClinicoRepository historialRepository;
    private final CirugiaRepository cirugiaRepository;
    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrdenLaboratorioRepository laboratorioRepository;
    private final HospitalizacionRepository hospitalizacionRepository;
    private final ProductoRepository productoRepository;
    private final ProductoService productoService;

    public DashboardResponse resumen() {
        long totalClientes = usuarioRepository.findByRolesNombre("ROLE_CLIENTE").size();
        long totalMascotas = mascotaRepository.count();
        long mascotasInternadas = mascotaRepository.countByEstado("INTERNADA");

        LocalDateTime hoyInicio = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime hoyFin = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        long citasHoy = citaRepository.findAll().stream()
                .filter(c -> c.getFechaHora() != null && c.getFechaHora().isAfter(hoyInicio) && c.getFechaHora().isBefore(hoyFin))
                .count();

        long citasMes = citaRepository.findAll().stream()
                .filter(c -> c.getFechaHora() != null && c.getFechaHora().getMonth() == LocalDateTime.now().getMonth())
                .count();

        long cirugiasHoy = cirugiaRepository.findByFechaProgramadaBetween(hoyInicio, hoyFin).size();

        long totalConsultas = historialRepository.findAll().stream()
                .filter(h -> "CONSULTA".equalsIgnoreCase(h.getTipoEvento()))
                .count();

        long totalCirugias = cirugiaRepository.count();

        long totalVacunas = historialRepository.findAll().stream()
                .filter(h -> "VACUNA".equalsIgnoreCase(h.getTipoEvento()))
                .count();

        long totalLaboratorio = laboratorioRepository.count();
        long totalProductos = productoRepository.count();
        long totalEmergencias = hospitalizacionRepository.count();

        List<Integer> citasUltimos7Dias = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime diaInicio = LocalDateTime.now().minusDays(i).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime diaFin = LocalDateTime.now().minusDays(i).withHour(23).withMinute(59).withSecond(59);
            long count = citaRepository.findAll().stream()
                    .filter(c -> c.getFechaHora() != null && c.getFechaHora().isAfter(diaInicio) && c.getFechaHora().isBefore(diaFin))
                    .count();
            citasUltimos7Dias.add((int) count);
        }

        long totalPerros = mascotaRepository.findAll().stream()
                .filter(m -> "canino".equalsIgnoreCase(m.getEspecie()) || "perro".equalsIgnoreCase(m.getEspecie()))
                .count();

        long totalGatos = mascotaRepository.findAll().stream()
                .filter(m -> "felino".equalsIgnoreCase(m.getEspecie()) || "gato".equalsIgnoreCase(m.getEspecie()))
                .count();

        List<Map<String, Object>> cirugiasRecientes = cirugiaRepository.findAll().stream()
                .sorted((c1, c2) -> c2.getFechaProgramada().compareTo(c1.getFechaProgramada()))
                .limit(5)
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("procedimiento", c.getProcedimiento());
                    map.put("mascotaNombre", c.getMascota() != null ? c.getMascota().getNombre() : "Paciente");
                    map.put("estado", c.getEstado());
                    return map;
                })
                .toList();

        List<Producto> stockCritico = productoService.stockCritico();

        return new DashboardResponse(
                totalClientes,
                totalMascotas,
                mascotasInternadas,
                citasHoy,
                citasMes,
                cirugiasHoy,
                totalConsultas,
                totalCirugias,
                totalVacunas,
                totalLaboratorio,
                totalProductos,
                totalEmergencias,
                citasUltimos7Dias,
                totalPerros,
                totalGatos,
                cirugiasRecientes,
                stockCritico
        );
    }
}
