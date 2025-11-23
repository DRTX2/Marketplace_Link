package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // ====== FINDS y EXISTS actuales (solo activos por @SoftDelete) ======

    /**
     * Busca el moderador con menor carga de trabajo actual, excluyendo a uno específico.
     *
     * Este método ejecuta un query que selecciona a todos los usuarios con el rol de moderador,
     * cuenta el número total de incidencias y apelaciones que tienen asignadas y los ordena por:
     *
     * 1. Cantidad total de tareas (incidencias + apelaciones) en orden ascendente.
     *    - El moderador con menos asignaciones aparecerá primero.
     * 2. Fecha de la incidencia más antigua (MIN(i.created_at)) en orden ascendente.
     *    - Si hay empate en la cantidad de tareas, se prioriza al moderador que lleva más tiempo
     *      sin recibir una nueva asignación.
     *
     * También se excluye del resultado al moderador cuyo ID se pasa como parámetro excludeId.
     * Esto es útil cuando se desea reasignar una incidencia y no devolverla al mismo moderador.
     *
     * Detalles del query:
     * - JOIN users_roles y JOIN roles: filtra únicamente usuarios con rol ROLE_MODERATOR.
     * - LEFT JOIN incidences y LEFT JOIN appeals: se cuentan tareas incluso si el moderador no tiene ninguna.
     * - COUNT(DISTINCT i.id) + COUNT(DISTINCT a.id): total de tareas por moderador.
     * - COALESCE(MIN(i.created_at), NOW()): si no tiene incidencias aún, se usa la fecha actual.
     *
     * @param excludeId ID del moderador que debe ser excluido del resultado.
     * @return El ID del moderador menos ocupado actualmente.
     */
    @Query(value =
            """
       select u.id
       from users u
       join users_roles ur ON u.id = ur.user_id
       join roles r on r.id = ur.role_id
       left join incidences i ON i.moderator_id = u.id
       left join appeals a on a.new_moderator_id = u.id
       where (r.name = 'ROLE_MODERATOR' OR r.name = 'ROLE_ADMIN') and u.id <> :excludeId
       group by u.id
       order by count(distinct i.id) + count(distinct a.id) asc, coalesce(min(i.created_at), now()) asc
       limit 1
      """, nativeQuery = true)
    Long findLeastBusyModeratorOrAdminExcludingId(@Param("excludeId") Long excludeId);

    Optional<User> findByEmail(String email);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.roles r
        WHERE LOWER(u.email) = LOWER(:email)
    """)
    Optional<User> findByEmailWithRoles(String email);

    /**
     * Finds all users including soft-deleted ones, filtered by optional roles and search term.
     *
     * @param Q Search term (nullable)
     * @param Roles Array of role names to filter by (nullable)
     * @param RolesIsNull Whether the roles filter should be ignored
     * @return List of users matching the criteria, including deleted ones
     */
    @Query(value = """
    SELECT DISTINCT u.*
    FROM users u
    LEFT JOIN users_roles ur ON ur.user_id = u.id
    LEFT JOIN roles r        ON r.id = ur.role_id
    WHERE
      ( :Q IS NULL OR
        u.username   ILIKE :Q OR
        u.email      ILIKE :Q OR
        u.first_name ILIKE :Q OR
        u.last_name  ILIKE :Q OR
        u.phone      ILIKE :Q OR
        u.cedula     ILIKE :Q
      )
      AND ( :RolesIsNull = TRUE OR r.name = ANY(CAST(:Roles AS varchar[])) )
    """,
            nativeQuery = true)
    List<User> findAllIncludingDeletedNative(
            @Param("Q") String Q,
            @Param("Roles") String[] Roles,
            @Param("RolesIsNull") boolean RolesIsNull
    );

    /**
     * Finds all users including soft-deleted ones, filtered by optional roles and search term, with pagination.
     *
     * @param Q Search term (nullable)
     * @param Roles Array of role names to filter by (nullable)
     * @param RolesIsNull Whether the roles filter should be ignored
     * @param pageable Pagination and sorting information
     * @return Page of users matching the criteria, including deleted ones
     */
    @Query(value = """
    SELECT DISTINCT u.*
    FROM users u
    LEFT JOIN users_roles ur ON ur.user_id = u.id
    LEFT JOIN roles r        ON r.id = ur.role_id
    WHERE
      ( :Q IS NULL OR
        u.username   ILIKE :Q OR
        u.email      ILIKE :Q OR
        u.first_name ILIKE :Q OR
        u.last_name  ILIKE :Q OR
        u.phone      ILIKE :Q OR
        u.cedula     ILIKE :Q
      )
      AND ( :RolesIsNull = TRUE OR r.name = ANY(CAST(:Roles AS varchar[])) )
    """,
            countQuery = """
    SELECT COUNT(DISTINCT u.id)
    FROM users u
    LEFT JOIN users_roles ur ON ur.user_id = u.id
    LEFT JOIN roles r        ON r.id = ur.role_id
    WHERE
      ( :Q IS NULL OR
        u.username   ILIKE :Q OR
        u.email      ILIKE :Q OR
        u.first_name ILIKE :Q OR
        u.last_name  ILIKE :Q OR
        u.phone      ILIKE :Q OR
        u.cedula     ILIKE :Q
      )
      AND ( :RolesIsNull = TRUE OR r.name = ANY(CAST(:Roles AS varchar[])) )
    """,
            nativeQuery = true)
    Page<User> findAllIncludingDeletedNative(
            @Param("Q") String Q,
            @Param("Roles") String[] Roles,
            @Param("RolesIsNull") boolean RolesIsNull,
            Pageable pageable
    );

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    // ====== SOFT DELETE ======
    // ====== DESACTIVAR USUARIO ======
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        UPDATE users
        SET deleted = true, account_status = 'INACTIVE'
        WHERE id = :id AND deleted = false
        """, nativeQuery = true)
    int deactivateById(@Param("id") Long id);

    // ====== ACTIVAR USUARIO ======
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        UPDATE users
        SET deleted = false, account_status = 'ACTIVE'
        WHERE id = :id AND deleted = true
        """, nativeQuery = true)
    int activateById(@Param("id") Long id);

    @Query(
            value = """
        SELECT CASE WHEN COUNT(u.id) > 0 THEN TRUE ELSE FALSE END
        FROM users u
        WHERE u.id = :vendorId
          AND u.deleted = FALSE
          AND u.account_status = 'ACTIVE'
        """,
            nativeQuery = true
    )
    boolean existsByIdNative(@Param("vendorId") Long vendorId);


    @Query(value = "SELECT * FROM users WHERE id = :userId", nativeQuery = true)
    Optional<User> findByIdNative(@Param("userId") Long userId);


}
