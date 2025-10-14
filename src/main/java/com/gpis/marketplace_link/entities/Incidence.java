package com.gpis.marketplace_link.entities;

import com.gpis.marketplace_link.enums.Decision;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "incidences")
public class Incidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne()
    @JoinColumn(name = "publication_id")
    private Publication publication;

    @Enumerated(EnumType.STRING)
    private IncidenceStatus status; // por default es OPEN

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // se setea al crear la incidencia

    @Column(name = "last_report_at", nullable = false)
    private LocalDateTime lastReportAt; // se actualiza cada vez que se agrega un reporte

    @Column(name = "auto_closed")
    public Boolean autoclosed; // por default es false

    @ManyToOne()
    @JoinColumn(name = "moderator_id")
    private User moderator; // por default es null

    @Column(name = "moderator_comment", length = 500)
    private String moderatorComment; // por default es null

    @Enumerated(EnumType.STRING)
    private Decision decision; // por default es null

    @OneToMany(mappedBy = "incidence", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Report> reports;

    @PrePersist()
    public void prePersist() {
        createdAt = LocalDateTime.now();
        lastReportAt = LocalDateTime.now();
        status = IncidenceStatus.OPEN;
        autoclosed = false;
    }
}
