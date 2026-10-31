package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.modules.clinic.dto.UsuarioRequest;
import com.VetSphere.Backend.exception.BusinessException;
import com.VetSphere.Backend.modules.clinic.domain.Rol;
import com.VetSphere.Backend.modules.clinic.domain.Usuario;
import com.VetSphere.Backend.modules.clinic.domain.SolicitudRegistro;
import com.VetSphere.Backend.modules.clinic.repository.RolRepository;
import com.VetSphere.Backend.modules.clinic.repository.UsuarioRepository;
import com.VetSphere.Backend.modules.clinic.repository.SolicitudRegistroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SolicitudRegistroRepository solicitudRegistroRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final CorreoService correoService;
    private final ChatMemoriaService chatMemoriaService;
    private final EntityManager entityManager;

    public List<Usuario> listar() { return usuarioRepository.findAll(); }

    public List<Usuario> solicitudesPendientes() {
        return solicitudRegistroRepository.findAll().stream()
                .map(sol -> {
                    Rol rol = rolRepository.findByNombre(sol.getRolSolicitado()).orElse(null);
                    return Usuario.builder()
                            .id(sol.getId())
                            .nombres(sol.getNombres())
                            .apellidos(sol.getApellidos())
                            .email(sol.getEmail())
                            .telefono(sol.getTelefono())
                            .activo(false)
                            .roles(rol != null ? Set.of(rol) : Set.of())
                            .build();
                })
                .toList();
    }

    @Transactional
    public Usuario aprobar(Long id) {
        SolicitudRegistro sol = solicitudRegistroRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada."));
        Rol rol = rolRepository.findByNombre(sol.getRolSolicitado())
                .orElseThrow(() -> new BusinessException("Rol no encontrado."));

        Usuario usuario = usuarioRepository.findByEmail(sol.getEmail())
                .orElseGet(Usuario::new);

        usuario.setNombres(sol.getNombres());
        usuario.setApellidos(sol.getApellidos());
        usuario.setEmail(sol.getEmail());
        usuario.setTelefono(sol.getTelefono());
        usuario.setPassword(sol.getPassword());
        usuario.setActivo(true);
        // Coleccion mutable: Hibernate no puede vaciar Set.of(...) al hacer merge
        usuario.setRoles(new HashSet<>(Set.of(rol)));

        Usuario saved = usuarioRepository.save(usuario);
        solicitudRegistroRepository.delete(sol);
        auditoriaService.registrar(AuditoriaService.USUARIO_APROBADO, sol.getEmail(),
                "SolicitudRegistro", id, "Solicitud de " + sol.getRolSolicitado() + " aprobada");
        return saved;
    }

    @Transactional
    public void rechazar(Long id) {
        SolicitudRegistro sol = solicitudRegistroRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Solicitud no encontrada."));
        solicitudRegistroRepository.delete(sol);
        auditoriaService.registrar(AuditoriaService.USUARIO_RECHAZADO, sol.getEmail(),
                "SolicitudRegistro", id, "Solicitud de " + sol.getRolSolicitado() + " rechazada");
    }

    public List<Usuario> veterinarios() {
        return usuarioRepository.findByRolesNombre("ROLE_VETERINARIO");
    }

    @Transactional
    public Usuario crear(UsuarioRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("La contrasena del usuario es obligatoria.");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new BusinessException("El correo del usuario es obligatorio.");
        }
        String email = AuthService.normalizar(request.email());
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Ya existe un usuario con ese correo.");
        }
        var roles = new HashSet<Rol>();
        if (request.roles() != null) {
            request.roles().forEach(nombre -> roles.add(rolRepository.findByNombre(nombre)
                    .orElseThrow(() -> new BusinessException("Rol no valido: " + nombre))));
        }
        return usuarioRepository.save(Usuario.builder()
                .nombres(request.nombres())
                .apellidos(request.apellidos())
                .email(email)
                .telefono(request.telefono())
                .password(passwordEncoder.encode(request.password()))
                .fotoUrl(request.fotoUrl())
                .especialidad(request.especialidad())
                .cedulaProfesional(request.cedulaProfesional())
                .biografia(request.biografia())
                .activo(request.activo() != null ? request.activo() : true)
                .emailVerificado(true)
                .intentosFallidos(0)
                .roles(roles)
                .build());
    }

    @Transactional
    public Usuario actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado."));
        if (request.nombres() != null) usuario.setNombres(request.nombres());
        if (request.apellidos() != null) usuario.setApellidos(request.apellidos());
        if (request.email() != null && !request.email().isBlank()) {
            String email = AuthService.normalizar(request.email());
            usuarioRepository.findByEmail(email)
                    .filter(otro -> !otro.getId().equals(usuario.getId()))
                    .ifPresent(otro -> { throw new BusinessException("Ya existe un usuario con ese correo."); });
            usuario.setEmail(email);
        }
        if (request.telefono() != null) usuario.setTelefono(request.telefono());
        if (request.fotoUrl() != null) usuario.setFotoUrl(request.fotoUrl());
        if (request.especialidad() != null) usuario.setEspecialidad(request.especialidad());
        if (request.cedulaProfesional() != null) usuario.setCedulaProfesional(request.cedulaProfesional());
        if (request.biografia() != null) usuario.setBiografia(request.biografia());
        if (request.activo() != null) {
            usuario.setActivo(request.activo());
            if (!request.activo()) {
                usuario.setDesactivadoEn(LocalDateTime.now());
            } else {
                usuario.setDesactivadoEn(null);
            }
        }
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.password()));
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuario.setMotivoBloqueo(null);
        }
        if (request.roles() != null && !request.roles().isEmpty()) {
            var roles = new HashSet<Rol>();
            request.roles().forEach(nombre -> roles.add(rolRepository.findByNombre(nombre)
                    .orElseThrow(() -> new BusinessException("Rol no valido: " + nombre))));
            usuario.setRoles(roles);
        }
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizarRoles(Long id, Set<String> nombresRoles) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado."));
        var roles = new HashSet<Rol>();
        nombresRoles.forEach(nombre -> roles.add(rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new BusinessException("Rol no valido: " + nombre))));
        usuario.setRoles(roles);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado."));
        if (esPropio(id)) {
            throw new BusinessException("No puedes eliminar tu propia cuenta de administracion.");
        }
        // Desvincula referencias clínicas y contables para poder borrar la cuenta
        // sin borrar historiales, citas ni comprobantes asociados.
        String[] referencias = {
                "update Mascota x set x.propietario = null where x.propietario.id = :id",
                "update Cita x set x.veterinario = null where x.veterinario.id = :id",
                "update Cita x set x.recepcionista = null where x.recepcionista.id = :id",
                "update Cirugia x set x.cirujano = null where x.cirujano.id = :id",
                "update EstudioImagen x set x.veterinario = null where x.veterinario.id = :id",
                "update Factura x set x.cliente = null where x.cliente.id = :id",
                "update HistorialClinico x set x.veterinario = null where x.veterinario.id = :id",
                "update Hospitalizacion x set x.veterinarioResponsable = null where x.veterinarioResponsable.id = :id",
                "update OrdenLaboratorio x set x.veterinarioSolicitante = null where x.veterinarioSolicitante.id = :id",
                "update OrdenCompra x set x.responsable = null where x.responsable.id = :id",
                "update Seguimiento x set x.veterinario = null where x.veterinario.id = :id",
                "update Turno x set x.empleado = null where x.empleado.id = :id",
                "update Urgencia x set x.veterinario = null where x.veterinario.id = :id"
        };
        for (String jpql : referencias) {
            entityManager.createQuery(jpql).setParameter("id", id).executeUpdate();
        }
        solicitudRegistroRepository.findByEmail(usuario.getEmail())
                .ifPresent(solicitudRegistroRepository::delete);
        chatMemoriaService.eliminarHistorialUsuario(id);
        entityManager.flush();
        usuarioRepository.delete(usuario);
    }

    // ------------------------------------------------------------------
    // Bloqueo y estado de la cuenta (acciones del administrador)
    // ------------------------------------------------------------------

    /** Bloquea manualmente la cuenta del usuario durante las horas indicadas. */
    @Transactional
    public Usuario bloquear(Long id, Integer horas, String motivo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado."));
        int duracion = horas == null || horas <= 0 ? 24 : horas;
        if (esPropio(id)) {
            throw new BusinessException("No puedes bloquear tu propia cuenta.");
        }
        usuario.setBloqueadoHasta(LocalDateTime.now().plusHours(duracion));
        usuario.setIntentosFallidos(0);
        usuario.setMotivoBloqueo(motivo == null || motivo.isBlank()
                ? "Bloqueado por el administrador" : motivo.trim());
        Usuario guardado = usuarioRepository.save(usuario);
        auditoriaService.registrar(AuditoriaService.CUENTA_BLOQUEADA, usuario.getEmail(),
                "Usuario", id, "Cuenta bloqueada por el administrador hasta " + usuario.getBloqueadoHasta()
                        + ". Motivo: " + usuario.getMotivoBloqueo());
        correoService.enviarAvisoAdministrativo(usuario.getEmail(),
                "Cuenta bloqueada por " + duracion + " horas. Motivo: " + usuario.getMotivoBloqueo());
        return guardado;
    }

    /** Levanta el bloqueo manual o automatico y reinicia el contador de intentos. */
    @Transactional
    public Usuario desbloquear(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado."));
        usuario.setBloqueadoHasta(null);
        usuario.setIntentosFallidos(0);
        usuario.setMotivoBloqueo(null);
        Usuario guardado = usuarioRepository.save(usuario);
        auditoriaService.registrar(AuditoriaService.CUENTA_DESBLOQUEADA, usuario.getEmail(),
                "Usuario", id, "Bloqueo levantado por el administrador");
        return guardado;
    }

    /** Desactiva la cuenta (deja de poder iniciar sesion) sin eliminarla. */
    @Transactional
    public Usuario desactivar(Long id, String motivo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado."));
        if (esPropio(id)) {
            throw new BusinessException("No puedes desactivar tu propia cuenta desde la administracion.");
        }
        usuario.setActivo(false);
        usuario.setDesactivadoEn(LocalDateTime.now());
        Usuario guardado = usuarioRepository.save(usuario);
        auditoriaService.registrar(AuditoriaService.CUENTA_DESACTIVADA, usuario.getEmail(),
                "Usuario", id, "Cuenta desactivada por el administrador. Motivo: "
                        + (motivo == null || motivo.isBlank() ? "No especificado" : motivo));
        correoService.enviarAvisoAdministrativo(usuario.getEmail(),
                "Cuenta desactivada. Motivo: " + (motivo == null || motivo.isBlank() ? "No especificado" : motivo));
        return guardado;
    }

    /** Reactiva una cuenta desactivada. */
    @Transactional
    public Usuario reactivar(Long id, String motivo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado."));
        usuario.setActivo(true);
        usuario.setDesactivadoEn(null);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setMotivoBloqueo(null);
        Usuario guardado = usuarioRepository.save(usuario);
        auditoriaService.registrar(AuditoriaService.CUENTA_REACTIVADA, usuario.getEmail(),
                "Usuario", id, "Cuenta reactivada por el administrador. Motivo: "
                        + (motivo == null || motivo.isBlank() ? "No especificado" : motivo));
        return guardado;
    }

    /** true si el id corresponde al usuario que esta ejecutando la operacion. */
    private boolean esPropio(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return false;
        }
        return usuarioRepository.findByEmail(authentication.getName())
                .map(u -> u.getId().equals(id))
                .orElse(false);
    }
}
