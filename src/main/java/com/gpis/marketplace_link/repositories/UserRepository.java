package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

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
       where r.name = 'ROLE_MODERATOR' and u.id <> :excludeId
       group by u.id
       order by count(distinct i.id) + count(distinct a.id) asc, coalesce(min(i.created_at), now()) asc
       limit 1
      """, nativeQuery = true)
    Long findLeastBusyModeratorExcludingId(@Param("excludeId") Long excludeId);

    Optional<User> findByEmail(String email);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.roles r
        WHERE LOWER(u.email) = LOWER(:email)
    """)
    Optional<User> findByEmailWithRoles(String email);

    Optional<User> findByUsername(String username);
    Optional<User> findByPhone(String phone);
    Optional<User> findByCedula(String cedula);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByPhone(String phone);
    boolean existsByCedula(String cedula);

    // ====== SOFT DELETE ======
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE users SET deleted = true WHERE id = :id AND deleted = false", nativeQuery = true)
    int desactivateById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE users SET deleted = false WHERE id = :id AND deleted = true", nativeQuery = true)
    int activateById(@Param("id") Long id);

    // ====== EXISTS (INCLUYENDO INACTIVOS) ======
    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE LOWER(email) = LOWER(:email))", nativeQuery = true)
    boolean existsIncludingInactiveByEmailNative(@Param("email") String email);

    // NUEVOS: username/phone/dni (incluye inactivos)
    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE LOWER(username) = LOWER(:username))", nativeQuery = true)
    boolean existsIncludingInactiveByUsernameNative(@Param("username") String username);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE phone = :phone)", nativeQuery = true)
    boolean existsIncludingInactiveByPhoneNative(@Param("phone") String phone);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE dni = :dni)", nativeQuery = true)
    boolean existsIncludingInactiveByDniNative(@Param("dni") String dni);

    // ====== EXISTS ACTIVOS EXCLUYENDO ID (para UPDATE) ======
    @Query(value = """
        SELECT EXISTS(
          SELECT 1 FROM users
          WHERE enabled = true
            AND LOWER(email) = LOWER(:email)
            AND id <> :excludeId
        )
        """, nativeQuery = true)
    boolean existsActiveEmailExcludingId(@Param("email") String email, @Param("excludeId") Long excludeId);

    @Query(value = """
        SELECT EXISTS(
          SELECT 1 FROM users
          WHERE enabled = true
            AND LOWER(username) = LOWER(:username)
            AND id <> :excludeId
        )
        """, nativeQuery = true)
    boolean existsActiveUsernameExcludingId(@Param("username") String username, @Param("excludeId") Long excludeId);

    @Query(value = """
        SELECT EXISTS(
          SELECT 1 FROM users
          WHERE enabled = true
            AND phone = :phone
            AND id <> :excludeId
        )
        """, nativeQuery = true)
    boolean existsActivePhoneExcludingId(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    @Query(value = """
        SELECT EXISTS(
          SELECT 1 FROM users
          WHERE enabled = true
            AND dni = :dni
            AND id <> :excludeId
        )
        """, nativeQuery = true)
    boolean existsActiveDniExcludingId(@Param("dni") String dni, @Param("excludeId") Long excludeId);

    // ====== EXISTS (INCLUYENDO INACTIVOS) EXCLUYENDO ID (para UPDATE sin autocolision) ======
    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE LOWER(email) = LOWER(:email) AND id <> :excludeId)", nativeQuery = true)
    boolean existsIncludingInactiveEmailExcludingId(@Param("email") String email, @Param("excludeId") Long excludeId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE LOWER(username) = LOWER(:username) AND id <> :excludeId)", nativeQuery = true)
    boolean existsIncludingInactiveUsernameExcludingId(@Param("username") String username, @Param("excludeId") Long excludeId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE phone = :phone AND id <> :excludeId)", nativeQuery = true)
    boolean existsIncludingInactivePhoneExcludingId(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE dni = :dni AND id <> :excludeId)", nativeQuery = true)
    boolean existsIncludingInactiveDniExcludingId(@Param("dni") String dni, @Param("excludeId") Long excludeId);
}
