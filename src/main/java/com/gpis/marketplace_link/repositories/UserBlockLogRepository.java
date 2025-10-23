package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.UserBlockLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserBlockLogRepository extends JpaRepository<UserBlockLog, Long> {

    @Query("""
        SELECT u FROM UserBlockLog u
        WHERE u.user.id = :userId
        AND u.blockedAction = 'REPORT'
        AND u.targetPublication.id = :publicationId
        AND u.blockedUntil > :now
        """)
    Optional<UserBlockLog> findActiveBlockForReport(
            @Param("userId") Long userId,
            @Param("publicationId") Long publicationId,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT u FROM UserBlockLog u
        WHERE u.user.id = :userId
        AND u.blockedAction = 'REPORT'
        AND u.targetPublication.id = :publicationId
        ORDER BY u.blockedUntil DESC
        LIMIT 1
    """)
    Optional<UserBlockLog> findLastBlockForReport(
            @Param("userId") Long userId,
            @Param("publicationId") Long publicationId
    );

}
