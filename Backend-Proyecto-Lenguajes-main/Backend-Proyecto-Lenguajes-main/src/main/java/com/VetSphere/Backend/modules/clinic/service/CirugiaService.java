package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.modules.clinic.dto.CirugiaRequest;
import com.VetSphere.Backend.modules.clinic.domain.Cirugia;
import com.VetSphere.Backend.modules.clinic.domain.Mascota;
import com.VetSphere.Backend.modules.clinic.repository.CirugiaRepository;
import com.VetSphere.Backend.modules.clinic.repository.MascotaRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CirugiaService {
    private final CirugiaRepository cirugiaRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TarifaService tarifaService;

    public List<Cirugia> listar() {
        return cirugiaRepository.findAll();
    }

    public Cirugia crear(CirugiaRequest request) {
        if (request.mascotaId() == null) {
            throw new BusinessException("Debes indicar la mascota de la cirugia.");
        }
        if (request.cirujanoId() == null) {
            throw new BusinessException("Debes indicar el cirujano de la cirugia.");
        }
        Mascota mascota = mascotaRepository.findById(request.mascotaId()).orElseThrow();
        BigDecimal subtotal = request.precio() != null ? request.precio() : tarifaService.precioCirugia();

        return cirugiaRepository.save(Cirugia.builder()
                .mascota(mascota)
                .cirujano(usuarioRepository.findById(request.cirujanoId()).orElseThrow())
                .fechaProgramada(request.fechaProgramada())
                .procedimiento(request.procedimiento())
                .tipoAnestesia(request.tipoAnestesia())
                .cirujanoPrincipal(request.cirujanoPrincipal())
                .constantesVitales(request.constantesVitales())
                .estadoPostoperatorio(request.estadoPostoperatorio())
                .estado(request.estado())
                .notas(request.notas())
                .precio(subtotal)
                .build());
    }

    public Cirugia actualizar(Long id, CirugiaRequest request) {
        Cirugia cirugia = cirugiaRepository.findById(id).orElseThrow();
        if (request.mascotaId() != null) cirugia.setMascota(mascotaRepository.findById(request.mascotaId()).orElseThrow());
        if (request.cirujanoId() != null) cirugia.setCirujano(usuarioRepository.findById(request.cirujanoId()).orElseThrow());
        cirugia.setFechaProgramada(request.fechaProgramada());
        cirugia.setProcedimiento(request.procedimiento());
        cirugia.setTipoAnestesia(request.tipoAnestesia());
        cirugia.setCirujanoPrincipal(request.cirujanoPrincipal());
        cirugia.setConstantesVitales(request.constantesVitales());
        cirugia.setEstadoPostoperatorio(request.estadoPostoperatorio());
        cirugia.setEstado(request.estado());
        cirugia.setNotas(request.notas());
        if (request.precio() != null) cirugia.setPrecio(request.precio());
        return cirugiaRepository.save(cirugia);
    }

    public void eliminar(Long id) {
        cirugiaRepository.deleteById(id);
    }

    public List<Cirugia> hoy() {
        var inicio = LocalDate.now().atStartOfDay();
        return cirugiaRepository.findByFechaProgramadaBetween(inicio, inicio.plusDays(1).minusSeconds(1));
    }
}
