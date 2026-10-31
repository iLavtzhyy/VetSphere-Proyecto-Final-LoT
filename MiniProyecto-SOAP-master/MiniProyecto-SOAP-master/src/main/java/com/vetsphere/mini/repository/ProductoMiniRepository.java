package com.vetsphere.mini.repository;

import com.vetsphere.mini.domain.ProductoMini;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface ProductoMiniRepository extends JpaRepository<ProductoMini, Long> {
    Optional<ProductoMini> findByCodigo(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductoMini p where p.codigo = :codigo")
    Optional<ProductoMini> findByCodigoParaActualizar(@Param("codigo") String codigo);
}
