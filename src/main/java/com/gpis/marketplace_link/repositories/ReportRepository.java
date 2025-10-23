package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("""
        SELECT COUNT(r)
        FROM Report r
        WHERE r.reporter.id = :reporterId AND
              r.incidence.publication.id = :publicationId AND
              r.createdAt >= :createdAtAfter
    """)
    int countByReporterIdAndPublicationIdAndCreatedAtAfter(@Param("reporterId") Long reporterId,
                                                           @Param("publicationId") Long publicationId,
                                                           @Param("createdAtAfter") LocalDateTime createdAtAfter);
}
