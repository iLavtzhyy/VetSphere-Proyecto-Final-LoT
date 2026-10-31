package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.EstudioImagen;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.dto.EstudioImagenRequest;
import com.VetSphere.Backend.modules.clinic.repository.EstudioImagenRepository;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Logica del modulo de imagenologia: los estudios se guardan en base de datos
 * (antes el controlador devolvia datos de demostracion).
 */
@Service
@RequiredArgsConstructor
public class ImagenologiaService {
    /** Tipos de estudio soportados por el modulo. */
    private static final Set<String> TIPOS_VALIDOS = Set.of("Radiografia", "Ecografia", "TAC", "Resonancia");
    /** Estados del ciclo de vida de un estudio. */
    private static final Set<String> ESTADOS_VALIDOS = Set.of("Solicitado", "Realizado", "Informado");

    private final EstudioImagenRepository estudioImagenRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;

    public List<EstudioImagen> listar() {
        return estudioImagenRepository.findAllByOrderByFechaDesc();
    }

    public List<EstudioImagen> listarPorMascota(Long mascotaId) {
        return estudioImagenRepository.findByMascotaIdOrderByFechaDesc(mascotaId);
    }

    public EstudioImagen obtener(Long id) {
        return estudioImagenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudio de imagenologia no encontrado"));
    }

    public EstudioImagen crear(EstudioImagenRequest request) {
        if (request.mascotaId() == null) {
            throw new BusinessException("Debes indicar la mascota del estudio de imagenologia.");
        }
        Mascota mascota = mascotaRepository.findById(request.mascotaId())
                .orElseThrow(() -> new BusinessException("La mascota indicada no existe."));
        Usuario veterinario = request.veterinarioId() == null ? null
                : usuarioRepository.findById(request.veterinarioId())
                .orElseThrow(() -> new BusinessException("El veterinario indicado no existe."));

        return estudioImagenRepository.save(EstudioImagen.builder()
                .mascota(mascota)
                .tipo(validarTipo(request.tipo()))
                .fecha(request.fecha() == null ? LocalDate.now() : request.fecha())
                .estado(request.estado() == null || request.estado().isBlank()
                        ? "Solicitado" : validarEstado(request.estado()))
                .informe(request.informe())
                .veterinario(veterinario)
                .archivoUrl(request.archivoUrl())
                .build());
    }

    public EstudioImagen actualizar(Long id, EstudioImagenRequest request) {
        EstudioImagen estudio = obtener(id);
        if (request.mascotaId() != null) {
            estudio.setMascota(mascotaRepository.findById(request.mascotaId())
                    .orElseThrow(() -> new BusinessException("La mascota indicada no existe.")));
        }
        if (request.tipo() != null && !request.tipo().isBlank()) {
            estudio.setTipo(validarTipo(request.tipo()));
        }
        if (request.fecha() != null) {
            estudio.setFecha(request.fecha());
        }
        if (request.estado() != null && !request.estado().isBlank()) {
            estudio.setEstado(validarEstado(request.estado()));
        }
        if (request.informe() != null) {
            estudio.setInforme(request.informe());
        }
        if (request.veterinarioId() != null) {
            estudio.setVeterinario(usuarioRepository.findById(request.veterinarioId())
                    .orElseThrow(() -> new BusinessException("El veterinario indicado no existe.")));
        }
        if (request.archivoUrl() != null) {
            estudio.setArchivoUrl(request.archivoUrl());
        }
        return estudioImagenRepository.save(estudio);
    }

    public void eliminar(Long id) {
        estudioImagenRepository.delete(obtener(id));
    }

    /** Normaliza el tipo de estudio y rechaza los que no estan soportados. */
    private String validarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new BusinessException("Debes indicar el tipo de estudio de imagenologia.");
        }
        String candidato = tipo.trim();
        for (String valido : TIPOS_VALIDOS) {
            if (valido.equalsIgnoreCase(candidato)) {
                return valido;
            }
        }
        throw new BusinessException("El tipo de estudio debe ser Radiografia, Ecografia, TAC o Resonancia.");
    }

    /** Normaliza el estado del estudio y rechaza los que no estan soportados. */
    private String validarEstado(String estado) {
        String candidato = estado == null ? "" : estado.trim();
        for (String valido : ESTADOS_VALIDOS) {
            if (valido.equalsIgnoreCase(candidato)) {
                return valido;
            }
        }
        throw new BusinessException("El estado del estudio debe ser Solicitado, Realizado o Informado.");
    }
}
