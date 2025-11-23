package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.FavoritePublication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritePublicationRepository extends JpaRepository<FavoritePublication, Long> {

    boolean existsByUserIdAndPublicationId(Long userId, Long publicationId);

    Optional<FavoritePublication> findByUserIdAndPublicationId(Long userId, Long publicationId);

    @Query(
            value = "SELECT DISTINCT f FROM FavoritePublication f " +
                    "JOIN FETCH f.publication p " +
                    "LEFT JOIN FETCH p.images " +
                    "JOIN FETCH p.vendor v " +
                    "WHERE f.user.id = :userId " +
                    "AND p.deletedAt IS NULL " +
                    "AND COALESCE(v.deleted, false) = false " +
                    "ORDER BY f.createdAt DESC",
            countQuery = "SELECT COUNT(f) FROM FavoritePublication f JOIN f.publication p JOIN p.vendor v WHERE f.user.id = :userId AND p.deletedAt IS NULL AND COALESCE(v.deleted, false) = false"
    )
    Page<FavoritePublication> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT f FROM FavoritePublication f JOIN FETCH f.publication p JOIN FETCH p.vendor v LEFT JOIN FETCH p.images WHERE p.id = :publicationId AND p.deletedAt IS NULL AND COALESCE(v.deleted, false) = false ORDER BY f.createdAt DESC")
    List<FavoritePublication> findAllByPublicationId(@Param("publicationId") Long publicationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE favorite_publications SET deleted = true WHERE publication_id = :publicationId", nativeQuery = true)
    int softDeleteAllByPublicationId(@Param("publicationId") Long publicationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE favorite_publications SET deleted = false WHERE publication_id = :publicationId", nativeQuery = true)
    int restoreAllByPublicationId(@Param("publicationId") Long publicationId);

    /**
     * Recupera un favorite_publications por user_id y publication_id sin aplicar el comportamiento de soft-delete
     * (usa consulta nativa para asegurarse de leer incluso filas marcadas como deleted=true).
     */
    @Query(value = "SELECT * FROM favorite_publications WHERE user_id = :userId AND publication_id = :publicationId", nativeQuery = true)
    Optional<FavoritePublication> findAnyByUserIdAndPublicationId(@Param("userId") Long userId, @Param("publicationId") Long publicationId);

    /**
     * Restaura (deleted = false) un favorito concreto y actualiza created_at a NOW().
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE favorite_publications SET deleted = false, created_at = now() WHERE user_id = :userId AND publication_id = :publicationId", nativeQuery = true)
    int restoreByUserIdAndPublicationId(@Param("userId") Long userId, @Param("publicationId") Long publicationId);

    @Query(value = "SELECT CASE WHEN COUNT(f)>0 THEN true ELSE false END FROM FavoritePublication f JOIN f.publication p JOIN p.vendor v WHERE f.user.id = :userId AND p.id = :publicationId AND p.deletedAt IS NULL AND COALESCE(v.deleted, false) = false")
    boolean existsActiveByUserIdAndPublicationId(@Param("userId") Long userId, @Param("publicationId") Long publicationId);
}
