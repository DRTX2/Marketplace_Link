package com.gpis.marketplace_link.repositories.lights;

import com.gpis.marketplace_link.entities.lights.PublicationLight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PublicationLightRepository extends JpaRepository<PublicationLight, Long> {

    @Query(value = "SELECT * FROM publications WHERE id = :id", nativeQuery = true)
    Optional<PublicationLight> findByIdNative(@Param("id") Long id);

}
