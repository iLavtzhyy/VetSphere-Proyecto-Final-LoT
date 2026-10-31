package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.domain.Vacunacion;
import com.VetSphere.Backend.modules.clinic.dto.VacunacionRequest;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.modules.clinic.repository.VacunacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Vacunaciones: persistencia real del calendario de vacunacion de la clinica.
 */
@Service
@RequiredArgsConstructor
public class VacunacionService {
    /** Catalogo de estados admitidos para una vacunacion. */
    private static final List<String> ESTADOS_PERMITIDOS = List.of(
            "Pendiente", "Programada", "Aplicada", "Aplicado", "Realizada", "Realizado",
            "Completada", "Completado", "Vencida", "Vencido", "Cancelada", "Cancelado");

    private final VacunacionRepository vacunacionRepository;
    private final MascotaRepository mascotaRepository;

    /** Todas las vacunaciones registradas. */
    public List<Vacunacion> listar() {
        return vacunacionRepository.findAll();
    }

    /**
     * Calendario: las vacunaciones programadas para hoy o hacia adelante
     * (fecha >= hoy), ordenadas de la mas proxima a la mas lejana.
     */
    public List<Vacunacion> calendario() {
        LocalDate hoy = LocalDate.now();
        return vacunacionRepository.findAll().stream()
                .filter(v -> fechaDe(v) != null && !fechaDe(v).isBefore(hoy))
                .sorted(Comparator.comparing(VacunacionService::fechaDe)
                        .thenComparing(Vacunacion::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Registra una vacunacion nueva. */
    @Transactional
    public Vacunacion crear(VacunacionRequest request) {
        if (request.mascotaId() == null) {
            throw new BusinessException("Debes indicar la mascota de la vacunacion.");
        }
        Mascota mascota = mascotaRepository.findById(request.mascotaId())
                .orElseThrow(() -> new BusinessException("La mascota indicada no existe."));
        String vacuna = texto(request.vacuna());
        if (vacuna.isEmpty()) {
            throw new BusinessException("Debes indicar la vacuna aplicada.");
        }
        LocalDate fecha = request.fecha() != null ? request.fecha() : LocalDate.now();
        return vacunacionRepository.save(Vacunacion.builder()
                .mascota(mascota)
                .vacuna(vacuna)
                .fecha(fecha)
                .fechaProxima(request.fechaProxima())
                .lote(texto(request.lote()))
                .estado(estadoInicial(request.estado()))
                .observaciones(request.observaciones())
                .build());
    }

    /** Actualiza una vacunacion existente (solo se modifican los campos enviados). */
    @Transactional
    public Vacunacion actualizar(Long id, VacunacionRequest request) {
        Vacunacion vacunacion = obtener(id);
        if (request.mascotaId() != null) {
            Mascota mascota = mascotaRepository.findById(request.mascotaId())
                    .orElseThrow(() -> new BusinessException("La mascota indicada no existe."));
            vacunacion.setMascota(mascota);
        }
        if (request.vacuna() != null) {
            String vacuna = request.vacuna().trim();
            if (vacuna.isEmpty()) {
                throw new BusinessException("Debes indicar la vacuna aplicada.");
            }
            vacunacion.setVacuna(vacuna);
        }
        if (request.fecha() != null) {
            vacunacion.setFecha(request.fecha());
        }
        if (request.fechaProxima() != null) {
            vacunacion.setFechaProxima(request.fechaProxima());
        }
        if (request.lote() != null) {
            vacunacion.setLote(request.lote().trim());
        }
        if (request.estado() != null && !request.estado().isBlank()) {
            vacunacion.setEstado(estadoValido(request.estado()));
        }
        if (request.observaciones() != null) {
            vacunacion.setObservaciones(request.observaciones());
        }
        return vacunacionRepository.save(vacunacion);
    }

    /** Elimina una vacunacion existente. */
    @Transactional
    public void eliminar(Long id) {
        Vacunacion vacunacion = obtener(id);
        vacunacionRepository.delete(vacunacion);
    }

    private Vacunacion obtener(Long id) {
        return vacunacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacunacion no encontrada: " + id));
    }

    /** Fecha que ordena la vacunacion en el calendario (si no hay fecha, la proxima). */
    private static LocalDate fechaDe(Vacunacion vacunacion) {
        if (vacunacion.getFecha() != null) {
            return vacunacion.getFecha();
        }
        return vacunacion.getFechaProxima();
    }

    /** Estado por defecto cuando el cliente no envia ninguno. */
    private String estadoInicial(String valor) {
        if (valor == null || valor.isBlank()) {
            return "Pendiente";
        }
        return estadoValido(valor);
    }

    /** Valida que el estado pertenezca al catalogo permitido. */
    private String estadoValido(String valor) {
        String limpio = valor.trim();
        boolean permitido = ESTADOS_PERMITIDOS.stream().anyMatch(e -> e.equalsIgnoreCase(limpio));
        if (!permitido) {
            throw new BusinessException("El estado de la vacunacion no es valido. Valores permitidos: "
                    + String.join(", ", ESTADOS_PERMITIDOS) + ".");
        }
        return limpio;
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
