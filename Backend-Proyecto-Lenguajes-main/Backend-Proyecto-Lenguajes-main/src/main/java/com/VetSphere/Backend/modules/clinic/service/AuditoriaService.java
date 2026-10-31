package com.VetSphere.Backend.modules.clinic.service;

import com.VetSphere.Backend.modules.clinic.domain.AuditorEvento;
import com.VetSphere.Backend.modules.clinic.repository.AuditorEventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro real de eventos de auditoria en base de datos.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {
    /** Acciones registradas (se mantienen estables para poder filtrar/consultar). */
    public static final String LOGIN_EXITOSO = "LOGIN_EXITOSO";
    public static final String LOGIN_FALLIDO = "LOGIN_FALLIDO";
    public static final String LOGIN_BLOQUEADO = "LOGIN_BLOQUEADO";
    public static final String MASCOTA_CREADA = "MASCOTA_CREADA";
    public static final String MASCOTA_ELIMINADA = "MASCOTA_ELIMINADA";
    public static final String FACTURA_EMITIDA = "FACTURA_EMITIDA";
    public static final String STOCK_MODIFICADO = "STOCK_MODIFICADO";
    public static final String USUARIO_APROBADO = "USUARIO_APROBADO";
    public static final String USUARIO_RECHAZADO = "USUARIO_RECHAZADO";
    public static final String EMAIL_VERIFICADO = "EMAIL_VERIFICADO";
    public static final String PASSWORD_RESTABLECIDA = "PASSWORD_RESTABLECIDA";
    public static final String CUENTA_BLOQUEADA = "CUENTA_BLOQUEADA";
    public static final String CUENTA_DESBLOQUEADA = "CUENTA_DESBLOQUEADA";
    public static final String CUENTA_DESACTIVADA = "CUENTA_DESACTIVADA";
    public static final String CUENTA_REACTIVADA = "CUENTA_REACTIVADA";
    public static final String PRODUCTO_CREADO = "PRODUCTO_CREADO";
    public static final String PRODUCTO_ACTUALIZADO = "PRODUCTO_ACTUALIZADO";
    public static final String CUPON_CREADO = "CUPON_CREADO";
    public static final String CUPON_CANJEADO = "CUPON_CANJEADO";

    private final AuditorEventoRepository auditorEventoRepository;

    @Transactional
    public void registrar(String accion, String entidad, Long entidadId, String detalle) {
        registrar(accion, usuarioActual(), entidad, entidadId, detalle);
    }

    @Transactional
    public void registrar(String accion, String usuarioEmail, String entidad, Long entidadId, String detalle) {
        auditorEventoRepository.save(AuditorEvento.builder()
                .fecha(java.time.LocalDateTime.now())
                .usuarioEmail(usuarioEmail == null || usuarioEmail.isBlank() ? "sistema" : usuarioEmail)
                .accion(accion)
                .entidad(entidad)
                .entidadId(entidadId)
                .detalle(detalle == null || detalle.length() <= 2000 ? detalle : detalle.substring(0, 2000))
                .build());
    }

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }
}
