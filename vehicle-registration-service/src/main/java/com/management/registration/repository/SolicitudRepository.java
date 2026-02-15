package com.management.registration.repository;

import com.management.registration.entity.Solicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, UUID> {

    boolean existsByPatente(String patente);
    Optional<Solicitud> findByPatente(String patente);
    Page<Solicitud> findAll(Pageable pageable);
    @Query("SELECT s FROM Solicitud s WHERE s.estado = :estado")
    Page<Solicitud> findByEstado(@Param("estado") String estado, Pageable pageable);

}
