package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.FavoritePublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritePublicationRepository extends JpaRepository<FavoritePublication, Long> {

    boolean existsByUserIdAndPublicationId(Long userId, Long publicationId);

    Optional<FavoritePublication> findByUserIdAndPublicationId(Long userId, Long publicationId);

    @Query("SELECT f FROM FavoritePublication f JOIN FETCH f.publication p LEFT JOIN FETCH p.images WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    List<FavoritePublication> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    long countByUserId(Long userId);

    long countByPublicationId(Long publicationId);
}

