package com.gpis.marketplace_link.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "favorite_publications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_publication", columnNames = {"user_id", "publication_id"})
        }
)
@Data
@NoArgsConstructor
@SoftDelete(strategy = SoftDeleteType.DELETED)
@AllArgsConstructor
public class FavoritePublication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    private Publication publication;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted", insertable = false, updatable = false)
    private Boolean deleted;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

