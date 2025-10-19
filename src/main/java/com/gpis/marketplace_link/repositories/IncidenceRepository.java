package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.Incidence;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /**
     * Cierra automáticamente las incidencias abiertas cuya última fecha de reporte sea anterior al tiempo de corte indicado.
     *
     * Este método se utiliza en procesos automáticos (como {@code autoclose()})
     * para actualizar en lote el estado de incidencias inactivas. Las incidencias afectadas pasan a estado
     * {@code RESOLVED} y se marcan como autoclosed.
     *
     * Por razones de rendimiento, esta operación se ejecuta directamente mediante una sentencia
     * {@code UPDATE} y no carga las entidades en memoria.
     *
     * @param cutoff fecha y hora límite para determinar qué incidencias deben cerrarse.
     * @return el número de incidencias actualizadas.
     */
    @Modifying
    @Query("""
        UPDATE Incidence i
        SET i.status = com.gpis.marketplace_link.enums.IncidenceStatus.RESOLVED, i.autoclosed = true
        WHERE i.status = com.gpis.marketplace_link.enums.IncidenceStatus.OPEN AND i.lastReportAt < :cutoff
    """)
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
    @Query("""
        SELECT DISTINCT i FROM Incidence i
        JOIN FETCH i.publication p
        JOIN FETCH i.reports r
        JOIN FETCH r.reporter
        WHERE i.status IN (com.gpis.marketplace_link.enums.IncidenceStatus.OPEN, com.gpis.marketplace_link.enums.IncidenceStatus.UNDER_REVIEW) AND
              i.moderator IS NULL AND
              i.decision IS NULL
    """)
    List<Incidence> findAllUnreviewedWithDetails();

    @Query("""
        SELECT DISTINCT i FROM Incidence i
        JOIN FETCH i.publication p
        JOIN FETCH i.reports r
        JOIN FETCH r.reporter
        WHERE i.moderator.id = :userId
    """)
    List<Incidence> findAllReviewedWithDetails(Long userId);

    @Query("""
        SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
        FROM Incidence i
        WHERE i.id = :incidenceId AND i.moderator.id = :moderatorId
    """)
    boolean existsByIdAndModeratorId(Long incidenceId, Long moderatorId);

}
