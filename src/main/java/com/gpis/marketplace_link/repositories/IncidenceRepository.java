package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.Incidence;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para el manejo de entidades {@link Incidence}.
 *
 * Provee operaciones de persistencia y consultas personalizadas
 * relacionadas con las incidencias reportadas en el sistema.
 *
 * @author Josué
 * @since 1.0
 */
public interface IncidenceRepository extends JpaRepository<Incidence, Long> {

    /**
     * Busca una incidencia asociada a una publicación específica que se encuentre en alguno de los estados indicados.
     *
     * Este método se utiliza principalmente para verificar si ya existe una incidencia
     * activa (por ejemplo, con estado {@code OPEN}, {@code UNDER_REVIEW} o {@code APPEALED})
     * antes de crear una nueva.
     *
     * @param publicationId el ID de la publicación.
     * @param status lista de estados válidos para filtrar la búsqueda.
     * @return un {@link Optional} que contiene la incidencia si existe, o vacío si no se encontró.
     */
    Optional<Incidence> findByPublicationIdAndStatusIn(Long publicationId, List<IncidenceStatus> status);

    @Modifying
    @Query(value = """
        UPDATE incidences i
        SET i.status = 'RESOLVED',
            i.auto_closed = true
        WHERE i.status = 'OPEN'
        AND (
            SELECT MAX(r.created_at)
            FROM reports r
            WHERE r.incidence_id = i.id
        ) < :cutoff
    """, nativeQuery = true)
    int bulkAutoClose(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Obtiene todas las incidencias no revisadas con sus detalles completos.
     *
     * Devuelve incidencias en estado {@code OPEN} que no han sido reclamadas por un moderador
     * y que no tienen una decisión registrada. La consulta utiliza {@code JOIN FETCH}
     * para cargar eficientemente la publicación y los reportes asociados, incluyendo
     * los reportadores correspondientes.
     *
     * @return lista de incidencias abiertas pendientes de revisión, con datos de publicación y reportes.
     */
    @Query(
            value = """
        SELECT DISTINCT i FROM Incidence i
        JOIN FETCH i.publication p
        JOIN FETCH i.reports r
        JOIN FETCH r.reporter
        WHERE i.status IN (
            com.gpis.marketplace_link.enums.IncidenceStatus.OPEN,
            com.gpis.marketplace_link.enums.IncidenceStatus.UNDER_REVIEW
        )
        AND i.moderator IS NULL
        AND i.decision IS NULL
    """,
            countQuery = """
        SELECT COUNT(i) FROM Incidence i
        WHERE i.status IN (
            com.gpis.marketplace_link.enums.IncidenceStatus.OPEN,
            com.gpis.marketplace_link.enums.IncidenceStatus.UNDER_REVIEW
        )
        AND i.moderator IS NULL
        AND i.decision IS NULL
    """
    )
    Page<Incidence> findAllUnreviewedWithDetails(Pageable pageable);

    @Query(
            value =
        """
        SELECT DISTINCT i FROM Incidence i
        JOIN FETCH i.publication p
        JOIN FETCH i.reports r
        JOIN FETCH r.reporter
        WHERE i.moderator.id = :userId
    """,
            countQuery = """
        SELECT COUNT(i)
        FROM Incidence i
        WHERE i.moderator.id = :userId
    """
    )
    Page<Incidence> findAllReviewedWithDetails(Long userId, Pageable pageable);

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM Incidence i
        WHERE i.id = :incidenceId AND i.moderator.id = :moderatorId
    """)
    boolean existsByIdAndModeratorId(Long incidenceId, Long moderatorId);

    Optional<Incidence> findByPublicId(UUID publicId);
    
}
