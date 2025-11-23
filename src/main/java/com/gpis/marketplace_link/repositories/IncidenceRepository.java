package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.dto.incidence.projections.*;
import com.gpis.marketplace_link.entities.Incidence;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
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

    @Query(value = """
        SELECT 
            COUNT(*) AS total,
            SUM(CASE WHEN i.status = 'UNDER_REVIEW' THEN 1 ELSE 0 END) AS underReview,
            SUM(CASE WHEN i.status = 'APPEALED' THEN 1 ELSE 0 END) as appealed,
            SUM(CASE WHEN i.status = 'RESOLVED' THEN 1 ELSE 0 END) AS resolved
        FROM incidences i
        WHERE i.moderator_id = :userId
        """, nativeQuery = true)
    IncidenceStatsProjection fetchStatsById(@Param("userId") Long userId);


    /**
     * Cierra automáticamente todas las incidencias con estado "OPEN"
     * cuya última actividad (último reporte) sea anterior a la fecha indicada.
     *
     * @param autoCloseLimit fecha límite; incidencias sin actividad más reciente que esta serán cerradas.
     * @return número de incidencias actualizadas.
     */
    @Modifying
    @Query(value = """
        UPDATE incidences
        SET status = 'RESOLVED',
            auto_closed = true
        WHERE status = 'OPEN'
        AND (
            SELECT MAX(r.created_at)
            FROM reports r
            WHERE r.incidence_id = incidences.id
        ) < :autoCloseLimit
    """, nativeQuery = true)
    int bulkAutoClose(@Param("autoCloseLimit") LocalDateTime autoCloseLimit);

    /**
     * Busca una incidencia asociada a una publicación específica que se encuentre en alguno de los estados indicados.
     *
     * Este método se utiliza principalmente para verificar si ya existe una incidencia
     * activa (por ejemplo, con estado {@code OPEN}, {@code PENDING_REVIEW},{@code UNDER_REVIEW} o {@code APPEALED})
     * antes de crear una nueva.
     *
     * @param publicationId el ID de la publicación.
     * @param status lista de estados válidos para filtrar la búsqueda.
     * @return un {@link Optional} que contiene la incidencia si existe, o vacío si no se encontró.
     */
    Optional<Incidence> findByPublicationIdAndStatusIn(Long publicationId, List<IncidenceStatus> status);

    // Si el vendedor esta deshabilitado y la publicacion tiene fetch eager en el vendedor, esto muere.
    // Trae todas las incidencias sin revisar entre dos fechas
    @Query(
            value = """
        SELECT DISTINCT i FROM Incidence i
        JOIN FETCH i.publication p
        WHERE i.status IN (
            com.gpis.marketplace_link.enums.IncidenceStatus.OPEN,
            com.gpis.marketplace_link.enums.IncidenceStatus.PENDING_REVIEW
        )
        AND i.moderator IS NULL
        AND i.decision = com.gpis.marketplace_link.enums.IncidenceDecision.PENDING
        AND i.createdAt BETWEEN :startDate AND :endDate
    """,
            countQuery = """
        SELECT COUNT(i) FROM Incidence i
        WHERE i.status IN (
            com.gpis.marketplace_link.enums.IncidenceStatus.OPEN,
            com.gpis.marketplace_link.enums.IncidenceStatus.PENDING_REVIEW
        )
        AND i.moderator IS NULL
        AND i.decision = com.gpis.marketplace_link.enums.IncidenceDecision.PENDING
        AND i.createdAt BETWEEN :startDate AND :endDate
    """
    )
    Page<Incidence> findAllUnreviewedWithDetailsBetween(Pageable pageable, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Todas las incidencias sin revisar sin filtro de fecha
    @Query(
            value = """
        SELECT DISTINCT i FROM Incidence i
        JOIN FETCH i.publication p
        WHERE i.status IN (
            com.gpis.marketplace_link.enums.IncidenceStatus.OPEN,
            com.gpis.marketplace_link.enums.IncidenceStatus.PENDING_REVIEW
        )
        AND i.moderator IS NULL
        AND i.decision = com.gpis.marketplace_link.enums.IncidenceDecision.PENDING
    """,
            countQuery = """
        SELECT COUNT(i) FROM Incidence i
        WHERE i.status IN (
            com.gpis.marketplace_link.enums.IncidenceStatus.OPEN,
            com.gpis.marketplace_link.enums.IncidenceStatus.PENDING_REVIEW
        )
        AND i.moderator IS NULL
        AND i.decision = com.gpis.marketplace_link.enums.IncidenceDecision.PENDING
    """
    )
    Page<Incidence> findAllUnreviewedWithDetails(Pageable pageable);


    // Si el vendedor esta deshabilitado y la publicacion tiene fetch eager en el vendedor, esto muere
    @Query(
            value =
                    """
                    SELECT DISTINCT i FROM Incidence i
                    JOIN FETCH i.publication p
                    WHERE i.moderator.id = :userId
                    AND i.createdAt BETWEEN :startDate AND :endDate
                """,
            countQuery = """
        SELECT COUNT(i)
        FROM Incidence i
        WHERE i.moderator.id = :userId
    """
    )
    Page<Incidence> findAllReviewedWithDetailsBetween(Long userId, Pageable pageable, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(
            value =
                    """
                    SELECT DISTINCT i FROM Incidence i
                    JOIN FETCH i.publication p
                    WHERE i.moderator.id = :userId
                """,
            countQuery = """
        SELECT COUNT(i)
        FROM Incidence i
        WHERE i.moderator.id = :userId
    """
    )
    Page<Incidence> findAllReviewedWithDetails(Long userId, Pageable pageable);


    /**
     * Obtiene los detalles completos de una incidencia junto con sus reportes asociados.
     * <p>
     * Cada fila del resultado representa un reporte distinto de la misma incidencia.
     * Si una incidencia tiene varios reportes, los datos de la incidencia se repiten
     * pero cambian los campos correspondientes al reporte y su reportero.
     *
     * @param publicUi identificador público (UUID) de la incidencia.
     * @return lista de proyecciones con los datos de la incidencia, publicación, reportes y reporteros.
     */
    @Query(
            value = """
        SELECT i.public_ui AS incidencePublicUi,
               i.auto_closed AS incidenceAutoclosed,
               i.created_at AS incidenceCreatedAt,
               i.status AS incidenceStatus,
               i.decision AS incidenceDecision,
               i.moderator_id AS incidenceModeratorId,
               i.moderator_comment AS incidenceModeratorComment,
               p.id AS publicationId,
               p.description AS publicationDescription,
               p.status AS publicationStatus,
               p.name AS publicationName,
               p.vendor_id AS publicationVendorId,
               u.id AS reporterId,
               u.email AS reporterEmail,
               CONCAT(u.first_name, ' ', u.last_name) AS reporterFullname,
               r.id AS reportId,
               r.comment AS reportComment,
               r.reason AS reportReason,
               r.created_at AS reportCreatedAt
        FROM incidences i
        JOIN publications p ON i.publication_id = p.id
        JOIN reports r ON r.incidence_id = i.id
        JOIN users u ON r.reporter_id = u.id
        WHERE i.public_ui = :publicUi
        """,
            nativeQuery = true
    )
    List<IncidenceDetailsProjection> findByPublicUiWithDetailsNative(UUID publicUi);

    List<Incidence> findAllByModeratorIdAndStatusIn(Long moderatorId, Collection<IncidenceStatus> statuses);

    /**
     * Verifica si una incidencia pertenece a un moderador específico.
     *
     * @param incidenceId identificador de la incidencia.
     * @param moderatorId identificador del moderador.
     * @return {@code true} si la incidencia fue asignada al moderador, de lo contrario {@code false}.
     */
    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM Incidence i
        WHERE i.id = :incidenceId AND i.moderator.id = :moderatorId
    """)
    boolean existsByIdAndModeratorId(Long incidenceId, Long moderatorId);

    @Query(
            value = """
        SELECT v.id AS vendorId
        FROM incidences i
        JOIN publications p ON i.publication_id = p.id
        JOIN users v ON p.vendor_id = v.id
        WHERE i.public_ui = :publicUi
        """,
            nativeQuery = true
    )
    Optional<VendorIdProjection> findVendorIdByIncidencePublicUi(UUID publicUi);

    Optional<Incidence> findByPublicUi(UUID publicUi);

    @Query(
            value = """
        SELECT u.id AS userId
        FROM incidences i
        JOIN publications p ON i.publication_id = p.id
        JOIN users u ON p.vendor_id = u.id
        WHERE i.public_ui = :publicUi
        """,
            nativeQuery = true
    )
    Optional<UserIdProjection> findUserIdByPublicUi(UUID publicUi);

    @Query(value = """
        SELECT i
        FROM Incidence i
        JOIN FETCH i.publication p
        JOIN FETCH p.vendor v
        WHERE i.publicUi = :publicUi
    """)
    Optional<Incidence> findByPublicUiEager(UUID publicUi);

}
