package com.gpis.marketplace_link.repositories;

import com.gpis.marketplace_link.entities.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import java.time.LocalDateTime;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findTopByUserIdAndConsumedAtIsNullAndExpiresAtAfterOrderByExpiresAtDesc(
            Long userId, LocalDateTime now
    );
}
