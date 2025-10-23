package com.gpis.marketplace_link.entities;

import com.gpis.marketplace_link.enums.IncidenceDecision;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "incidences")
public class Incidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_ui",nullable = false, unique = true, updatable = false)
    private UUID publicUi = UUID.randomUUID();

    @ManyToOne()
    @JoinColumn(name = "publication_id")
    private Publication publication;

    @Enumerated(EnumType.STRING)
    private IncidenceStatus status; // por default es OPEN

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // se setea al crear la incidencia

    @Column(name = "auto_closed")
    public Boolean autoclosed; // por default es false

    @ManyToOne()
    @JoinColumn(name = "moderator_id")
    private User moderator; // por default es null

    @Column(name = "moderator_comment", length = 500)
    private String moderatorComment; // por default es null

    @Enumerated(EnumType.STRING)
    private IncidenceDecision decision; // por default es null

    @OneToMany(mappedBy = "incidence", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Report> reports;

    @PrePersist()
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = IncidenceStatus.OPEN;
        }
        if (decision == null) {
            decision = IncidenceDecision.PENDING;
        }
        autoclosed = false;
    }
}
