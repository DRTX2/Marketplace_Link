package com.gpis.marketplace_link.repository;

import com.gpis.marketplace_link.entities.Incidence;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidenceRepository extends JpaRepository<Incidence, Long> {

    Optional<Incidence> findByPublicationIdAndStatusIn(Long publicationId, List<IncidenceStatus> status);

    @Modifying
    @Query("""
    UPDATE Incidence i
    SET i.status = com.gpis.marketplace_link.enums.IncidenceStatus.RESOLVED, i.autoclosed = true
    WHERE i.status = com.gpis.marketplace_link.enums.IncidenceStatus.OPEN AND i.lastReportAt < :cutoff """)
    int bulkAutoClose(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
        SELECT DISTINCT i FROM Incidence i
        JOIN FETCH i.publication p
        JOIN FETCH i.reports r
        JOIN FETCH r.reporter
        WHERE i.status = com.gpis.marketplace_link.enums.IncidenceStatus.OPEN AND
              i.moderator IS NULL AND
              i.decision IS NULL
    """)
    List<Incidence> findAllUnreviewedWithDetails();






}
