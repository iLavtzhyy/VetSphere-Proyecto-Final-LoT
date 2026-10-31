package com.VetSphere.Backend.modules.clinic.repository;

import com.VetSphere.Backend.modules.clinic.domain.ChatConversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatConversacionRepository extends JpaRepository<ChatConversacion, Long> {
    @EntityGraph(attributePaths = "usuario")
    Optional<ChatConversacion> findTopByUsuario_IdOrderByActualizadoEnDesc(Long usuarioId);
    @EntityGraph(attributePaths = "usuario")
    Optional<ChatConversacion> findByIdAndUsuario_Id(Long id, Long usuarioId);
    @EntityGraph(attributePaths = "usuario")
    List<ChatConversacion> findAllByOrderByActualizadoEnDesc(Pageable pageable);
    List<ChatConversacion> findAllByUsuario_Id(Long usuarioId);
    void deleteByUsuario_Id(Long usuarioId);
    @EntityGraph(attributePaths = "usuario")
    Optional<ChatConversacion> findById(Long id);

    @Modifying
    @Query("delete from ChatConversacion c where c.id = :conversacionId")
    int eliminarPorId(@Param("conversacionId") Long conversacionId);

    @Modifying
    @Query("delete from ChatConversacion c where c.actualizadoEn < :limite")
    int eliminarAnterioresA(@Param("limite") java.time.LocalDateTime limite);
}
