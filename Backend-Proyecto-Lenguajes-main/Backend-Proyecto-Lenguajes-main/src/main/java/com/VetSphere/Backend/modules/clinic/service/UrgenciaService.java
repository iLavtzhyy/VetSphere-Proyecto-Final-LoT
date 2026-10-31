package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.domain.Urgencia;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.dto.UrgenciaRequest;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.modules.clinic.repository.UrgenciaRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Urgencias: ingresos por emergencia con su prioridad de atencion y su
 * evolucion (en curso, resuelto o alta hospitalizada).
 */
@Service
@RequiredArgsConstructor
public class UrgenciaService {
    /** Prioridades admitidas para una urgencia. */
    private static final List<String> PRIORIDADES_PERMITIDAS = List.of("Alta", "Media", "Baja");

    /** Estados admitidos para una urgencia. */
    private static final List<String> ESTADOS_PERMITIDOS = List.of(
            "En curso", "Resuelto", "Resuelta", "Alta hospitalizada");

    /** Estado de una urgencia que sigue en atencion. */
    private static final String ESTADO_EN_CURSO = "En curso";

    private final UrgenciaRepository urgenciaRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;

    /** Todas las urgencias registradas. */
    public List<Urgencia> listar() {
        return urgenciaRepository.findAll();
    }

    /**
     * Urgencias activas: las que siguen en curso, ordenadas por hora de ingreso
     * (la mas antigua primero).
     */
    public List<Urgencia> activas() {
        return urgenciaRepository.findAll().stream()
                .filter(u -> normalizar(ESTADO_EN_CURSO).equals(normalizar(u.getEstado())))
                .sorted(Comparator.comparing(Urgencia::getHoraIngreso, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Urgencia::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Registra un ingreso por urgencia. */
    @Transactional
    public Urgencia crear(UrgenciaRequest request) {
        if (request.mascotaId() == null) {
            throw new BusinessException("Debes indicar la mascota de la urgencia.");
        }
        Mascota mascota = mascotaRepository.findById(request.mascotaId())
                .orElseThrow(() -> new BusinessException("La mascota indicada no existe."));
        String motivo = texto(request.motivo());
        if (motivo.isEmpty()) {
            throw new BusinessException("Debes indicar el motivo de la urgencia.");
        }
        LocalDateTime horaIngreso = request.horaIngreso() != null ? request.horaIngreso() : LocalDateTime.now();
        return urgenciaRepository.save(Urgencia.builder()
                .mascota(mascota)
                .horaIngreso(horaIngreso)
                .prioridad(prioridadInicial(request.prioridad()))
                .motivo(motivo)
                .estado(estadoInicial(request.estado()))
                .veterinario(veterinario(request.veterinarioId()))
                .notas(request.notas())
                .build());
    }

    /** Actualiza una urgencia existente (solo se modifican los campos enviados). */
    @Transactional
    public Urgencia actualizar(Long id, UrgenciaRequest request) {
        Urgencia urgencia = obtener(id);
        if (request.mascotaId() != null) {
            Mascota mascota = mascotaRepository.findById(request.mascotaId())
                    .orElseThrow(() -> new BusinessException("La mascota indicada no existe."));
            urgencia.setMascota(mascota);
        }
        if (request.horaIngreso() != null) {
            urgencia.setHoraIngreso(request.horaIngreso());
        }
        if (request.prioridad() != null && !request.prioridad().isBlank()) {
            urgencia.setPrioridad(prioridadValida(request.prioridad()));
        }
        if (request.motivo() != null) {
            String motivo = request.motivo().trim();
            if (motivo.isEmpty()) {
                throw new BusinessException("Debes indicar el motivo de la urgencia.");
            }
            urgencia.setMotivo(motivo);
        }
        if (request.estado() != null && !request.estado().isBlank()) {
            urgencia.setEstado(estadoValido(request.estado()));
        }
        if (request.veterinarioId() != null) {
            urgencia.setVeterinario(veterinario(request.veterinarioId()));
        }
        if (request.notas() != null) {
            urgencia.setNotas(request.notas());
        }
        return urgenciaRepository.save(urgencia);
    }

    /** Elimina una urgencia existente. */
    @Transactional
    public void eliminar(Long id) {
        Urgencia urgencia = obtener(id);
        urgenciaRepository.delete(urgencia);
    }

    private Urgencia obtener(Long id) {
        return urgenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Urgencia no encontrada: " + id));
    }

    /** El veterinario es opcional: si no se indica, la urgencia queda sin responsable. */
    private Usuario veterinario(Long veterinarioId) {
        if (veterinarioId == null) {
            return null;
        }
        return usuarioRepository.findById(veterinarioId)
                .orElseThrow(() -> new BusinessException("El veterinario indicado no existe."));
    }

    /** Prioridad por defecto cuando el cliente no envia ninguna. */
    private String prioridadInicial(String valor) {
        if (valor == null || valor.isBlank()) {
            return "Media";
        }
        return prioridadValida(valor);
    }

    /** Valida que la prioridad pertenezca al catalogo permitido. */
    private String prioridadValida(String valor) {
        String limpio = valor.trim();
        boolean permitida = PRIORIDADES_PERMITIDAS.stream().anyMatch(p -> normalizar(p).equals(normalizar(limpio)));
        if (!permitida) {
            throw new BusinessException("La prioridad de la urgencia no es valida. Valores permitidos: "
                    + String.join(", ", PRIORIDADES_PERMITIDAS) + ".");
        }
        return limpio;
    }

    /** Estado por defecto cuando el cliente no envia ninguno. */
    private String estadoInicial(String valor) {
        if (valor == null || valor.isBlank()) {
            return ESTADO_EN_CURSO;
        }
        return estadoValido(valor);
    }

    /** Valida que el estado pertenezca al catalogo permitido. */
    private String estadoValido(String valor) {
        String limpio = valor.trim();
        boolean permitido = ESTADOS_PERMITIDOS.stream().anyMatch(e -> normalizar(e).equals(normalizar(limpio)));
        if (!permitido) {
            throw new BusinessException("El estado de la urgencia no es valido. Valores permitidos: "
                    + String.join(", ", ESTADOS_PERMITIDOS) + ".");
        }
        return limpio;
    }

    /** Compara sin distinguir mayusculas, espacios, guiones ni guiones bajos. */
    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim().toLowerCase().replace(" ", "").replace("-", "").replace("_", "");
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
