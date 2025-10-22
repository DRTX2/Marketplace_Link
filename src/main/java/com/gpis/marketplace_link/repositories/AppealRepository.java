package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.Appeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AppealRepository extends JpaRepository<Appeal,Long> {

    boolean existsByIncidenceId(Long incidenceId);

    @Query(value = """
        SELECT a 
        FROM Appeal a 
        JOIN FETCH a.incidence i
        JOIN FETCH i.moderator
        WHERE a.status = com.gpis.marketplace_link.enums.AppealStatus.FAILED_NO_MOD AND 
              a.newModerator IS NULL
    """)
    List<Appeal> findAllPendingWithModerator();

    @Query(value = """
        SELECT DISTINCT a 
        FROM Appeal a 
        JOIN FETCH a.incidence i
        JOIN FETCH i.reports r 
        JOIN FETCH i.publication p 
        JOIN FETCH i.moderator m
        JOIN FETCH a.newModerator nm
        JOIN FETCH a.seller s 
        WHERE a.newModerator.id = :userId
    """, countQuery = """
        SELECT COUNT(a) 
        FROM Appeal a 
        WHERE a.newModerator.id = :userId
    """)
    Page<Appeal> findAppealsByUserId(Long userId, Pageable pageable);

    @Query("""
        SELECT COUNT(a) > 0
        FROM Appeal a
        WHERE a.id = :appealId AND a.newModerator.id = :moderatorId
    """)
    boolean isUserAuthorizedToDecideAppeal(Long appealId, Long moderatorId);

}
