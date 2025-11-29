package com.gpis.marketplace_link.repository;

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
    Optional<User> findByEmail(String email);
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
    @Query(value = "UPDATE users SET enabled = false WHERE id = :id AND enabled = true", nativeQuery = true)
    int desactivateById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE users SET enabled = true WHERE id = :id AND enabled = false", nativeQuery = true)
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
