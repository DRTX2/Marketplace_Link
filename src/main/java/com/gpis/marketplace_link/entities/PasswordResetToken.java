package com.gpis.marketplace_link.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="password_reset_token")
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id",nullable = false)
    private User user;

    @Column(nullable = false,length = 255)
    private String token;
    @Column(nullable = false)
    private Boolean used;

    @Column(nullable = false)
    private LocalDateTime expiration;

    @PrePersist
    public void prePersist() {
        this.used = false;
    }
}
