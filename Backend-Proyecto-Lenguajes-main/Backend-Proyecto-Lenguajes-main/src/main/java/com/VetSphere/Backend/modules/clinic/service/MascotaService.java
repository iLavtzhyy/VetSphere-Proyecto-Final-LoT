package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.modules.clinic.dto.HistorialRequest;
import com.VetSphere.Backend.modules.clinic.dto.MascotaRequest;
import com.VetSphere.Backend.modules.clinic.domain.HistorialClinico;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.repository.HistorialClinicoRepository;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MascotaService {
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialClinicoRepository historialRepository;
    private final FileStorageService fileStorageService;
    private final AuditoriaService auditoriaService;

    public List<Mascota> listar() {
        return mascotaRepository.findAll().stream().filter(m -> !archivada(m)).toList();
    }

    public List<Mascota> listarParaUsuario(Authentication authentication) {
        if (authentication == null) return List.of();
        boolean esCliente = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CLIENTE".equals(a.getAuthority()));
        boolean soloCliente = esCliente && authentication.getAuthorities().size() == 1;
        List<Mascota> mascotas = soloCliente ? mascotaRepository.findByPropietarioEmail(authentication.getName()) : mascotaRepository.findAll();
        return mascotas.stream().filter(m -> !archivada(m)).toList();
    }

    public Mascota obtener(Long id) {
        return mascotaRepository.findById(id).orElseThrow();
    }

    public Mascota obtenerParaUsuario(Long id, Authentication authentication) {
        Mascota mascota = mascotaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        if (esClienteSinPermisosInternos(authentication)
                && (mascota.getPropietario() == null
                || !mascota.getPropietario().getEmail().equalsIgnoreCase(authentication.getName()))) {
            throw new ResourceNotFoundException("Mascota no encontrada");
        }
        return mascota;
    }

    private boolean esClienteSinPermisosInternos(Authentication authentication) {
        if (authentication == null) return false;
        var authorities = authentication.getAuthorities();
        boolean esCliente = authorities.stream().anyMatch(a -> "ROLE_CLIENTE".equals(a.getAuthority()));
        boolean tieneAccesoInterno = authorities.stream().anyMatch(a -> !"ROLE_CLIENTE".equals(a.getAuthority()));
        return esCliente && !tieneAccesoInterno;
    }

    @Transactional
    public Mascota crear(MascotaRequest request, MultipartFile foto) throws IOException {
        if (request.propietarioId() == null) {
            throw new BusinessException("El propietario de la mascota es obligatorio.");
        }
        Mascota mascota = Mascota.builder()
                .nombre(request.nombre())
                .especie(request.especie())
                .raza(request.raza())
                .sexo(request.sexo())
                .color(request.color())
                .pesoKg(request.pesoKg())
                .fechaNacimiento(request.fechaNacimiento())
                .estado(request.estado() == null ? "ACTIVA" : request.estado())
                .propietario(usuarioRepository.findById(request.propietarioId()).orElseThrow())
                .fotoUrl(foto == null || foto.isEmpty() ? null : fileStorageService.guardar(foto))
                .build();
        Mascota guardada = mascotaRepository.save(mascota);
        auditoriaService.registrar(AuditoriaService.MASCOTA_CREADA, "Mascota", guardada.getId(),
                "Mascota " + guardada.getNombre() + " registrada");
        return guardada;
    }

    public Mascota actualizar(Long id, MascotaRequest request) {
        Mascota mascota = obtener(id);
        mascota.setNombre(request.nombre());
        mascota.setEspecie(request.especie());
        mascota.setRaza(request.raza());
        mascota.setSexo(request.sexo());
        mascota.setColor(request.color());
        mascota.setPesoKg(request.pesoKg());
        mascota.setFechaNacimiento(request.fechaNacimiento());
        mascota.setEstado(request.estado() == null ? "ACTIVA" : request.estado());
        if (request.propietarioId() != null) {
            mascota.setPropietario(usuarioRepository.findById(request.propietarioId()).orElseThrow());
        }
        return mascotaRepository.save(mascota);
    }

    @Transactional
    public void eliminar(Long id) {
        Mascota mascota = mascotaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada"));
        String nombre = mascota.getNombre();
        // El historial, las facturas y las hospitalizaciones tienen referencias
        // legales/clinicas a esta ficha. Se archiva en vez de borrar en cascada.
        mascota.setEstado("ARCHIVADA");
        mascotaRepository.save(mascota);
        auditoriaService.registrar(AuditoriaService.MASCOTA_ELIMINADA, "Mascota", id,
                "Mascota " + nombre + " archivada; se conserva su historial clinico");
    }

    private boolean archivada(Mascota mascota) {
        return mascota.getEstado() != null && "ARCHIVADA".equalsIgnoreCase(mascota.getEstado());
    }

    public HistorialClinico agregarHistorial(Long mascotaId, HistorialRequest request, MultipartFile archivo) throws IOException {
        var veterinario = request.veterinarioId() == null
                ? usuarioRepository.findByRolesNombre("ROLE_VETERINARIO").stream().findFirst().orElse(null)
                : usuarioRepository.findById(request.veterinarioId()).orElseThrow();
        var historial = HistorialClinico.builder()
                .mascota(obtener(mascotaId))
                .veterinario(veterinario)
                .fecha(request.fecha() == null ? LocalDateTime.now() : request.fecha())
                .tipoEvento(request.tipoEvento())
                .motivo(request.motivo())
                .diagnostico(request.diagnostico())
                .tratamiento(request.tratamiento())
                .vacunas(request.vacunas())
                .temperatura(request.temperatura())
                .frecuenciaCardiaca(request.frecuenciaCardiaca())
                .frecuenciaRespiratoria(request.frecuenciaRespiratoria())
                .archivosUrl(archivo == null || archivo.isEmpty() ? null : fileStorageService.guardar(archivo))
                .build();
        return historialRepository.save(historial);
    }

    public List<HistorialClinico> historial(Long mascotaId) {
        return historialRepository.findByMascotaIdOrderByFechaDesc(mascotaId);
    }

    public void eliminarArchivoHistorial(Long mascotaId, Long historialId) throws IOException {
        HistorialClinico historial = historialRepository.findById(historialId).orElseThrow();
        if (historial.getMascota() == null || !historial.getMascota().getId().equals(mascotaId)) {
            throw new IllegalArgumentException("El archivo no pertenece a esta mascota");
        }
        fileStorageService.eliminar(historial.getArchivosUrl());
        if ("ARCHIVO MEDICO".equalsIgnoreCase(historial.getTipoEvento())) {
            historialRepository.delete(historial);
            return;
        }
        historial.setArchivosUrl(null);
        historialRepository.save(historial);
    }
}


