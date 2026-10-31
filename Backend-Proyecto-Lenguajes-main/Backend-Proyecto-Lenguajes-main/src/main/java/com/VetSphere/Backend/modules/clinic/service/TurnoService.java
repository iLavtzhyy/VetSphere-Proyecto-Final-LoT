package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.exception.ResourceNotFoundException;
import com.VetSphere.Backend.modules.clinic.domain.Turno;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.dto.TurnoRequest;
import com.VetSphere.Backend.modules.clinic.repository.TurnoRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Gestion de los turnos del personal de la clinica.
 *
 * Reglas de negocio:
 * <ul>
 *   <li>El empleado indicado debe existir en el sistema.</li>
 *   <li>La hora de fin siempre debe ser posterior a la hora de inicio.</li>
 *   <li>Un empleado no puede tener dos turnos que se solapen el mismo dia.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TurnoService {
    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;

    /** Lista los turnos ordenados por fecha y hora de inicio. */
    public List<Turno> listar() {
        return turnoRepository.findAllByOrderByFechaAscHoraInicioAsc();
    }

    public Turno crear(TurnoRequest request) {
        Usuario empleado = obtenerEmpleado(request.empleadoId());
        validarHorario(request);
        validarSolapamiento(empleado.getId(), request.fecha(), request.horaInicio(), request.horaFin(), null);

        return turnoRepository.save(Turno.builder()
                .empleado(empleado)
                .fecha(request.fecha())
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .area(request.area().trim())
                .notas(request.notas())
                .build());
    }

    public Turno actualizar(Long id, TurnoRequest request) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));
        Usuario empleado = obtenerEmpleado(request.empleadoId());
        validarHorario(request);
        validarSolapamiento(empleado.getId(), request.fecha(), request.horaInicio(), request.horaFin(), turno.getId());

        turno.setEmpleado(empleado);
        turno.setFecha(request.fecha());
        turno.setHoraInicio(request.horaInicio());
        turno.setHoraFin(request.horaFin());
        turno.setArea(request.area().trim());
        turno.setNotas(request.notas());
        return turnoRepository.save(turno);
    }

    public void eliminar(Long id) {
        if (!turnoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Turno no encontrado");
        }
        turnoRepository.deleteById(id);
    }

    /** Localiza el usuario asignado al turno. */
    private Usuario obtenerEmpleado(Long empleadoId) {
        if (empleadoId == null) {
            throw new BusinessException("Debes indicar el empleado del turno.");
        }
        return usuarioRepository.findById(empleadoId)
                .orElseThrow(() -> new BusinessException("El empleado indicado no existe en el sistema."));
    }

    /** Comprueba que la franja horaria del turno sea coherente. */
    private void validarHorario(TurnoRequest request) {
        if (request.fecha() == null) {
            throw new BusinessException("Debes indicar la fecha del turno.");
        }
        if (request.horaInicio() == null) {
            throw new BusinessException("Debes indicar la hora de inicio del turno.");
        }
        if (request.horaFin() == null) {
            throw new BusinessException("Debes indicar la hora de fin del turno.");
        }
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new BusinessException("La hora de fin del turno debe ser posterior a la hora de inicio.");
        }
    }

    /**
     * Impide registrar dos turnos solapados para el mismo empleado el mismo dia.
     * Dos turnos se solapan cuando sus franjas horarias se cruzan.
     *
     * @param turnoId turno que se esta actualizando (null al crear): se excluye
     *                de la comprobacion para no compararse consigo mismo.
     */
    private void validarSolapamiento(Long empleadoId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                                     Long turnoId) {
        List<Turno> turnos = turnoRepository.findByEmpleadoIdAndFecha(empleadoId, fecha);
        for (Turno otro : turnos) {
            if (turnoId != null && turnoId.equals(otro.getId())) {
                continue;
            }
            if (otro.getHoraInicio() == null || otro.getHoraFin() == null) {
                continue;
            }
            boolean solapa = horaInicio.isBefore(otro.getHoraFin()) && otro.getHoraInicio().isBefore(horaFin);
            if (solapa) {
                throw new BusinessException("El empleado ya tiene un turno el " + fecha + " de "
                        + otro.getHoraInicio() + " a " + otro.getHoraFin() + ".");
            }
        }
    }
}
