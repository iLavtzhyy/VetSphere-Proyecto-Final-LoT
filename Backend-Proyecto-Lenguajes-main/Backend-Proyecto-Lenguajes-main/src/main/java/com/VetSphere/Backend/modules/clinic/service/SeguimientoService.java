package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.domain.Seguimiento;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.dto.SeguimientoRequest;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.modules.clinic.repository.SeguimientoRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Seguimientos clinicos (postoperatorios, controles y vacunaciones) con
 * persistencia real sobre la mascota atendida.
 */
@Service
@RequiredArgsConstructor
public class SeguimientoService {
    /** Catalogo de tipos de seguimiento admitidos. */
    private static final List<String> TIPOS_PERMITIDOS = List.of(
            "Postoperatorio", "Control", "Vacunacion", "Consulta", "Desparasitacion", "Analisis", "Revision");

    /** Catalogo de estados de seguimiento admitidos. */
    private static final List<String> ESTADOS_PERMITIDOS = List.of(
            "Pendiente", "Realizado", "Realizada", "Cancelado", "Cancelada", "Atrasado", "Atrasada");

    /** Tipo de seguimiento postoperatorio. */
    private static final String TIPO_POSTOPERATORIO = "Postoperatorio";

    /** Estado de un seguimiento que aun no se realiza. */
    private static final String ESTADO_PENDIENTE = "Pendiente";

    private final SeguimientoRepository seguimientoRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;

    /** Todos los seguimientos registrados. */
    public List<Seguimiento> listar() {
        return seguimientoRepository.findAll();
    }

    /**
     * Seguimientos postoperatorios: los de tipo postoperatorio o los que siguen
     * pendientes, ordenados por fecha (del mas antiguo al mas reciente).
     */
    public List<Seguimiento> postoperatorios() {
        return seguimientoRepository.findAll().stream()
                .filter(s -> esPostoperatorio(s) || esPendiente(s))
                .sorted(Comparator.comparing(Seguimiento::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Seguimiento::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Registra un seguimiento nuevo. */
    @Transactional
    public Seguimiento crear(SeguimientoRequest request) {
        if (request.mascotaId() == null) {
            throw new BusinessException("Debes indicar la mascota del seguimiento.");
        }
        Mascota mascota = mascotaRepository.findById(request.mascotaId())
                .orElseThrow(() -> new BusinessException("La mascota indicada no existe."));
        String indicacion = texto(request.indicacion());
        if (indicacion.isEmpty()) {
            throw new BusinessException("Debes indicar la indicacion del seguimiento.");
        }
        LocalDate fecha = request.fecha() != null ? request.fecha() : LocalDate.now();
        return seguimientoRepository.save(Seguimiento.builder()
                .mascota(mascota)
                .fecha(fecha)
                .indicacion(indicacion)
                .tipo(tipoInicial(request.tipo()))
                .estado(estadoInicial(request.estado()))
                .veterinario(veterinario(request.veterinarioId()))
                .observaciones(request.observaciones())
                .build());
    }

    /** Actualiza un seguimiento existente (solo se modifican los campos enviados). */
    @Transactional
    public Seguimiento actualizar(Long id, SeguimientoRequest request) {
        Seguimiento seguimiento = obtener(id);
        if (request.mascotaId() != null) {
            Mascota mascota = mascotaRepository.findById(request.mascotaId())
                    .orElseThrow(() -> new BusinessException("La mascota indicada no existe."));
            seguimiento.setMascota(mascota);
        }
        if (request.indicacion() != null) {
            String indicacion = request.indicacion().trim();
            if (indicacion.isEmpty()) {
                throw new BusinessException("Debes indicar la indicacion del seguimiento.");
            }
            seguimiento.setIndicacion(indicacion);
        }
        if (request.fecha() != null) {
            seguimiento.setFecha(request.fecha());
        }
        if (request.tipo() != null && !request.tipo().isBlank()) {
            seguimiento.setTipo(tipoValido(request.tipo()));
        }
        if (request.estado() != null && !request.estado().isBlank()) {
            seguimiento.setEstado(estadoValido(request.estado()));
        }
        if (request.veterinarioId() != null) {
            seguimiento.setVeterinario(veterinario(request.veterinarioId()));
        }
        if (request.observaciones() != null) {
            seguimiento.setObservaciones(request.observaciones());
        }
        return seguimientoRepository.save(seguimiento);
    }

    /** Elimina un seguimiento existente. */
    @Transactional
    public void eliminar(Long id) {
        Seguimiento seguimiento = obtener(id);
        seguimientoRepository.delete(seguimiento);
    }

    private Seguimiento obtener(Long id) {
        return seguimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seguimiento no encontrado: " + id));
    }

    /** El veterinario es opcional: si no se indica, el seguimiento queda sin responsable. */
    private Usuario veterinario(Long veterinarioId) {
        if (veterinarioId == null) {
            return null;
        }
        return usuarioRepository.findById(veterinarioId)
                .orElseThrow(() -> new BusinessException("El veterinario indicado no existe."));
    }

    private boolean esPostoperatorio(Seguimiento seguimiento) {
        return normalizar(TIPO_POSTOPERATORIO).equals(normalizar(seguimiento.getTipo()));
    }

    private boolean esPendiente(Seguimiento seguimiento) {
        return normalizar(ESTADO_PENDIENTE).equals(normalizar(seguimiento.getEstado()));
    }

    /** Tipo por defecto cuando el cliente no envia ninguno. */
    private String tipoInicial(String valor) {
        if (valor == null || valor.isBlank()) {
            return TIPO_POSTOPERATORIO;
        }
        return tipoValido(valor);
    }

    /** Valida que el tipo pertenezca al catalogo permitido. */
    private String tipoValido(String valor) {
        String limpio = valor.trim();
        boolean permitido = TIPOS_PERMITIDOS.stream().anyMatch(t -> normalizar(t).equals(normalizar(limpio)));
        if (!permitido) {
            throw new BusinessException("El tipo de seguimiento no es valido. Valores permitidos: "
                    + String.join(", ", TIPOS_PERMITIDOS) + ".");
        }
        return limpio;
    }

    /** Estado por defecto cuando el cliente no envia ninguno. */
    private String estadoInicial(String valor) {
        if (valor == null || valor.isBlank()) {
            return ESTADO_PENDIENTE;
        }
        return estadoValido(valor);
    }

    /** Valida que el estado pertenezca al catalogo permitido. */
    private String estadoValido(String valor) {
        String limpio = valor.trim();
        boolean permitido = ESTADOS_PERMITIDOS.stream().anyMatch(e -> normalizar(e).equals(normalizar(limpio)));
        if (!permitido) {
            throw new BusinessException("El estado del seguimiento no es valido. Valores permitidos: "
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
