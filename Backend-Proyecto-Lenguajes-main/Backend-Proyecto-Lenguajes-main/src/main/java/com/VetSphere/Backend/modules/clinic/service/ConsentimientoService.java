package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.Cirugia;
import com.VetSphere.Backend.modules.clinic.domain.Consentimiento;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.dto.ConsentimientoRequest;
import com.VetSphere.Backend.modules.clinic.repository.CirugiaRepository;
import com.VetSphere.Backend.modules.clinic.repository.ConsentimientoRepository;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Set;

/**
 * Logica del modulo de consentimientos: los consentimientos se guardan en base
 * de datos y el folio se genera en el servidor con el formato
 * CONS-&lt;anio&gt;-&lt;correlativo de 4 digitos&gt;.
 */
@Service
@RequiredArgsConstructor
public class ConsentimientoService {
    /** Tipos de consentimiento soportados por el modulo. */
    private static final Set<String> TIPOS_VALIDOS = Set.of("Quirurgico", "Tratamiento", "Vacunacion");
    /** Estados de un consentimiento. */
    private static final Set<String> ESTADOS_VALIDOS = Set.of("Firmado", "Pendiente");

    private final ConsentimientoRepository consentimientoRepository;
    private final MascotaRepository mascotaRepository;
    private final CirugiaRepository cirugiaRepository;

    /** Lista los consentimientos con filtros opcionales por mascota y estado. */
    public List<Consentimiento> listar(Long mascotaId, String estado) {
        boolean filtrarMascota = mascotaId != null;
        boolean filtrarEstado = estado != null && !estado.isBlank();
        if (filtrarMascota && filtrarEstado) {
            return consentimientoRepository.findByMascotaIdAndEstadoOrderByFechaDesc(mascotaId, estado.trim());
        }
        if (filtrarMascota) {
            return consentimientoRepository.findByMascotaIdOrderByFechaDesc(mascotaId);
        }
        if (filtrarEstado) {
            return consentimientoRepository.findByEstadoOrderByFechaDesc(estado.trim());
        }
        return consentimientoRepository.findAllByOrderByFechaDesc();
    }

    public Consentimiento obtener(Long id) {
        return consentimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consentimiento no encontrado"));
    }

    /**
     * Registra un consentimiento quirurgico. Mantiene la firma del endpoint
     * historico (POST /api/consentimientos/quirurgico) pero ahora persiste el
     * consentimiento con su folio generado en el servidor.
     */
    public Consentimiento registrarQuirurgico(ConsentimientoRequest request) {
        if (request == null || request.mascotaId() == null) {
            throw new BusinessException("Debes indicar la mascota del consentimiento.");
        }
        String tipo = request.tipo() == null || request.tipo().isBlank()
                ? "Quirurgico" : validarTipo(request.tipo());
        String estado = request.estado() == null || request.estado().isBlank()
                ? "Firmado" : validarEstado(request.estado());
        return guardar(request, tipo, estado);
    }

    /** Registra un consentimiento de cualquier tipo (tratamiento, vacunacion, quirurgico). */
    public Consentimiento crear(ConsentimientoRequest request) {
        if (request == null || request.mascotaId() == null) {
            throw new BusinessException("Debes indicar la mascota del consentimiento.");
        }
        if (request.tipo() == null || request.tipo().isBlank()) {
            throw new BusinessException("Debes indicar el tipo de consentimiento.");
        }
        String tipo = validarTipo(request.tipo());
        String estado = request.estado() == null || request.estado().isBlank()
                ? "Pendiente" : validarEstado(request.estado());
        return guardar(request, tipo, estado);
    }

    public Consentimiento actualizar(Long id, ConsentimientoRequest request) {
        Consentimiento consentimiento = obtener(id);
        if (request.mascotaId() != null) {
            consentimiento.setMascota(mascotaRepository.findById(request.mascotaId())
                    .orElseThrow(() -> new BusinessException("La mascota indicada no existe.")));
        }
        if (request.cirugiaId() != null) {
            consentimiento.setCirugia(cirugiaRepository.findById(request.cirugiaId())
                    .orElseThrow(() -> new BusinessException("La cirugia indicada no existe.")));
        }
        if (request.tipo() != null && !request.tipo().isBlank()) {
            consentimiento.setTipo(validarTipo(request.tipo()));
        }
        if (request.firmadoPor() != null) {
            consentimiento.setFirmadoPor(request.firmadoPor());
        }
        if (request.cargo() != null && !request.cargo().isBlank()) {
            consentimiento.setCargo(request.cargo().trim());
        }
        if (request.fecha() != null) {
            consentimiento.setFecha(request.fecha());
        }
        if (request.estado() != null && !request.estado().isBlank()) {
            consentimiento.setEstado(validarEstado(request.estado()));
        }
        if (request.documento() != null) {
            consentimiento.setDocumento(request.documento());
        }
        if (request.observaciones() != null) {
            consentimiento.setObservaciones(request.observaciones());
        }
        return consentimientoRepository.save(consentimiento);
    }

    public void eliminar(Long id) {
        consentimientoRepository.delete(obtener(id));
    }

    /** Persiste el consentimiento validando la mascota y la cirugia indicadas. */
    private Consentimiento guardar(ConsentimientoRequest request, String tipo, String estado) {
        Mascota mascota = mascotaRepository.findById(request.mascotaId())
                .orElseThrow(() -> new BusinessException("La mascota indicada no existe."));
        Cirugia cirugia = null;
        if (request.cirugiaId() != null) {
            cirugia = cirugiaRepository.findById(request.cirugiaId())
                    .orElseThrow(() -> new BusinessException("La cirugia indicada no existe."));
        }
        return consentimientoRepository.save(Consentimiento.builder()
                .folio(generarFolio())
                .mascota(mascota)
                .cirugia(cirugia)
                .tipo(tipo)
                .firmadoPor(request.firmadoPor())
                .cargo(request.cargo() == null || request.cargo().isBlank() ? "Propietario" : request.cargo().trim())
                .fecha(request.fecha() == null ? LocalDateTime.now() : request.fecha())
                .estado(estado)
                .documento(request.documento())
                .observaciones(request.observaciones())
                .build());
    }

    /**
     * Genera el folio correlativo del ano con el formato CONS-2026-0001.
     * El contador se calcula sobre los consentimientos existentes y se avanza
     * hasta encontrar un folio libre.
     */
    private String generarFolio() {
        String prefijo = "CONS-" + Year.now().getValue() + "-";
        int correlativo = (int) consentimientoRepository.countByFolioStartingWith(prefijo) + 1;
        String folio;
        do {
            folio = String.format("%s%04d", prefijo, correlativo);
            correlativo++;
        } while (consentimientoRepository.existsByFolio(folio));
        return folio;
    }

    /** Normaliza el tipo de consentimiento y rechaza los que no estan soportados. */
    private String validarTipo(String tipo) {
        String candidato = tipo == null ? "" : tipo.trim();
        for (String valido : TIPOS_VALIDOS) {
            if (valido.equalsIgnoreCase(candidato)) {
                return valido;
            }
        }
        throw new BusinessException("El tipo de consentimiento debe ser Quirurgico, Tratamiento o Vacunacion.");
    }

    /** Normaliza el estado del consentimiento y rechaza los que no estan soportados. */
    private String validarEstado(String estado) {
        String candidato = estado == null ? "" : estado.trim();
        for (String valido : ESTADOS_VALIDOS) {
            if (valido.equalsIgnoreCase(candidato)) {
                return valido;
            }
        }
        throw new BusinessException("El estado del consentimiento debe ser Firmado o Pendiente.");
    }
}
