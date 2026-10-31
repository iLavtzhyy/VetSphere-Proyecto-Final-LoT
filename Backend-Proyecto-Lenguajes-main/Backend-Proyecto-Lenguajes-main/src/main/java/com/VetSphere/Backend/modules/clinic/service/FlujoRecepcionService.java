package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.Cita;
import com.VetSphere.Backend.modules.clinic.domain.EtapaFlujoRecepcion;
import com.VetSphere.Backend.modules.clinic.dto.EtapaFlujoRecepcionRequest;
import com.VetSphere.Backend.modules.clinic.repository.CitaRepository;
import com.VetSphere.Backend.modules.clinic.repository.EtapaFlujoRecepcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FlujoRecepcionService {
    private final EtapaFlujoRecepcionRepository etapaRepository;
    private final CitaRepository citaRepository;

    @Transactional
    public List<EtapaFlujoRecepcion> listar() {
        if (etapaRepository.count() == 0) {
            inicializarFlujo();
        }
        return etapaRepository.findAllByOrderByPosicionAscIdAsc();
    }

    @Transactional
    public EtapaFlujoRecepcion crear(EtapaFlujoRecepcionRequest request) {
        List<EtapaFlujoRecepcion> etapas = listar();
        if (etapas.size() >= 50) {
            throw new BusinessException("El flujo admite hasta 50 etapas.");
        }
        validarUnicos(request, null);
        int posicionSolicitada = request.posicion() == null ? etapas.size() + 1 : request.posicion();
        int posicion = Math.max(1, Math.min(posicionSolicitada, etapas.size() + 1));
        desplazarDesde(posicion, 1);
        return etapaRepository.save(EtapaFlujoRecepcion.builder()
                .nombre(request.nombre().trim())
                .estadoCita(request.estadoCita().trim())
                .descripcion(limpiar(request.descripcion()))
                .color(request.color())
                .posicion(posicion)
                .activa(request.activa() == null || request.activa())
                .build());
    }

    @Transactional
    public EtapaFlujoRecepcion actualizar(Long id, EtapaFlujoRecepcionRequest request) {
        listar();
        EtapaFlujoRecepcion etapa = etapaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La etapa del flujo no existe."));
        if (Boolean.FALSE.equals(request.activa()) && etapa.isActiva()
                && etapaRepository.findByActivaTrueOrderByPosicionAscIdAsc().size() <= 1) {
            throw new BusinessException("El flujo debe conservar al menos una etapa activa.");
        }
        validarUnicos(request, id);
        String estadoAnterior = etapa.getEstadoCita();
        String estadoNuevo = request.estadoCita().trim();
        if (!estadoAnterior.equalsIgnoreCase(estadoNuevo)) {
            for (Cita cita : citaRepository.findAll()) {
                if (cita.getEstado() != null && cita.getEstado().equalsIgnoreCase(estadoAnterior)) {
                    cita.setEstado(estadoNuevo);
                }
            }
        }
        etapa.setNombre(request.nombre().trim());
        etapa.setEstadoCita(estadoNuevo);
        etapa.setDescripcion(limpiar(request.descripcion()));
        etapa.setColor(request.color());
        etapa.setActiva(request.activa() == null || request.activa());
        if (request.posicion() != null && !request.posicion().equals(etapa.getPosicion())) {
            mover(etapa, request.posicion());
        }
        return etapaRepository.save(etapa);
    }

    @Transactional
    public EtapaFlujoRecepcion eliminar(Long id) {
        EtapaFlujoRecepcion etapa = etapaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("La etapa del flujo no existe."));
        if (etapaRepository.findByActivaTrueOrderByPosicionAscIdAsc().size() <= 1 && etapa.isActiva()) {
            throw new BusinessException("El flujo debe conservar al menos una etapa activa.");
        }
        if (citaRepository.existsByEstadoIgnoreCase(etapa.getEstadoCita())) {
            throw new BusinessException("No puedes eliminar esta etapa porque hay citas con ese estado. Puedes desactivarla.");
        }
        etapaRepository.delete(etapa);
        normalizarPosiciones();
        return etapa;
    }

    @Transactional(readOnly = true)
    public List<String> estadosActivos() {
        return etapaRepository.findByActivaTrueOrderByPosicionAscIdAsc().stream()
                .map(EtapaFlujoRecepcion::getEstadoCita)
                .toList();
    }

    private void inicializarFlujo() {
        guardarPredeterminada("Pendiente", "Solicitud recibida", "slate", 1);
        guardarPredeterminada("Programada", "Cita reservada en agenda", "cyan", 2);
        guardarPredeterminada("Confirmada", "El cliente confirmo la asistencia", "emerald", 3);
        guardarPredeterminada("En espera", "Paciente recibido en clinica", "amber", 4);
        guardarPredeterminada("En triage", "Evaluacion inicial y prioridad", "violet", 5);
        guardarPredeterminada("En curso", "Atencion veterinaria en curso", "blue", 6);
        guardarPredeterminada("Completada", "Atencion finalizada", "emerald", 7);
        guardarPredeterminada("Cancelada", "Cita cancelada", "rose", 8);
    }

    private void guardarPredeterminada(String estado, String descripcion, String color, int posicion) {
        etapaRepository.save(EtapaFlujoRecepcion.builder()
                .nombre(estado)
                .estadoCita(estado)
                .descripcion(descripcion)
                .color(color)
                .posicion(posicion)
                .activa(true)
                .build());
    }

    private void validarUnicos(EtapaFlujoRecepcionRequest request, Long id) {
        String nombre = request.nombre().trim();
        String estado = request.estadoCita().trim();
        etapaRepository.findByNombreIgnoreCase(nombre).ifPresent(otra -> {
            if (!otra.getId().equals(id)) {
                throw new BusinessException("Ya existe una etapa con ese nombre.");
            }
        });
        etapaRepository.findByEstadoCitaIgnoreCase(estado).ifPresent(otra -> {
            if (!otra.getId().equals(id)) {
                throw new BusinessException("Ese estado de cita ya esta asignado a otra etapa.");
            }
        });
    }

    private void mover(EtapaFlujoRecepcion etapa, int nuevaPosicion) {
        List<EtapaFlujoRecepcion> etapas = etapaRepository.findAllByOrderByPosicionAscIdAsc();
        int destino = Math.max(1, Math.min(nuevaPosicion, etapas.size()));
        int origen = etapa.getPosicion();
        for (EtapaFlujoRecepcion otra : etapas) {
            if (otra.getId().equals(etapa.getId())) continue;
            if (origen < destino && otra.getPosicion() > origen && otra.getPosicion() <= destino) {
                otra.setPosicion(otra.getPosicion() - 1);
                etapaRepository.save(otra);
            } else if (destino < origen && otra.getPosicion() >= destino && otra.getPosicion() < origen) {
                otra.setPosicion(otra.getPosicion() + 1);
                etapaRepository.save(otra);
            }
        }
        etapa.setPosicion(destino);
    }

    private void desplazarDesde(int posicion, int diferencia) {
        etapaRepository.findAllByOrderByPosicionAscIdAsc().stream()
                .filter(etapa -> etapa.getPosicion() >= posicion)
                .forEach(etapa -> {
                    etapa.setPosicion(etapa.getPosicion() + diferencia);
                    etapaRepository.save(etapa);
                });
    }

    private void normalizarPosiciones() {
        List<EtapaFlujoRecepcion> etapas = etapaRepository.findAllByOrderByPosicionAscIdAsc();
        for (int i = 0; i < etapas.size(); i++) {
            etapas.get(i).setPosicion(i + 1);
            etapaRepository.save(etapas.get(i));
        }
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
