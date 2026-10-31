package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.CodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {
    Optional<CodigoVerificacion> findTopByEmailAndCodigoAndUsadoFalseOrderByIdDesc(String email, String codigo);

    /** Busca un codigo validando ademas el proposito para el que fue emitido. */
    Optional<CodigoVerificacion> findTopByEmailAndTipoAndCodigoAndUsadoFalseOrderByIdDesc(String email, String tipo, String codigo);

    /** Codigos vigentes (sin usar) emitidos para un correo y proposito en la ventana indicada. */
    long countByEmailAndTipoAndUsadoFalseAndExpiraEnAfter(String email, String tipo, LocalDateTime desde);

    /** Invalida los codigos previos del mismo correo y proposito. */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update CodigoVerificacion c set c.usado = true where c.email = :email and c.tipo = :tipo and c.usado = false")
    void invalidar(String email, String tipo);
}
