package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.Cita;
import com.VetSphere.Backend.modules.clinic.domain.EtapaFlujoRecepcion;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.dto.CitaRequest;
import com.VetSphere.Backend.modules.clinic.repository.CitaRepository;
import com.VetSphere.Backend.modules.clinic.repository.EtapaFlujoRecepcionRepository;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Reglas de negocio de las citas.
 *
 * El controlador ya no recibe la entidad JPA: el servicio decide que campos se
 * guardan, resuelve las referencias (mascota, veterinario, recepcionista) y
 * rechaza los estados inventados por el cliente.
 */
@Service
@RequiredArgsConstructor
public class CitaService {

    /** Estados admitidos para una cita. */
    public static final Set<String> ESTADOS = Set.of(
            "Pendiente", "Programada", "Confirmada", "En curso", "Completada", "Cancelada");

    private final CitaRepository citaRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EtapaFlujoRecepcionRepository etapaRepository;

    @Transactional(readOnly = true)
    public List<String> estadosDisponibles() {
        List<String> activos = etapaRepository.findByActivaTrueOrderByPosicionAscIdAsc().stream()
                .map(etapa -> etapa.getEstadoCita())
                .toList();
        if (!activos.isEmpty()) return activos;
        return etapaRepository.count() == 0 ? ESTADOS.stream().sorted().toList() : List.of();
    }

    @Transactional(readOnly = true)
    public List<Cita> listar() {
        return citaRepository.findAll();
    }

    /** Crea una cita nueva; el identificador lo asigna el servidor. */
    @Transactional
    public Cita crear(CitaRequest request) {
        Cita cita = new Cita();
        aplicar(cita, request);
        if (cita.getEstado() == null || cita.getEstado().isBlank()) {
            List<EtapaFlujoRecepcion> etapasActivas = etapaRepository.findByActivaTrueOrderByPosicionAscIdAsc();
            if (!etapasActivas.isEmpty()) cita.setEstado(etapasActivas.get(0).getEstadoCita());
            else if (etapaRepository.count() == 0) cita.setEstado("Pendiente");
            else throw new BusinessException("Activa una etapa del flujo antes de crear citas.");
        }
        return citaRepository.save(cita);
    }

    /** Actualiza una cita existente; el identificador viene de la ruta. */
    @Transactional
    public Cita actualizar(Long id, CitaRequest request) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La cita solicitada no existe."));
        aplicar(cita, request);
        return citaRepository.save(cita);
    }

    private void aplicar(Cita cita, CitaRequest request) {
        cita.setMascota(resolverMascota(request.mascota()));
        cita.setVeterinario(resolverUsuario(request.veterinario(), "El veterinario"));
        cita.setRecepcionista(resolverUsuario(request.recepcionista(), "El recepcionista"));
        cita.setFechaHora(request.fechaHora());
        cita.setMotivo(request.motivo() == null ? null : request.motivo().trim());
        if (request.estado() != null && !request.estado().isBlank()) {
            String estado = request.estado().trim();
            List<EtapaFlujoRecepcion> etapasActivas = etapaRepository.findByActivaTrueOrderByPosicionAscIdAsc();
            boolean esEstadoActivo = etapasActivas.stream()
                    .anyMatch(etapa -> etapa.getEstadoCita().equalsIgnoreCase(estado));
            boolean conservaEstadoActual = cita.getEstado() != null && cita.getEstado().equalsIgnoreCase(estado);
            boolean estadoPredeterminadoSinFlujo = etapaRepository.count() == 0 && ESTADOS.contains(estado);
            if (!estadoPredeterminadoSinFlujo && !esEstadoActivo && !conservaEstadoActual) {
                throw new BusinessException("El estado de la cita no es valido. Use uno de: "
                        + String.join(", ", estadosDisponibles()) + ".");
            }
            cita.setEstado(estado);
        }
        cita.setObservaciones(request.observaciones());
    }

    private Mascota resolverMascota(CitaRequest.ReferenciaRequest referencia) {
        if (referencia == null || referencia.id() == null) {
            return null;
        }
        return mascotaRepository.findById(referencia.id())
                .orElseThrow(() -> new ResourceNotFoundException("La mascota indicada no existe."));
    }

    private Usuario resolverUsuario(CitaRequest.ReferenciaRequest referencia, String etiqueta) {
        if (referencia == null || referencia.id() == null) {
            return null;
        }
        return usuarioRepository.findById(referencia.id())
                .orElseThrow(() -> new ResourceNotFoundException(etiqueta + " indicado no existe."));
    }
}
