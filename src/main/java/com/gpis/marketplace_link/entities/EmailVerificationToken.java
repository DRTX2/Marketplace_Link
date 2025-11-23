package com.gpis.marketplace_link.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_tokens", indexes = {
        @Index(name="idx_email_token", columnList="token", unique = true),
        @Index(name="idx_email_token_user", columnList="user_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailVerificationToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=100)
    private String token;

    @ManyToOne(optional=false, fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @Column(name="expires_at", nullable=false)
    private LocalDateTime expiresAt;

    @Column(name="consumed_at")
    private LocalDateTime consumedAt;

    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
    public boolean isUsed()    { return consumedAt != null; }
}
