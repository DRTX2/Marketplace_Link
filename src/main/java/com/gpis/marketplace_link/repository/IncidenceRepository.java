package com.gpis.marketplace_link.repository;

import com.gpis.marketplace_link.entities.Incidence;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidenceRepository extends JpaRepository<Incidence, Long> {

    Optional<Incidence> findByPublicationIdAndStatusIn(Long publicationId, List<IncidenceStatus> status);

}
