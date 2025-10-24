package com.gpis.marketplace_link.repositories.lights;

import com.gpis.marketplace_link.entities.lights.IncidenceLight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IncidenceLightRepository extends JpaRepository<IncidenceLight, Long> {

    @Query(value = "SELECT * FROM incidences WHERE id = :id", nativeQuery = true)
    Optional<IncidenceLight> findByIdNative(@Param("id") Long id);

}
