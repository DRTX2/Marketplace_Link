package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

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

    @Query(value = """
                SELECT r.*
                FROM reports r
                INNER JOIN incidences i ON r.incidence_id = i.id
                WHERE r.reporter_id = :reporterId AND
                      i.publication_id = :publicationId
                ORDER BY r.created_at DESC
                LIMIT 1
            """, nativeQuery = true)
    Optional<Report> findLastReportByReporterIdAndPublicationId(@Param("reporterId") Long reporterId,
            @Param("publicationId") Long publicationId);
}
