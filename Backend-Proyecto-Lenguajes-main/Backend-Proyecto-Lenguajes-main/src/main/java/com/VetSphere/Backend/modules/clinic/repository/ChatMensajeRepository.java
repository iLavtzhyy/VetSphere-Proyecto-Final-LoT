package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.ChatMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Long> {
    List<ChatMensaje> findByConversacion_IdOrderByFechaAscIdAsc(Long conversacionId);
    List<ChatMensaje> findTop200ByConversacion_IdOrderByFechaDescIdDesc(Long conversacionId);
    List<ChatMensaje> findTop24ByConversacion_IdOrderByFechaDescIdDesc(Long conversacionId);
    long countByConversacion_Id(Long conversacionId);
    @Modifying
    @Transactional
    @Query("delete from ChatMensaje m where m.conversacion.id = :conversacionId")
    int deleteByConversacion_Id(@Param("conversacionId") Long conversacionId);

    @Modifying
    @Query("delete from ChatMensaje m where m.fecha < :limite")
    int eliminarAnterioresA(@Param("limite") java.time.LocalDateTime limite);
}
