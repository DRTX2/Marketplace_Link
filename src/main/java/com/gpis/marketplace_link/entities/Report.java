package com.gpis.marketplace_link.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne()
    @JoinColumn(name = "incidence_id")
    private Incidence incidence;

    @ManyToOne()
    @JoinColumn(name = "reporter_id")
    private User reporter; // usuario que reporta

    @Column(name = "reason", nullable = false, length = 100)
    private String reason; // motivo del reporte

    @Column(name = "comment")
    private String comment; // comentario adicional (opcional)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // se setea al crear el reporte

    @PrePersist()
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
